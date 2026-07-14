package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// Fix (Ricerca): prima il backend ignorava silenziosamente il parametro `page`
// (nemmeno dichiarato nel metodo), quindi la ricerca prendeva sempre e solo la
// prima pagina di TMDB (fissa a 20 risultati) — sembrava un tetto fisso di
// 40/20 quando in realtà mancava solo la paginazione vera
@Getter
@Builder
public class TmdbSearchResponse {
    private List<TmdbSearchResultResponse> results;
    private int page;
    private int totalPages;
    private int totalResults;
}
