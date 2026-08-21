package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// Include page/totalPages/totalResults: la ricerca ora pagina davvero
// invece di prendere sempre la prima pagina fissa di TMDB
@Getter
@Builder
public class TmdbSearchResponse {
    private List<TmdbSearchResultResponse> results;
    private int page;
    private int totalPages;
    private int totalResults;
}
