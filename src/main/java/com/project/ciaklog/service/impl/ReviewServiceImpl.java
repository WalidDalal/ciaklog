package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.request.ReviewRequest;
import com.project.ciaklog.dto.request.ReviewUpdateRequest;
import com.project.ciaklog.dto.response.ReviewResponse;
import com.project.ciaklog.entity.*;
import com.project.ciaklog.exception.DuplicateResourceException;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.exception.ForbiddenException;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.repository.WatchEntryRepository;
import com.project.ciaklog.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int POINTS_CREATE_REVIEW = 10;

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final WatchEntryRepository watchEntryRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(String username, ReviewRequest dto) {
        User user = getUser(username);

        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Gli amministratori non possono pubblicare recensioni");
        }

        ContentType contentType = dto.getContentType();

        if (reviewRepository.existsByUserAndTmdbIdAndContentType(user, dto.getTmdbId(), contentType)) {
            throw new DuplicateResourceException("Hai già recensito questo contenuto");
        }

        boolean isWatched = watchEntryRepository
                .findByUserAndTmdbIdAndContentType(user, dto.getTmdbId(), contentType)
                .map(e -> e.getStatus() == WatchStatus.WATCHED)
                .orElse(false);

        if (!isWatched) {
            throw new BusinessRuleException("Puoi recensire solo contenuti che hai contrassegnato come 'Visto'");
        }

        Review review = Review.builder()
                .user(user)
                .tmdbId(dto.getTmdbId())
                .contentType(contentType)
                .rating(dto.getRating())
                .text(dto.getText())
                .build();

        reviewRepository.save(review);

        // +10 punti — fonte di verità unica: user.score persistito
        user.setScore(user.getScore() + POINTS_CREATE_REVIEW);
        userRepository.save(user);

        return toDTO(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(String username, UUID reviewId, ReviewUpdateRequest dto) {
        User user = getUser(username);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Non autorizzato a modificare questa recensione");
        }

        review.setRating(dto.getRating());
        review.setText(dto.getText());

        return toDTO(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteReview(String username, UUID reviewId) {
        User user = getUser(username);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Non autorizzato a eliminare questa recensione");
        }

        review.setStatus(ReviewStatus.REMOVED);
        reviewRepository.save(review);

        // -10 punti: la cancellazione volontaria revoca il bonus della pubblicazione
        user.setScore(Math.max(0, user.getScore() - POINTS_CREATE_REVIEW));
        userRepository.save(user);
    }

    @Override
    public Page<ReviewResponse> getReviewsForMedia(Long tmdbId, ContentType contentType, Pageable pageable) {
        return reviewRepository
                .findByTmdbIdAndContentTypeAndStatus(tmdbId, contentType, ReviewStatus.VISIBLE, pageable)
                .map(this::toDTO);
    }

    @Override
    public Page<ReviewResponse> getUserReviews(String username, Pageable pageable) {
        User user = getUser(username);
        return reviewRepository.findByUser(user, pageable).map(this::toDTO);
    }

    // ── helpers ──

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    private ReviewResponse toDTO(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .username(r.getUser().getUsername())
                .tmdbId(r.getTmdbId())
                .contentType(r.getContentType())
                .rating(r.getRating())
                .text(r.getText())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}