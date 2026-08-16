package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// @NoArgsConstructor necessario: @Builder da solo toglie il costruttore
// vuoto implicito, e Jackson ne ha bisogno per deserializzare questa
// classe (usato in AiServiceImpl.toDailyDTO per rileggere i suggerimenti
// salvati in cache) — senza, ogni readValue falliva silenziosamente.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbSearchResultResponse {
    private Long tmdbId;
    private String title;
    private ContentType contentType;
    private String posterPath;
    private Integer releaseYear;
    private Double tmdbRating;

    // Valorizzato solo quando il suggerimento viene dalla chat/daily AI,
    // null nella ricerca TMDB normale
    private String reason;
}
