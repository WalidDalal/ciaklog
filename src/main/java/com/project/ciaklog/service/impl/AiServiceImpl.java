package com.project.ciaklog.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ciaklog.dto.request.AiChatRequest;
import com.project.ciaklog.dto.response.AiChatResponse;
import com.project.ciaklog.dto.response.DailyRecommendationResponse;
import com.project.ciaklog.dto.response.TmdbSearchResultResponse;
import com.project.ciaklog.dto.response.TrendingItemResponse;
import com.project.ciaklog.entity.*;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.AiRecommendationLogRepository;
import com.project.ciaklog.repository.DailyRecommendationCacheRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.repository.WatchEntryRepository;
import com.project.ciaklog.service.AiService;
import com.project.ciaklog.service.ChartService;
import com.project.ciaklog.service.TmdbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";

    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final int CACHE_VALID_HOURS = 24;
    private static final int LIBRARY_PROFILE_LIMIT = 15;

    @Value("${groq.api.key}")
    private String apiKey;

    private final UserRepository userRepository;
    private final WatchEntryRepository watchEntryRepository;
    private final AiRecommendationLogRepository logRepository;
    private final DailyRecommendationCacheRepository cacheRepository;
    private final TmdbService tmdbService;
    private final ChartService chartService;

    // Timeout: 5s di connessione, 30s per la risposta completa di Groq
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public AiChatResponse chat(String username, AiChatRequest request) {
        User user = getUser(username);

        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();

        String libraryProfile = buildLibraryProfile(user);
        String historyText = buildHistoryText(request);

        boolean isFirstMessage = request.getSessionHistory() == null || request.getSessionHistory().isEmpty();

        String prompt;
        if (isFirstMessage) {
            // Primo messaggio: risposta naturale + suggerimenti
            prompt = """
                    Sei l'assistente AI di CiakLog, un'app di tracking film/serie TV.
                    Profilo cinematografico dell'utente:
                    %s
                    Messaggio dell'utente: %s
                    Rispondi in modo naturale e amichevole in italiano (1-2 frasi), poi suggerisci titoli pertinenti.
                    Formato risposta — SOLO questo JSON, niente altro:
                    { "reply": "testo naturale qui", "titles": ["Titolo 1", "Titolo 2", "Titolo 3"] }
                    """.formatted(libraryProfile, request.getMessage());
        } else {
            // Messaggi successivi: comportamento normale
            prompt = """
                    Sei l'assistente AI di CiakLog, un'app di tracking film/serie TV.
                    Profilo cinematografico dell'utente:
                    %s
                    Storico della conversazione:
                    %s
                    Richiesta attuale: %s
                    Rispondi SOLO con un array JSON di titoli esistenti (max 5), formato:
                    ["Titolo 1", "Titolo 2", ...]
                    Niente testo aggiuntivo, solo l'array JSON.
                    """.formatted(libraryProfile, historyText, request.getMessage());
        }

        try {
            String rawResponse = callGroq(prompt);

            String reply;
            List<String> titles;

            if (isFirstMessage) {
                // Parsing formato { "reply": "...", "titles": [...] }
                try {
                    String cleaned = rawResponse.replaceAll("```json|```", "").trim();
                    JsonNode root = mapper.readTree(cleaned);
                    reply = root.path("reply").asText("Ciao! Ecco alcuni suggerimenti per te:");
                    titles = new ArrayList<>();
                    for (JsonNode n : root.path("titles")) {
                        titles.add(n.asText());
                    }
                } catch (Exception e) {
                    // Fallback: tratta come array di titoli
                    reply = "Ecco alcuni titoli che potrebbero piacerti:";
                    titles = parseTitlesFromResponse(rawResponse);
                }
            } else {
                reply = "Ecco alcuni titoli che potrebbero piacerti:";
                titles = parseTitlesFromResponse(rawResponse);
            }

            List<TmdbSearchResultResponse> suggestions = resolveTitlesOnTmdb(titles);

            if (suggestions.isEmpty()) {
                reply = "Non ho trovato suggerimenti validi, prova a riformulare la richiesta.";
            }

            saveLog(user, sessionId, request.getMessage(), rawResponse);

            return AiChatResponse.builder()
                    .reply(reply)
                    .suggestions(suggestions)
                    .sessionId(sessionId)
                    .build();

        } catch (Exception e) {
            log.error("Errore nella chiamata a Groq per utente {}: {}", username, e.getMessage());
            throw new RuntimeException("Assistente temporaneamente non disponibile", e);
        }
    }

    @Override
    public DailyRecommendationResponse getDaily(String username) {
        User user = getUser(username);

        var existingCache = cacheRepository.findByUser(user).orElse(null);

        if (existingCache != null &&
                existingCache.getGeneratedAt().isAfter(LocalDateTime.now().minusHours(CACHE_VALID_HOURS))) {
            return toDailyDTO(existingCache);
        }

        String prompt = buildDailyPrompt(user);

        List<TmdbSearchResultResponse> suggestions;
        try {
            String rawResponse = callGroq(prompt);
            List<String> titles = parseTitlesFromResponse(rawResponse);
            suggestions = resolveTitlesOnTmdb(titles);
        } catch (Exception e) {
            log.error("Errore nella generazione raccomandazione giornaliera per {}: {}", username, e.getMessage());
            if (existingCache != null) {
                return toDailyDTO(existingCache);
            }
            return DailyRecommendationResponse.builder()
                    .suggestions(List.of())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        String suggestionsJson;
        try {
            suggestionsJson = mapper.writeValueAsString(suggestions);
        } catch (Exception e) {
            suggestionsJson = "[]";
        }

        DailyRecommendationCache cache = existingCache != null ? existingCache : new DailyRecommendationCache();
        cache.setUser(user);
        cache.setSuggestionsJson(suggestionsJson);
        cache.setGeneratedAt(LocalDateTime.now());
        cacheRepository.save(cache);

        return DailyRecommendationResponse.builder()
                .suggestions(suggestions)
                .generatedAt(cache.getGeneratedAt())
                .build();
    }

    // ── helpers ──

    private String buildDailyPrompt(User user) {
        String libraryProfile = buildLibraryProfile(user);
        String watchingNow = buildWatchingNowProfile(user);
        String trendingNow = buildTrendingProfile();

        return """
                Sei l'assistente AI di CiakLog. Genera una raccomandazione giornaliera personalizzata.
                Profilo cinematografico dell'utente:
                %s
                Cosa sta guardando ora (per continuità o varietà):
                %s
                Cosa sta discutendo la community questa settimana:
                %s
                Suggerisci 3 titoli che potrebbero piacergli oggi, privilegiando varietà
                rispetto a quello che ha già visto, ma considerando anche i trend della community.
                Rispondi SOLO con un array JSON di titoli esistenti, formato:
                ["Titolo 1", "Titolo 2", "Titolo 3"]
                Niente testo aggiuntivo, solo l'array JSON.
                """.formatted(libraryProfile, watchingNow, trendingNow);
    }

    private String buildWatchingNowProfile(User user) {
        List<WatchEntry> watching = watchEntryRepository.findAllByUserAndStatus(user, WatchStatus.WATCHING);
        if (watching.isEmpty()) return "Nessun contenuto attualmente in visione.";

        StringBuilder sb = new StringBuilder();
        watching.forEach(e -> sb.append("- ").append(e.getTitle()).append("\n"));
        return sb.toString();
    }

    private String buildTrendingProfile() {
        List<TrendingItemResponse> trending = chartService.getTrending();
        if (trending.isEmpty()) return "Nessun dato di tendenza disponibile.";

        StringBuilder sb = new StringBuilder();
        trending.stream().limit(3).forEach(t ->
                sb.append("- tmdbId ").append(t.getTmdbId())
                        .append(" (").append(t.getWeeklyReviewCount()).append(" recensioni questa settimana)\n"));
        return sb.toString();
    }

    private List<TmdbSearchResultResponse> resolveTitlesOnTmdb(List<String> titles) {
        List<TmdbSearchResultResponse> suggestions = new ArrayList<>();
        for (String title : titles) {
            List<TmdbSearchResultResponse> found = tmdbService.search(title, null);
            if (!found.isEmpty()) {
                suggestions.add(found.get(0));
            }
        }
        return suggestions;
    }

    private DailyRecommendationResponse toDailyDTO(DailyRecommendationCache cache) {
        List<TmdbSearchResultResponse> suggestions;
        try {
            suggestions = List.of(mapper.readValue(cache.getSuggestionsJson(), TmdbSearchResultResponse[].class));
        } catch (Exception e) {
            suggestions = List.of();
        }
        return DailyRecommendationResponse.builder()
                .suggestions(suggestions)
                .generatedAt(cache.getGeneratedAt())
                .build();
    }

    private String callGroq(String prompt) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "model", GROQ_MODEL,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        ));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .timeout(Duration.ofSeconds(30)) // timeout per risposta completa
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Groq ha risposto {} : {}", response.statusCode(), response.body());
            throw new RuntimeException("Groq API non disponibile (status " + response.statusCode() + ")");
        }

        JsonNode root = mapper.readTree(response.body());
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    private List<String> parseTitlesFromResponse(String rawResponse) {
        try {
            String cleaned = rawResponse.replaceAll("```json|```", "").trim();
            JsonNode arr = mapper.readTree(cleaned);
            List<String> titles = new ArrayList<>();
            arr.forEach(node -> titles.add(node.asText()));
            return titles;
        } catch (Exception e) {
            log.warn("Impossibile fare il parsing della risposta AI come JSON: {}", rawResponse);
            return List.of();
        }
    }

    private String buildLibraryProfile(User user) {
        // PageRequest.of(0, 15) invece di Pageable.unpaged() — evita di caricare tutta la libreria in memoria
        List<WatchEntry> entries = watchEntryRepository
                .findByUser(user, PageRequest.of(0, LIBRARY_PROFILE_LIMIT))
                .getContent();

        if (entries.isEmpty()) {
            return "Libreria vuota — nessun dato disponibile, fornisci suggerimenti generici.";
        }

        StringBuilder sb = new StringBuilder();
        entries.forEach(e ->
                sb.append("- ").append(e.getTitle())
                        .append(" (").append(e.getStatus()).append(")\n"));
        return sb.toString();
    }

    private String buildHistoryText(AiChatRequest request) {
        if (request.getSessionHistory() == null || request.getSessionHistory().isEmpty()) {
            return "Nessuno storico — è il primo messaggio della sessione.";
        }

        List<AiChatRequest.MessageDTO> history = request.getSessionHistory();
        int from = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);

        StringBuilder sb = new StringBuilder();
        for (AiChatRequest.MessageDTO msg : history.subList(from, history.size())) {
            sb.append(msg.getRole().name()).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    private void saveLog(User user, String sessionId, String message, String rawResponse) {
        String messagesJson;
        try {
            messagesJson = mapper.writeValueAsString(Map.of(
                    "request", message,
                    "response", rawResponse
            ));
        } catch (Exception e) {
            messagesJson = "{}";
        }

        AiRecommendationLog logEntry = AiRecommendationLog.builder()
                .user(user)
                .sessionId(sessionId)
                .messagesJson(messagesJson)
                .build();

        logRepository.save(logEntry);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }
}