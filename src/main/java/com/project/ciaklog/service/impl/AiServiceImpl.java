package com.project.ciaklog.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ciaklog.dto.request.AiChatRequest;
import com.project.ciaklog.dto.request.MovieQuestionRequest;
import com.project.ciaklog.dto.request.StructureCommentRequest;
import com.project.ciaklog.dto.request.StructureReviewRequest;
import com.project.ciaklog.dto.response.*;
import com.project.ciaklog.entity.*;
import com.project.ciaklog.entity.Review;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.AiRecommendationLogRepository;
import com.project.ciaklog.repository.ReportRepository;
import com.project.ciaklog.entity.ReportStatus;
import com.project.ciaklog.entity.UserStatus;
import com.project.ciaklog.repository.DailyRecommendationCacheRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.repository.WatchEntryRepository;
import org.springframework.transaction.annotation.Transactional;
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
    private final ReviewRepository reviewRepository;
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
            // Fix: aggiunta istruzione esplicita per i saluti/small talk — senza,
            // un semplice "ciao" veniva comunque interpretato come richiesta di
            // consigli, allegando sempre 3 titoli anche se non richiesti
            prompt = """
                    Sei l'assistente AI di CiakLog, un'app di tracking film/serie TV.
                    Profilo cinematografico dell'utente:
                    %s
                    Messaggio dell'utente: %s
                    Rispondi in modo naturale e amichevole in italiano (1-2 frasi), poi suggerisci titoli pertinenti.
                    Presta attenzione a richieste situazionali (es. "qualcosa di leggero stasera",
                    "un film simile a X ma più recente", "poco tempo, qualcosa di breve") e adatta
                    i suggerimenti al contesto/mood richiesto, non solo al genere.

                    IMPORTANTE — resta sempre nei binari di CiakLog:
                    Rispondi SOLO a richieste su film, serie TV, consigli di visione o l'uso della piattaforma.
                    Se il messaggio dell'utente non riguarda questi argomenti (es. domande generiche,
                    richieste su altri argomenti non cinematografici, richieste di scrivere codice, ecc.),
                    NON inventare titoli a caso: nel campo "reply" spiega gentilmente che puoi aiutare solo
                    con film, serie TV e consigli di visione, e lascia "titles" vuoto ([]).

                    Se il messaggio è solo un saluto, small talk, un ringraziamento o non contiene una
                    richiesta reale (es. "ciao", "come stai", "grazie"), rispondi in modo naturale e
                    amichevole ma lascia "titles" vuoto ([]) — non allegare suggerimenti non richiesti.

                    Per ogni titolo, aggiungi un motivo brevissimo (max 12 parole) del perché lo consigli
                    in base al profilo/richiesta dell'utente.

                    Formato risposta — SOLO questo JSON, niente altro:
                    { "reply": "testo naturale qui", "titles": [
                      { "title": "Titolo 1", "reason": "motivo breve" },
                      { "title": "Titolo 2", "reason": "motivo breve" },
                      { "title": "Titolo 3", "reason": "motivo breve" }
                    ] }
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
            // Fix (AI più centrale): motivo del suggerimento, per titolo — vuota
            // per i messaggi successivi e nei fallback (nessun motivo disponibile)
            Map<String, String> reasons = new java.util.LinkedHashMap<>();

            if (isFirstMessage) {
                // Parsing formato { "reply": "...", "titles": [{ "title": ..., "reason": ... }] }
                try {
                    String cleaned = rawResponse.replaceAll("```json|```", "").trim();
                    JsonNode root = mapper.readTree(cleaned);
                    reply = root.path("reply").asText("Ciao! Ecco alcuni suggerimenti per te:");
                    final java.util.List<String> titlesList = new java.util.ArrayList<>();
                    root.path("titles").forEach(n -> {
                        // Fix: n può essere sia stringa (formato vecchio/fallback del modello)
                        // sia oggetto { title, reason } — gestisco entrambi senza far crashare il parsing
                        String title = n.isObject() ? n.path("title").asText() : n.asText();
                        String reason = n.isObject() ? n.path("reason").asText(null) : null;
                        if (title != null && !title.isBlank()) {
                            titlesList.add(title);
                            if (reason != null && !reason.isBlank()) reasons.put(title, reason);
                        }
                    });
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

            List<TmdbSearchResultResponse> suggestions = resolveTitlesOnTmdb(titles, reasons);

            // Fix: questo controllo sovrascriveva SEMPRE il reply quando non c'erano
            // suggerimenti — ma da quando il prompt del primo messaggio istruisce il
            // modello a lasciare "titles" vuoto di proposito per saluti/small talk
            // (con una risposta naturale nel campo "reply"), un semplice "ciao"
            // otteneva comunque il messaggio generico "Non ho trovato suggerimenti
            // validi" al posto del saluto vero e proprio del modello. Ora si
            // sovrascrive solo se il modello AVEVA proposto dei titoli (falliti a
            // risolversi su TMDB) — se erano vuoti di proposito, il reply resta intatto
            if (suggestions.isEmpty() && (!isFirstMessage || !titles.isEmpty())) {
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

    // Fix: senza @Transactional la sessione Hibernate si chiude prima che
    // buildAdminContext() acceda a relazioni lazy (r.getReporter(), r.getReview().getUser())
    // -> LazyInitializationException: no session, 500 su ogni chiamata dell'admin
    @Override
    @Transactional(readOnly = true)
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
            // Fix: da quando esistono anche segnalazioni su risposte (ReviewComment),
            // r.getReview() può essere null — causava un NullPointerException (500)
            // ogni volta che una sola segnalazione pendente puntava a una risposta
            // invece che a una recensione. Già successo una volta, ripristinato dopo
            // essere sparito in un giro di modifiche successive — occhio a non perderlo di nuovo
            pendingReports.forEach(r -> {
                if (r.getReview() != null) {
                    sb.append("  - ")
                            .append(r.getReporter().getUsername())
                            .append(" ha segnalato la recensione di ")
                            .append(r.getReview().getUser().getUsername())
                            .append(" (categoria: ").append(r.getReasonCategory()).append(")")
                            .append("\n");
                } else {
                    sb.append("  - ")
                            .append(r.getReporter().getUsername())
                            .append(" ha segnalato la risposta di ")
                            .append(r.getReviewComment().getAuthor().getUsername())
                            .append(" (categoria: ").append(r.getReasonCategory()).append(")")
                            .append("\n");
                }
            });
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
            List<String> titles = new java.util.ArrayList<>();
            Map<String, String> reasons = new java.util.LinkedHashMap<>();
            parseTitlesAndReasons(rawResponse, titles, reasons);
            suggestions = resolveTitlesOnTmdb(titles, reasons);
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

    // Fix (AI dentro le risposte, deciso): stesso principio della "botta calda"
    // per le recensioni — l'utente scrive la risposta a modo suo, l'AI ripulisce
    // la forma SENZA inventare contenuto. Più breve di una recensione (una
    // risposta è un commento, non una recensione in miniatura) e usa il
    // contesto di cosa si sta rispondendo per restare coerente col thread
    @Override
    public StructureReviewResponse structureComment(String username, StructureCommentRequest request) {
        getUser(username); // verifica che l'utente esista, coerenza con gli altri metodi

        String replyContext = (request.getReplyingToText() != null && !request.getReplyingToText().isBlank())
                ? "Sta rispondendo a questa recensione: \"" + request.getReplyingToText() + "\""
                : "Non è specificato a cosa sta rispondendo.";

        String prompt = """
                Sei l'assistente di scrittura di CiakLog, un'app di tracking film/serie TV.
                Un utente sta scrivendo una RISPOSTA (non una recensione) a una recensione
                di un altro utente, e ha buttato giù alcuni appunti sparsi su cosa vuole dire.
                Il tuo compito è trasformarli in una risposta breve e ben scritta in italiano
                (massimo 2-3 frasi, tono da commento/conversazione, non da recensione formale),
                mantenendo FEDELMENTE il suo tono e le sue opinioni.

                REGOLE FONDAMENTALI:
                - NON inventare opinioni o argomenti che l'utente non ha espresso
                - NON ammorbidire né esagerare il suo giudizio
                - Riordina e ripulisci la forma, non il contenuto
                - Resta breve: è una risposta in un thread, non una recensione
                - Se gli appunti sono troppo confusi o vuoti di contenuto per essere
                  strutturati, restituisci il testo originale così com'è, senza inventare nulla

                %s

                Appunti dell'utente:
                %s

                Rispondi SOLO con il testo della risposta, niente virgolette, niente altro testo.
                """.formatted(replyContext, request.getRawNotes());

        try {
            String rawResponse = callGroq(prompt);
            String cleaned = rawResponse.trim().replaceAll("^\"|\"$", "");
            return StructureReviewResponse.builder().text(cleaned).build();
        } catch (Exception e) {
            log.error("Errore nella strutturazione risposta per {}: {}", username, e.getMessage());
            throw new RuntimeException("Assistente temporaneamente non disponibile", e);
        }
    }

    // Fix (AI più centrale): "Chiedi su questo film" — sessione separata dalla
    // chat generale, stateless (nessuna memoria tra domande), contesto = dati
    // TMDB di quel titolo specifico, non la libreria/gusti dell'utente
    private static final String SPOILER_MARKER = "[SPOILER]";

    @Override
    public MovieQuestionResponse answerMovieQuestion(String username, MovieQuestionRequest request) {
        getUser(username); // verifica che l'utente esista, coerenza con gli altri metodi

        TmdbDetailResponse detail = tmdbService.getDetail(request.getTmdbId(), request.getContentType());

        String castNames = detail.getCast() == null || detail.getCast().isEmpty()
                ? "Non disponibile"
                : detail.getCast().stream().map(TmdbDetailResponse.CastMember::getName)
                    .reduce((a, b) -> a + ", " + b).orElse("Non disponibile");

        String prompt = """
                Sei l'assistente di CiakLog per domande su un film/serie specifico.
                Rispondi SOLO a domande su questo titolo (trama, cast, genere, temi, ecc.) —
                se la domanda non riguarda questo titolo, spiega gentilmente che puoi
                rispondere solo a domande su "%s".

                Dati del titolo:
                - Titolo: %s (%s)
                - Genere/i: %s
                - Trama (sinossi ufficiale, non spoiler): %s
                - Cast principale: %s

                Puoi usare anche la tua conoscenza generale su questo titolo specifico
                (es. finale, colpi di scena) se la domanda lo richiede, ma resta pertinente
                e non inventare dettagli che non sai con certezza.

                GESTIONE SPOILER: se la risposta rivela il finale, un evento chiave della
                trama o un colpo di scena importante, fai iniziare la risposta ESATTAMENTE
                con il token "%s" (poi il resto della risposta normalmente). Se non ci sono
                spoiler, non aggiungere il token.

                Domanda dell'utente: %s

                Rispondi in italiano, in modo conciso (massimo 4-5 frasi).
                """.formatted(
                        detail.getTitle(), detail.getTitle(),
                        detail.getReleaseYear() != null ? detail.getReleaseYear() : "anno sconosciuto",
                        detail.getGenres() != null ? String.join(", ", detail.getGenres()) : "Non disponibile",
                        detail.getOverview() != null ? detail.getOverview() : "Non disponibile",
                        castNames, SPOILER_MARKER, request.getQuestion());

        try {
            String rawResponse = callGroq(prompt).trim();
            boolean containsSpoiler = rawResponse.startsWith(SPOILER_MARKER);
            String answer = containsSpoiler
                    ? rawResponse.substring(SPOILER_MARKER.length()).trim()
                    : rawResponse;

            return MovieQuestionResponse.builder()
                    .answer(answer)
                    .containsSpoiler(containsSpoiler)
                    .build();
        } catch (Exception e) {
            log.error("Errore nella risposta AI su film {} per {}: {}", request.getTmdbId(), username, e.getMessage());
            throw new RuntimeException("Assistente temporaneamente non disponibile", e);
        }
    }

    // Fix (CiakLog Wrapped): wrapper pubblico riusabile attorno a callGroq —
    // non lancia mai, torna stringa vuota se l'AI non risponde (il chiamante
    // decide come gestire l'assenza del testo, senza far fallire tutto)
    @Override
    public String generateNarrative(String prompt) {
        try {
            return callGroq(prompt).trim();
        } catch (Exception e) {
            log.warn("Narrativa AI non generata (proseguo senza): {}", e.getMessage());
            return "";
        }
    }

    // Fix (suggerimento contestuale AI negli stati vuoti, approvato): frase
    // breve invece del solito messaggio piatto — riusa generateNarrative(),
    // che già gestisce il fallback silenzioso se l'AI non risponde
    @Override
    public String getEmptyStateTip(String context) {
        String prompt = """
                Sei l'assistente di CiakLog, un'app di tracking film/serie TV.
                L'utente si trova in questa situazione: %s

                Scrivi UNA sola frase breve (max 15 parole), amichevole e utile, che lo
                incoraggi o gli suggerisca cosa fare — vai dritto al punto, niente saluti.
                Resta sempre in tema film/serie/piattaforma.

                Rispondi SOLO con la frase, niente virgolette, niente altro testo.
                """.formatted(context);

        return generateNarrative(prompt);
    }

    // Fix (AI che replica a una recensione negativa): SOLO su richiesta
    // esplicita — mai automatica. Tono leggero, offre una prospettiva
    // alternativa senza essere condiscendente. Solo sulla PROPRIA recensione
    // (evita che venga usata per "punzecchiare" recensioni altrui) e solo se
    // il voto è basso (rating <= 2), coerente col nome della funzione.
    private static final int LOW_RATING_THRESHOLD = 2;

    @Override
    @Transactional(readOnly = true)
    public ReviewOpinionResponse getAiOpinionOnReview(String username, UUID reviewId) {
        User user = getUser(username);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new BusinessRuleException("Puoi chiedere il parere dell'AI solo sulle tue recensioni");
        }
        if (review.getRating() > LOW_RATING_THRESHOLD) {
            throw new BusinessRuleException("Disponibile solo per recensioni con voto basso");
        }

        // Fix: il titolo non è salvato su Review — lo recupero da un WatchEntry
        // dello stesso utente per lo stesso tmdbId/contentType, se esiste
        String title = watchEntryRepository
                .findAllByUserAndStatus(user, com.project.ciaklog.entity.WatchStatus.WATCHED).stream()
                .filter(w -> w.getTmdbId().equals(review.getTmdbId()) && w.getContentType() == review.getContentType())
                .findFirst()
                .map(w -> w.getTitle())
                .orElse("questo titolo");

        String prompt = """
                Sei l'assistente di CiakLog. Un utente ha dato un voto basso (%d/5) a "%s"
                e ha scritto questa recensione: "%s"

                L'utente ti ha chiesto ESPLICITAMENTE il tuo parere. Rispondi con un tono
                leggero e amichevole, MAI condiscendente, offrendo una prospettiva diversa
                o un dettaglio che magari non ha considerato — senza insistere che ha
                torto, è solo uno scambio di opinioni scherzoso tra appassionati.
                Massimo 2-3 frasi, in italiano.
                """.formatted(review.getRating(), title, review.getText());

        String opinion = generateNarrative(prompt);
        if (opinion.isBlank()) {
            throw new RuntimeException("Assistente temporaneamente non disponibile");
        }
        return ReviewOpinionResponse.builder().opinion(opinion).build();
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
                Per ogni titolo aggiungi un motivo brevissimo (max 12 parole) del perché lo consigli oggi.
                Rispondi SOLO con questo JSON, niente altro:
                [
                  { "title": "Titolo 1", "reason": "motivo breve" },
                  { "title": "Titolo 2", "reason": "motivo breve" },
                  { "title": "Titolo 3", "reason": "motivo breve" }
                ]
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
        return resolveTitlesOnTmdb(titles, Map.of());
    }

    // Fix (AI più centrale): stessa risoluzione TMDB di sempre, ma arricchisce
    // il risultato col motivo del suggerimento quando disponibile (chat/daily AI)
    private List<TmdbSearchResultResponse> resolveTitlesOnTmdb(List<String> titles, Map<String, String> reasons) {
        List<TmdbSearchResultResponse> suggestions = new ArrayList<>();
        for (String title : titles) {
            List<TmdbSearchResultResponse> found = tmdbService.search(title, null, 1).getResults();
            if (!found.isEmpty()) {
                TmdbSearchResultResponse item = found.get(0);
                String reason = reasons.get(title);
                if (reason != null) {
                    item.setReason(reason);
                }
                suggestions.add(item);
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

    // Fix (AI più centrale): come parseTitlesFromResponse, ma gestisce anche il
    // nuovo formato [{ "title": ..., "reason": ... }] usato da getDaily.
    // Riempie le due collezioni passate (niente valore di ritorno multiplo in Java).
    private void parseTitlesAndReasons(String rawResponse, List<String> outTitles, Map<String, String> outReasons) {
        try {
            String cleaned = rawResponse.replaceAll("```json|```", "").trim();
            JsonNode arr = mapper.readTree(cleaned);
            arr.forEach(node -> {
                String title = node.isObject() ? node.path("title").asText() : node.asText();
                String reason = node.isObject() ? node.path("reason").asText(null) : null;
                if (title != null && !title.isBlank()) {
                    outTitles.add(title);
                    if (reason != null && !reason.isBlank()) outReasons.put(title, reason);
                }
            });
        } catch (Exception e) {
            log.warn("Impossibile fare il parsing della risposta AI come JSON: {}", rawResponse);
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