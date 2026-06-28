package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.response.ChartItemResponse;
import com.project.ciaklog.dto.response.ChartUserResponse;
import com.project.ciaklog.dto.response.TrendingItemResponse;
import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.Role;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.service.ChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChartServiceImpl implements ChartService {

    private static final int MIN_VOTES = 1; // soglia minima per entrare in classifica
    private static final int TOP_LIMIT  = 3;

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    public List<ChartItemResponse> getTopFilms() {
        return getTopByContentType(ContentType.MOVIE, TOP_LIMIT);
    }

    @Override
    public List<ChartItemResponse> getTopSeries() {
        return getTopByContentType(ContentType.TV, TOP_LIMIT);
    }

    @Override
    public List<ChartUserResponse> getTopUsers() {
        // Legge direttamente user.score persistito — nessun findAll(), nessuna query annidata
        return userRepository.findTop5ByRoleNotOrderByScoreDesc(Role.ADMIN)
                .stream()
                .filter(u -> u.getScore() > 0)
                .map(u -> ChartUserResponse.builder()
                        .username(u.getUsername())
                        .score(u.getScore())
                        .reviewCount(null) // non calcolato qui: richiederebbe una query extra per utente
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<TrendingItemResponse> getTrending() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        // Una sola query aggregata al DB — nessun findAll() + nessuna query per gruppo
        List<Object[]> rows = reviewRepository.findTrendingGrouped(weekAgo);

        return rows.stream()
                .limit(6)
                .map(row -> {
                    Long tmdbId       = (Long) row[0];
                    ContentType type  = (ContentType) row[1];
                    int weeklyCount   = ((Long) row[2]).intValue();

                    // Weighted average: necessita delle singole date di creazione —
                    // query separata ma solo per i 6 trending, non per tutti i record
                    List<Review> reviews = reviewRepository.findVisibleByTmdbIdAndContentType(tmdbId, type);
                    double avg = computeWeightedAverage(reviews);

                    return TrendingItemResponse.builder()
                            .tmdbId(tmdbId)
                            .contentType(type)
                            .posterPath(null)  // arricchito lato controller con TmdbService
                            .title(null)       // arricchito lato controller con TmdbService
                            .weeklyReviewCount(weeklyCount)
                            .ciakLogAverageRating(avg)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── helpers ──

    private List<ChartItemResponse> getTopByContentType(ContentType contentType, int limit) {
        // Una sola query aggregata — nessun findAll()
        List<Object[]> rows = reviewRepository.findAggregatedByContentType(contentType, MIN_VOTES);

        return rows.stream()
                .map(row -> {
                    Long tmdbId    = (Long) row[0];
                    long count     = (Long) row[2];
                    double avgRaw  = (Double) row[3];
                    double avg     = Math.round(avgRaw * 10.0) / 10.0;

                    return ChartItemResponse.builder()
                            .tmdbId(tmdbId)
                            .contentType(contentType)
                            .posterPath(null) // arricchito lato controller con TmdbService
                            .title(null)      // arricchito lato controller con TmdbService
                            .averageRating(avg)
                            .totalVotes((int) count)
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
            long days = ChronoUnit.DAYS.between(r.getCreatedAt(), LocalDateTime.now());
            double weight = 1.0 / (1 + (double) days / 30);
            weightedSum += r.getRating() * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? 0 : Math.round((weightedSum / totalWeight) * 10.0) / 10.0;
    }
}