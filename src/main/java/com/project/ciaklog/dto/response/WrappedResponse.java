package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

// Fix (Priorità 1 — CiakLog Wrapped): recap personale stile "Spotify Wrapped",
// calcolato dai dati già esistenti (WatchEntry + Review), nessuna nuova tabella
// per le statistiche in sé — solo il testo narrativo viene generato al volo.
@Getter
@Builder
public class WrappedResponse {
    private long totalWatched;
    private long totalReviews;

    private String topGenre;       // null se non ci sono abbastanza dati
    private int topGenreCount;

    private String mostActiveMonth; // es. "Marzo 2026", null se nessun dato
    private int mostActiveMonthCount;

    private String favoriteTitle;   // voto più alto tra le recensioni
    private Integer favoriteRating;

    private String leastFavoriteTitle; // voto più basso tra le recensioni
    private Integer leastFavoriteRating;

    private Double averageRating;   // null se nessuna recensione

    private String aiNarrative;     // piccolo commento narrativo generato dall'AI
}
