package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ContentType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TmdbSearchResultResponse {
    private Long tmdbId;
    private String title;
    private ContentType contentType;
    private String posterPath;
    private Integer releaseYear;
    private Double tmdbRating;

    // Fix (AI più centrale): "spiegazione del perché" — valorizzato solo quando
    // il suggerimento viene dalla chat/daily AI, null nella ricerca TMDB normale.
    // @Setter perché il risultato arriva già costruito da TmdbService e va
    // solo arricchito con il motivo dopo, non ricreato da capo.
    private String reason;
}