package com.project.ciaklog.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ciaklog.dto.request.AiChatRequest;
import com.project.ciaklog.dto.request.StructureReviewRequest;
import com.project.ciaklog.dto.response.AiChatResponse;
import com.project.ciaklog.dto.response.DailyRecommendationResponse;
import com.project.ciaklog.dto.response.StructureReviewResponse;
import com.project.ciaklog.dto.response.TmdbSearchResultResponse;
import com.project.ciaklog.dto.response.TrendingItemResponse;
import com.project.ciaklog.entity.*;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.AiRecommendationLogRepository;
import com.project.ciaklog.repository.ReportRepository;
import com.project.ciaklog.entity.ReportStatus;
import com.project.ciaklog.entity.UserStatus;
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
    private final ReportRepository reportRepository;
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

                    IMPORTANTE — resta sempre nei binari di CiakLog:
                    Rispondi SOLO a richieste su film, serie TV, consigli di visione o l'uso della piattaforma.
                    Se il messaggio dell'utente non riguarda questi argomenti (es. domande generiche,
                    richieste su altri argomenti non cinematografici, richieste di scrivere codice, ecc.),
                    NON inventare titoli a caso: nel campo "reply" spiega gentilmente che puoi aiutare solo
                    con film, serie TV e consigli di visione, e lascia "titles" vuoto ([]).

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

                    IMPORTANTE — resta sempre nei binari di CiakLog: rispondi SOLO a richieste su film,
                    serie TV o consigli di visione. Se la richiesta attuale non riguarda questi argomenti,
                    rispondi con un array vuoto: []

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
                    final java.util.List<String> titlesList = new java.util.ArrayList<>();
                    root.path("titles").forEach(n -> titlesList.add(n.asText()));
                    titles = titlesList;
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
    public AiChatResponse chatAdmin(String username, AiChatRequest request) {
        // Verifica che sia un admin
        User admin = getUser(username);
        String sessionId = request.getSessionId() != null ? request.getSessionId() : java.util.UUID.randomUUID().toString();

        // Costruisce il contesto gestionale dal DB
        String adminContext = buildAdminContext();
        String historyText = buildHistoryText(request);

        String prompt = """
                Sei l'assistente gestionale di CiakLog, una piattaforma di tracking film e serie TV.
                Stai parlando con un amministratore della piattaforma.
                
                Dati attuali della piattaforma:
                %s
                
                Storico conversazione:
                %s
                
                Domanda dell'amministratore: %s
                
                ISTRUZIONI:
                - Rispondi SOLO a domande gestionali sulla piattaforma (utenti, segnalazioni, violazioni, statistiche)
                - Usa i dati forniti sopra per rispondere in modo preciso
                - Se la domanda non riguarda la gestione della piattaforma, rispondi esattamente:
                  "Non posso aiutarti con questo. Sono l'assistente gestionale di CiakLog e rispondo solo a domande sulla gestione della piattaforma."
                - Rispondi in italiano, in modo conciso e diretto
                - NON restituire JSON, solo testo naturale
                """.formatted(adminContext, historyText, request.getMessage());

        try {
            String rawResponse = callGroq(prompt);
            saveLog(admin, sessionId, request.getMessage(), rawResponse);

            return AiChatResponse.builder()
                    .reply(rawResponse.trim())
                    .suggestions(java.util.List.of()) // nessun suggerimento film per l'admin
                    .sessionId(sessionId)
                    .build();

        } catch (Exception e) {
            log.error("Errore nella chat admin per {}: {}", username, e.getMessage());
            throw new RuntimeException("Assistente temporaneamente non disponibile", e);
        }
    }

    /**
     * Costruisce il contesto gestionale leggendo i dati dal DB.
     * Groq risponde solo ai dati qui presenti — non ha accesso diretto al DB.
     */
    private String buildAdminContext() {
        StringBuilder sb = new StringBuilder();

        // Segnalazioni
        long pending = reportRepository.findByStatus(ReportStatus.PENDING, org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        long approved = reportRepository.findByStatus(ReportStatus.APPROVED, org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        long rejected = reportRepository.findByStatus(ReportStatus.REJECTED, org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();

        sb.append("=== SEGNALAZIONI ===\n");
        sb.append("- In attesa (PENDING): ").append(pending).append("\n");
        sb.append("- Approvate (recensione rimossa): ").append(approved).append("\n");
        sb.append("- Rifiutate: ").append(rejected).append("\n\n");

        // Ultime 5 segnalazioni pendenti con dettaglio
        var pendingReports = reportRepository.findByStatus(ReportStatus.PENDING,
                org.springframework.data.domain.PageRequest.of(0, 5,
                        org.springframework.data.domain.Sort.by("createdAt").descending()));

        if (pendingReports.hasContent()) {
            sb.append("Ultime segnalazioni in attesa:\n");
            pendingReports.forEach(r -> sb.append("  - ")
                    .append(r.getReporter().getUsername())
                    .append(" ha segnalato la recensione di ")
                    .append(r.getReview().getUser().getUsername())
                    .append(" (categoria: ").append(r.getReasonCategory()).append(")")
                    .append("\n"));
            sb.append("\n");
        }

        // Utenti
        long totalUsers = userRepository.count() - 1; // escludi admin
        long suspended = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.SUSPENDED).count();
        long permSuspended = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.PERMANENTLY_SUSPENDED).count();

        sb.append("=== UTENTI ===\n");
        sb.append("- Utenti totali: ").append(totalUsers).append("\n");
        sb.append("- Sospesi temporaneamente: ").append(suspended).append("\n");
        sb.append("- Sospesi permanentemente: ").append(permSuspended).append("\n\n");

        // Utenti con violazioni
        var usersWithViolations = userRepository.findAll().stream()
                .filter(u -> u.getViolationCount() > 0 && u.getRole() == com.project.ciaklog.entity.Role.USER)
                .sorted((a, b) -> Integer.compare(b.getViolationCount(), a.getViolationCount()))
                .limit(5)
                .toList();

        if (!usersWithViolations.isEmpty()) {
            sb.append("Utenti con violazioni (top 5):\n");
            usersWithViolations.forEach(u -> sb.append("  - ")
                    .append(u.getUsername())
                    .append(": ").append(u.getViolationCount()).append(" violazioni")
                    .append(", stato: ").append(u.getStatus())
                    .append("\n"));
        }

        return sb.toString();
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

    // Fix (AI più centrale): "recensione a botta calda" — non salva nulla,
    // restituisce solo il testo strutturato che l'utente rivede prima di pubblicare
    @Override
    public StructureReviewResponse structureReview(String username, StructureReviewRequest request) {
        User user = getUser(username); // verifica che l'utente esista, coerenza con gli altri metodi

        String movieContext = (request.getMovieTitle() != null && !request.getMovieTitle().isBlank())
                ? "Il film/serie di cui sta scrivendo è: " + request.getMovieTitle()
                : "Non è specificato il titolo del film/serie.";

        String prompt = """
                Sei l'assistente di scrittura di CiakLog, un'app di tracking film/serie TV.
                Un utente ha appena visto un film/serie e ha buttato giù alcuni appunti sparsi
                su cosa ne pensa. Il tuo compito è trasformarli in una recensione breve e ben
                scritta in italiano (massimo 4-5 frasi), mantenendo FEDELMENTE il suo tono,
                le sue opinioni e i suoi giudizi.

                REGOLE FONDAMENTALI:
                - NON inventare opinioni, dettagli della trama o giudizi che l'utente non ha espresso
                - NON ammorbidire né esagerare il suo giudizio (se è severo, resta severo; se entusiasta, resta entusiasta)
                - Riordina e ripulisci la forma, non il contenuto
                - Se gli appunti sono troppo confusi o vuoti di contenuto per essere strutturati,
                  restituisci il testo originale così com'è, senza inventare nulla

                %s

                Appunti dell'utente:
                %s

                Rispondi SOLO con il testo della recensione, niente virgolette, niente titoli, niente altro testo.
                """.formatted(movieContext, request.getRawNotes());

        try {
            String rawResponse = callGroq(prompt);
            String cleaned = rawResponse.trim().replaceAll("^\"|\"$", "");
            return StructureReviewResponse.builder().text(cleaned).build();
        } catch (Exception e) {
            log.error("Errore nella strutturazione recensione per {}: {}", username, e.getMessage());
            throw new RuntimeException("Assistente temporaneamente non disponibile", e);
        }
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

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_BACKOFF_MS = 1500L;

    private String callGroq(String prompt) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "model", GROQ_MODEL,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        ));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = mapper.readTree(response.body());
                    return root.path("choices").get(0).path("message").path("content").asText();
                }

                // 429 o 5xx — errori transitori, vale la pena riprovare
                boolean isTransient = response.statusCode() == 429
                        || response.statusCode() >= 500;

                log.warn("Groq risposto {} al tentativo {}/{}", response.statusCode(), attempt + 1, MAX_RETRIES + 1);

                if (!isTransient || attempt == MAX_RETRIES) {
                    throw new RuntimeException("Groq API non disponibile (status " + response.statusCode() + ")");
                }

            } catch (java.io.IOException | InterruptedException e) {
                // Errore di rete — retriable
                log.warn("Errore di rete Groq al tentativo {}/{}: {}", attempt + 1, MAX_RETRIES + 1, e.getMessage());
                lastException = e;
                if (attempt == MAX_RETRIES) throw new RuntimeException("Assistente temporaneamente non disponibile", e);
            }

            // Backoff esponenziale: 1.5s, 3s
            Thread.sleep(RETRY_BACKOFF_MS * (long) Math.pow(2, attempt));
        }

        throw new RuntimeException("Assistente temporaneamente non disponibile", lastException);
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