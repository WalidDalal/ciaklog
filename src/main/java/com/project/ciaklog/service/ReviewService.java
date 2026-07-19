package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.ReviewRequest;
import com.project.ciaklog.dto.request.ReviewUpdateRequest;
import com.project.ciaklog.dto.response.ReviewResponse;
import com.project.ciaklog.entity.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {
    ReviewResponse createReview(String username, ReviewRequest dto);
    ReviewResponse updateReview(String username, UUID reviewId, ReviewUpdateRequest dto);
    void deleteReview(String username, UUID reviewId);
    Page<ReviewResponse> getReviewsForMedia(Long tmdbId, ContentType contentType, String viewerUsername, boolean isAdmin, Pageable pageable);
    Page<ReviewResponse> getUserReviews(String username, String viewerUsername, boolean isAdmin, Pageable pageable);

    // Fix (auto-nascondimento autore, deciso): toggle reversibile, separato
    // da status/moderazione, nessun impatto sul punteggio
    ReviewResponse setHiddenByAuthor(String username, UUID reviewId, boolean hidden);
}
