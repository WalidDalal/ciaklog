package com.project.ciaklog.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ciaklog.dto.response.TmdbDetailResponse;
import com.project.ciaklog.dto.response.TmdbSearchResponse;
import com.project.ciaklog.dto.response.TmdbSearchResultResponse;
import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.service.TmdbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbServiceImpl implements TmdbService {

    private static final String BASE_URL = "https://api.themoviedb.org/3";

    @Value("${tmdb.api.key}")
    private String apiKey;

    private final ReviewRepository reviewRepository;

    // Timeout condiviso — 5s connessione, 10s risposta (TMDB è veloce)
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public TmdbSearchResponse search(String query, String type, int page) {
        try {
            String endpoint = "movie".equals(type) ? "/search/movie"
                    : "tv".equals(type) ? "/search/tv"
                      : "/search/multi";

            // Bearer header (metodo moderno) — ?api_key= è deprecato da TMDB
            // Fix: prima non veniva mai passato &page= — sempre e solo pagina 1
            String url = BASE_URL + endpoint + "?query=" + java.net.URLEncoder.encode(query, "UTF-8")
                    + "&page=" + Math.max(1, page);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("TMDB ha risposto {} per la ricerca '{}': {}", response.statusCode(), query, response.body());
                throw new RuntimeException("Servizio TMDB temporaneamente non disponibile");
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode results = root.path("results");

            List<TmdbSearchResultResponse> out = new ArrayList<>();
            for (JsonNode r : results) {
                String mediaType = r.has("media_type") ? r.get("media_type").asText()
                        : (type != null ? type : "movie");
                if (!"movie".equals(mediaType) && !"tv".equals(mediaType)) continue;

                out.add(TmdbSearchResultResponse.builder()
                        .tmdbId(r.get("id").asLong())
                        .title(r.has("title") ? r.get("title").asText() : r.path("name").asText())
                        .contentType("tv".equals(mediaType) ? ContentType.TV : ContentType.MOVIE)
                        .posterPath(r.path("poster_path").asText(null))
                        .releaseYear(extractYear(r, mediaType))
                        .tmdbRating(r.path("vote_average").isMissingNode() ? null : r.path("vote_average").asDouble())
                        .build());
            }

            return TmdbSearchResponse.builder()
                    .results(out)
                    .page(root.path("page").asInt(page))
                    .totalPages(root.path("total_pages").asInt(1))
                    .totalResults(root.path("total_results").asInt(out.size()))
                    .build();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Errore nella ricerca TMDB per query '{}': {}", query, e.getMessage());
            throw new RuntimeException("Servizio TMDB temporaneamente non disponibile", e);
        }
    }

    @Override
    public TmdbDetailResponse getDetail(Long tmdbId, ContentType contentType) {
        try {
            String endpoint = contentType == ContentType.TV ? "/tv/" : "/movie/";
            String url = BASE_URL + endpoint + tmdbId + "?append_to_response=credits";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new ResourceNotFoundException(
                        "Contenuto TMDB non trovato: " + contentType + "/" + tmdbId);
            }
            if (response.statusCode() != 200) {
                log.error("TMDB ha risposto {} per {}/{}: {}", response.statusCode(), contentType, tmdbId, response.body());
                throw new RuntimeException("Servizio TMDB temporaneamente non disponibile");
            }

            JsonNode root = mapper.readTree(response.body());

            List<String> genres = new ArrayList<>();
            root.path("genres").forEach(g -> genres.add(g.get("name").asText()));

            List<TmdbDetailResponse.CastMember> cast = StreamSupport
                    .stream(root.path("credits").path("cast").spliterator(), false)
                    .limit(5)
                    .map(c -> TmdbDetailResponse.CastMember.builder()
                            .name(c.get("name").asText())
                            .photoPath(c.path("profile_path").asText(null))
                            .build())
                    .toList();

            double ciakLogAvg = computeCiakLogAverage(tmdbId, contentType);
            int ciakLogVotes = reviewRepository
                    .findVisibleByTmdbIdAndContentType(tmdbId, contentType)
                    .size();

            return TmdbDetailResponse.builder()
                    .tmdbId(tmdbId)
                    .title(contentType == ContentType.TV ? root.path("name").asText() : root.path("title").asText())
                    .contentType(contentType)
                    .posterPath(root.path("poster_path").asText(null))
                    .releaseYear(extractYear(root, contentType == ContentType.TV ? "tv" : "movie"))
                    .overview(root.path("overview").asText(null))
                    .genres(genres)
                    .cast(cast)
                    .tmdbRating(root.path("vote_average").asDouble())
                    .ciakLogAverageRating(ciakLogAvg)
                    .ciakLogVoteCount(ciakLogVotes)
                    .numberOfSeasons(contentType == ContentType.TV && root.hasNonNull("number_of_seasons")
                            ? root.path("number_of_seasons").asInt() : null)
                    .build();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Errore nel recupero dettaglio TMDB id {}: {}", tmdbId, e.getMessage());
            throw new RuntimeException("Servizio TMDB temporaneamente non disponibile", e);
        }
    }

    // ── helpers ──

    private double computeCiakLogAverage(Long tmdbId, ContentType contentType) {
        var reviews = reviewRepository.findVisibleByTmdbIdAndContentType(tmdbId, contentType);
        if (reviews.isEmpty()) return 0.0;
        double sum = reviews.stream().mapToInt(r -> r.getRating()).sum();
        return Math.round((sum / reviews.size()) * 10.0) / 10.0;
    }

    private Integer extractYear(JsonNode node, String mediaType) {
        String dateField = "tv".equals(mediaType) ? "first_air_date" : "release_date";
        String date = node.path(dateField).asText(null);
        if (date == null || date.length() < 4) return null;
        return Integer.parseInt(date.substring(0, 4));
    }
}