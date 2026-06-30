package com.project.ciaklog.controller;

import com.project.ciaklog.dto.response.ChartItemResponse;
import com.project.ciaklog.dto.response.ChartUserResponse;
import com.project.ciaklog.dto.response.TrendingItemResponse;
import com.project.ciaklog.service.ChartService;
import com.project.ciaklog.service.TmdbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/charts")
@RequiredArgsConstructor
public class ChartController {

    private final ChartService chartService;
    private final TmdbService tmdbService;

    @GetMapping("/films")
    public ResponseEntity<List<ChartItemResponse>> getTopFilms() {
        return ResponseEntity.ok(enrichChartItems(chartService.getTopFilms()));
    }

    @GetMapping("/series")
    public ResponseEntity<List<ChartItemResponse>> getTopSeries() {
        return ResponseEntity.ok(enrichChartItems(chartService.getTopSeries()));
    }

    @GetMapping("/users")
    public ResponseEntity<List<ChartUserResponse>> getTopUsers() {
        return ResponseEntity.ok(chartService.getTopUsers());
    }

    @GetMapping("/trending")
    public ResponseEntity<List<TrendingItemResponse>> getTrending() {
        return ResponseEntity.ok(enrichTrendingItems(chartService.getTrending()));
    }

    // ── helpers ──

    private List<ChartItemResponse> enrichChartItems(List<ChartItemResponse> items) {
        return items.stream().map(item -> {
            try {
                var detail = tmdbService.getDetail(item.getTmdbId(), item.getContentType());
                return ChartItemResponse.builder()
                        .tmdbId(item.getTmdbId())
                        .contentType(item.getContentType())
                        .title(detail.getTitle())
                        .posterPath(detail.getPosterPath())
                        .averageRating(item.getAverageRating())
                        .totalVotes(item.getTotalVotes())
                        .build();
            } catch (Exception e) {
                log.warn("Impossibile arricchire chart item tmdbId={}: {}", item.getTmdbId(), e.getMessage());
                return item; // restituisce il DTO senza titolo piuttosto che far saltare tutta la lista
            }
        }).toList();
    }

    private List<TrendingItemResponse> enrichTrendingItems(List<TrendingItemResponse> items) {
        return items.stream().map(item -> {
            try {
                var detail = tmdbService.getDetail(item.getTmdbId(), item.getContentType());
                return TrendingItemResponse.builder()
                        .tmdbId(item.getTmdbId())
                        .contentType(item.getContentType())
                        .title(detail.getTitle())
                        .posterPath(detail.getPosterPath())
                        .weeklyReviewCount(item.getWeeklyReviewCount())
                        .ciakLogAverageRating(item.getCiakLogAverageRating())
                        .build();
            } catch (Exception e) {
                log.warn("Impossibile arricchire trending item tmdbId={}: {}", item.getTmdbId(), e.getMessage());
                return item;
            }
        }).toList();
    }
}