package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.ReviewRequest;
import com.project.ciaklog.dto.response.ReviewResponse;
import com.project.ciaklog.entity.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {
    ReviewResponse createReview(String username, ReviewRequest dto);
    ReviewResponse updateReview(String username, UUID reviewId, ReviewRequest dto);
    void deleteReview(String username, UUID reviewId);
    Page<ReviewResponse> getReviewsForMedia(Long tmdbId, ContentType contentType, Pageable pageable);
    Page<ReviewResponse> getUserReviews(String username, Pageable pageable);
}