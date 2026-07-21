package com.project.ciaklog.service;

import com.project.ciaklog.dto.response.TmdbDetailResponse;
import com.project.ciaklog.dto.response.TmdbSearchResponse;
import com.project.ciaklog.entity.ContentType;

public interface TmdbService {
    // Aggiunto il parametro page, prima mancava del tutto — la ricerca
    // era sempre bloccata sulla prima pagina di TMDB (20 risultati fissi)
    TmdbSearchResponse search(String query, String type, int page);
    TmdbDetailResponse getDetail(Long tmdbId, ContentType contentType);
}