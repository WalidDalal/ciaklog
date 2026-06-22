package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.response.ChartItemResponse;
import com.project.ciaklog.dto.response.ChartUserResponse;
import com.project.ciaklog.dto.response.TrendingItemResponse;
import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.ReviewStatus;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.service.ChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChartServiceImpl implements ChartService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    public List<ChartItemResponse> getTopFilms() {
        return getTopByContentType(ContentType.MOVIE, 3);
    }

    @Override
    public List<ChartItemResponse> getTopSeries() {
        return getTopByContentType(ContentType.TV, 3);
    }

    @Override
    public List<ChartUserResponse> getTopUsers() {
        // score = count review + bonus per review con testo
        // TODO Fase 10: sostituire con query aggregata per evitare findAll()
        return userRepository.findAll().stream()
                .map(user -> {
                    List<Review> reviews = reviewRepository
                            .findByUser(user, org.springframework.data.domain.Pageable.unpaged())
                            .getContent()
                            .stream()
                            .filter(r -> r.getStatus() == ReviewStatus.VISIBLE)
                            .toList();

                    int score = reviews.size() + (int) reviews.stream()
                            .filter(r -> r.getText() != null && !r.getText().isBlank())
                            .count();

                    return ChartUserResponse.builder()
                            .username(user.getUsername())
                            .score(score)
                            .reviewCount(reviews.size())
                            .build();
                })
                .filter(u -> u.getScore() > 0)
                .sorted(Comparator.comparingInt(ChartUserResponse::getScore).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrendingItemResponse> getTrending() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        // Raggruppa recensioni recenti per tmdbId+contentType
        // TODO Fase 10: sostituire con query aggregata per evitare findAll()
        List<Review> recentReviews = reviewRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReviewStatus.VISIBLE && r.getCreatedAt().isAfter(weekAgo))
                .toList();

        Map<String, List<Review>> grouped = recentReviews.stream()
                .collect(Collectors.groupingBy(r -> r.getTmdbId() + "_" + r.getContentType()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<Review> group = entry.getValue();
                    Review sample = group.get(0);
                    double weightedAvg = computeWeightedAverage(
                            reviewRepository.findVisibleByTmdbIdAndContentType(sample.getTmdbId(), sample.getContentType()));

                    return TrendingItemResponse.builder()
                            .tmdbId(sample.getTmdbId())
                            .contentType(sample.getContentType())
                            .posterPath(null) // arricchito lato controller con TmdbService
                            .title(null)      // arricchito lato controller con TmdbService
                            .weeklyReviewCount(group.size())
                            .ciakLogAverageRating(weightedAvg)
                            .build();
                })
                .sorted(Comparator.comparingInt(TrendingItemResponse::getWeeklyReviewCount).reversed())
                .limit(6)
                .collect(Collectors.toList());
    }

    // ── helpers ──

    private List<ChartItemResponse> getTopByContentType(ContentType contentType, int limit) {
        // Raggruppa tutte le review VISIBLE per tmdbId
        Map<Long, List<Review>> byTmdb = reviewRepository.findAll().stream()
                .filter(r -> r.getContentType() == contentType && r.getStatus() == ReviewStatus.VISIBLE)
                .collect(Collectors.groupingBy(Review::getTmdbId));

        return byTmdb.entrySet().stream()
                .map(entry -> {
                    List<Review> reviews = entry.getValue();
                    double avg = computeWeightedAverage(reviews);
                    Review sample = reviews.get(0);
                    return ChartItemResponse.builder()
                            .tmdbId(sample.getTmdbId())
                            .contentType(contentType)
                            .posterPath(null) // arricchito lato controller con TmdbService
                            .title(null)      // arricchito lato controller con TmdbService
                            .averageRating(avg)
                            .totalVotes(reviews.size())
                            .build();
                })
                .sorted(Comparator.comparingDouble(ChartItemResponse::getAverageRating).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Weighted average: peso = 1 / (1 + giorni_da_creazione / 30)
     * Voto recente pesa ~1, voto di 30gg fa pesa ~0.5
     */
    private double computeWeightedAverage(List<Review> reviews) {
        if (reviews.isEmpty()) return 0.0;
        double weightedSum = 0;
        double totalWeight = 0;
        for (Review r : reviews) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(r.getCreatedAt(), LocalDateTime.now());
            double weight = 1.0 / (1 + (double) days / 30);
            weightedSum += r.getRating() * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? 0 : Math.round((weightedSum / totalWeight) * 10.0) / 10.0;
    }
}
