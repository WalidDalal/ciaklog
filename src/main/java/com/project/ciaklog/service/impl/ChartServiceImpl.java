package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.response.ChartItemResponse;
import com.project.ciaklog.dto.response.ChartUserResponse;
import com.project.ciaklog.dto.response.TrendingItemResponse;
import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.Role;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.service.ChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChartServiceImpl implements ChartService {

    private static final int MIN_VOTES = 1;
    private static final int TOP_LIMIT = 3;

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    @Cacheable("topFilms")
    public List<ChartItemResponse> getTopFilms() {
        return getTopByContentType(ContentType.MOVIE, TOP_LIMIT);
    }

    @Override
    @Cacheable("topSeries")
    public List<ChartItemResponse> getTopSeries() {
        return getTopByContentType(ContentType.TV, TOP_LIMIT);
    }

    @Override
    @Cacheable("topUsers")
    public List<ChartUserResponse> getTopUsers() {
        List<com.project.ciaklog.entity.User> users =
                userRepository.findTop5ByRoleNotOrderByScoreDesc(Role.ADMIN);

        List<ChartUserResponse> result = new java.util.ArrayList<>();
        // Prima si usava "rank = i+1"
        // solo quando il punteggio cambiava — un ranking "a salto" (1,2,2,4:
        // 3 persone in 2ª posizione, la successiva salta alla 4ª contando gli
        // scavalcati). Il comportamento richiesto è invece un ranking "denso"
        // (1,2,2,3): chi ha un punteggio diverso prende semplicemente la
        // posizione successiva a quella del gruppo precedente, senza salti.
        int rank = 1;
        for (int i = 0; i < users.size(); i++) {
            if (i > 0 && users.get(i).getScore() != users.get(i - 1).getScore()) {
                rank++;
            }
            com.project.ciaklog.entity.User u = users.get(i);
            result.add(ChartUserResponse.builder()
                    .username(u.getUsername())
                    .profileColor(u.getProfileColor())
                    .score(u.getScore())
                    .reviewCount(null)
                    .rank(rank)
                    .build());
        }
        return result;
    }

    @Override
    @Cacheable("trending")
    public List<TrendingItemResponse> getTrending() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> rows = reviewRepository.findTrendingGrouped(weekAgo);

        return rows.stream()
                .limit(6)
                .map(row -> {
                    Long tmdbId      = (Long) row[0];
                    ContentType type = (ContentType) row[1];
                    int weeklyCount  = ((Long) row[2]).intValue();

                    List<Review> reviews = reviewRepository.findVisibleByTmdbIdAndContentType(tmdbId, type);
                    double avg = computeWeightedAverage(reviews);

                    return TrendingItemResponse.builder()
                            .tmdbId(tmdbId)
                            .contentType(type)
                            .posterPath(null)
                            .title(null)
                            .weeklyReviewCount(weeklyCount)
                            .ciakLogAverageRating(avg)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<ChartItemResponse> getTopByContentType(ContentType contentType, int limit) {
        List<Object[]> rows = reviewRepository.findAggregatedByContentType(contentType, MIN_VOTES);

        return rows.stream()
                .map(row -> {
                    Long tmdbId   = (Long) row[0];
                    long count    = (Long) row[2];
                    double avgRaw = (Double) row[3];
                    double avg    = Math.round(avgRaw * 10.0) / 10.0;

                    return ChartItemResponse.builder()
                            .tmdbId(tmdbId)
                            .contentType(contentType)
                            .posterPath(null)
                            .title(null)
                            .averageRating(avg)
                            .totalVotes((int) count)
                            .build();
                })
                .sorted(Comparator.comparingDouble(ChartItemResponse::getAverageRating).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private double computeWeightedAverage(List<Review> reviews) {
        if (reviews.isEmpty()) return 0.0;
        double weightedSum = 0;
        double totalWeight = 0;
        for (Review r : reviews) {
            // Trovato in revisione, causa di un bug bloccante: con created_at
            // nel futuro (visto coi dati reali di un utente: alcune recensioni
            // datate mesi dopo "adesso" — un difetto del seed, non dell'app,
            // ma la formula sottostante non doveva comunque permetterlo),
            // "days" risultava negativo, il peso 1/(1+days/30) diventava
            // negativo, e una media pesata con pesi negativi può uscire da
            // QUALSIASI range sensato (verificato: con questi identici dati si
            // ottiene 16.3 su una scala 1-5). "days" non può mai essere < 0
            // ai fini del peso: una recensione "nel futuro" ha comunque il
            // peso massimo di una recensione di oggi, non uno negativo.
            long days = Math.max(0, ChronoUnit.DAYS.between(r.getCreatedAt(), LocalDateTime.now()));
            double weight = 1.0 / (1 + (double) days / 30);
            weightedSum += r.getRating() * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? 0 : Math.round((weightedSum / totalWeight) * 10.0) / 10.0;
    }
}