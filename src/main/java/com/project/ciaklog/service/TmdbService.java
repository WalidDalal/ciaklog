package com.project.ciaklog.service;

import com.project.ciaklog.dto.response.TmdbDetailResponse;
import com.project.ciaklog.dto.response.TmdbSearchResultResponse;
import com.project.ciaklog.entity.ContentType;

import java.util.List;

public interface TmdbService {
    List<TmdbSearchResultResponse> search(String query, String type);
    TmdbDetailResponse getDetail(Long tmdbId, ContentType contentType);
}