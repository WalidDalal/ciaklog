package com.project.ciaklog.service;

import com.project.ciaklog.dto.response.ChartItemResponse;
import com.project.ciaklog.dto.response.ChartUserResponse;
import com.project.ciaklog.dto.response.TrendingItemResponse;

import java.util.List;

public interface ChartService {
    List<ChartItemResponse> getTopFilms();
    List<ChartItemResponse> getTopSeries();
    List<ChartUserResponse> getTopUsers();
    List<TrendingItemResponse> getTrending();
}