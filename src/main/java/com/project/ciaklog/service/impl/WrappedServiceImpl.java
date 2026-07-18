package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.response.WrappedResponse;
import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.ReviewStatus;
import com.project.ciaklog.entity.User;
import com.project.ciaklog.entity.WatchEntry;
import com.project.ciaklog.entity.WatchStatus;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.repository.WatchEntryRepository;
import com.project.ciaklog.service.AiService;
import com.project.ciaklog.service.WrappedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

// Fix (Priorità 1 — CiakLog Wrapped): tutte le statistiche vengono dai dati
// già esistenti (WatchEntry + Review), nessuna nuova tabella per i numeri —
// solo il commento narrativo finale passa dall'AI, ed è opzionale (se l'AI
// non risponde, il Wrapped funziona comunque senza quella riga).
@Service
@RequiredArgsConstructor
public class WrappedServiceImpl implements WrappedService {

    private final UserRepository userRepository;
    private final WatchEntryRepository watchEntryRepository;
    private final ReviewRepository reviewRepository;
    private final AiService aiService;

    @Override
    @Transactional(readOnly = true)
    public WrappedResponse getWrapped(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        List<WatchEntry> watched = watchEntryRepository.findAllByUserAndStatus(user, WatchStatus.WATCHED);

        // Fix: le REMOVED non devono contare nelle statistiche personali —
        // sono state rimosse per violazione, non fanno parte del "tuo anno"
        List<Review> reviews = reviewRepository.findAllByUser(user).stream()
                .filter(r -> r.getStatus() != ReviewStatus.REMOVED)
                .toList();

        long totalWatched = watched.size();
        long totalReviews = reviews.size();

        // Genere preferito — i generi sono salvati come CSV su WatchEntry
        Map<String, Long> genreCounts = watched.stream()
                .filter(w -> w.getGenres() != null && !w.getGenres().isBlank())
                .flatMap(w -> Arrays.stream(w.getGenres().split(",")))
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));
        Map.Entry<String, Long> topGenre = genreCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null);

        // Mese più cinefilo — raggruppato su watchedDate
        Map<String, Long> monthCounts = watched.stream()
                .filter(w -> w.getWatchedDate() != null)
                .collect(Collectors.groupingBy(
                        w -> capitalize(w.getWatchedDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN))
                                + " " + w.getWatchedDate().getYear(),
                        Collectors.counting()));
        Map.Entry<String, Long> topMonth = monthCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null);

        // Titoli per recuperare un nome leggibile dato tmdbId+contentType
        // (Review non salva il titolo, WatchEntry sì — evita una chiamata TMDB in più)
        // Fix (Wrapped — titolo preferito che spariva): la mappa titoli usava solo
        // le entry con status WATCHED. Se dopo aver recensito un titolo il suo stato
        // cambiava (rimesso "da vedere", rimosso dalla libreria), la entry usciva da
        // "watched" e la recensione — che esiste ancora — non trovava più un titolo,
        // facendo sparire silenziosamente la card "titolo preferito/meno amato" anche
        // con voto e recensione ancora presenti. I titoli ora vengono da TUTTE le
        // entry dell'utente, indipendentemente dallo stato attuale.
        List<WatchEntry> allEntries = watchEntryRepository.findAllByUser(user);
        Map<String, String> titleByKey = allEntries.stream()
                .collect(Collectors.toMap(
                        w -> w.getTmdbId() + "-" + w.getContentType(),
                        WatchEntry::getTitle,
                        (a, b) -> a));

        Review favReview = reviews.stream().max(Comparator.comparingInt(Review::getRating)).orElse(null);
        Review leastReview = reviews.stream().min(Comparator.comparingInt(Review::getRating)).orElse(null);

        Double avgRating = reviews.isEmpty() ? null
                : reviews.stream().mapToInt(Review::getRating).average().orElse(0);

        String narrative = totalWatched == 0
                ? "" // Fix: niente chiamata AI se non c'è ancora nulla da raccontare
                : aiService.generateNarrative(buildNarrativePrompt(
                        user, totalWatched, totalReviews, topGenre, topMonth, avgRating));

        return WrappedResponse.builder()
                .totalWatched(totalWatched)
                .totalReviews(totalReviews)
                .topGenre(topGenre != null ? topGenre.getKey() : null)
                .topGenreCount(topGenre != null ? topGenre.getValue().intValue() : 0)
                .mostActiveMonth(topMonth != null ? topMonth.getKey() : null)
                .mostActiveMonthCount(topMonth != null ? topMonth.getValue().intValue() : 0)
                .favoriteTitle(favReview != null ? titleByKey.get(favReview.getTmdbId() + "-" + favReview.getContentType()) : null)
                .favoriteRating(favReview != null ? favReview.getRating() : null)
                .leastFavoriteTitle(leastReview != null ? titleByKey.get(leastReview.getTmdbId() + "-" + leastReview.getContentType()) : null)
                .leastFavoriteRating(leastReview != null ? leastReview.getRating() : null)
                .averageRating(avgRating)
                .aiNarrative(narrative)
                .build();
    }

    private String buildNarrativePrompt(User user, long totalWatched, long totalReviews,
                                         Map.Entry<String, Long> topGenre, Map.Entry<String, Long> topMonth,
                                         Double avgRating) {
        return """
                Sei l'assistente di CiakLog. Scrivi un breve commento narrativo (2-3 frasi,
                tono caldo e amichevole, in italiano) per il recap annuale dell'utente "%s",
                stile "Spotify Wrapped" ma per film/serie TV.

                Dati dell'utente:
                - Titoli visti: %d
                - Recensioni scritte: %d
                - Genere preferito: %s
                - Mese più attivo: %s
                - Voto medio dato: %s

                Non inventare dettagli non presenti sopra. Scrivi solo il commento, niente titoli o virgolette.
                """.formatted(
                        user.getUsername(), totalWatched, totalReviews,
                        topGenre != null ? topGenre.getKey() : "non abbastanza dati",
                        topMonth != null ? topMonth.getKey() : "non abbastanza dati",
                        avgRating != null ? String.format(Locale.ITALIAN, "%.1f/5", avgRating) : "nessuna recensione ancora"
                );
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
