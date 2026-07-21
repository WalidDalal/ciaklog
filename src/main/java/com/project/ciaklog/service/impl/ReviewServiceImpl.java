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

        // Testo obbligatorio solo per le recensioni di contenuti visti
        if (dto.getText() == null || dto.getText().isBlank()) {
            throw new BusinessRuleException("Il testo della recensione è obbligatorio per i contenuti visti");
        }

        Review review = Review.builder()
                .user(user)
                .tmdbId(dto.getTmdbId())
                .contentType(contentType)
                .rating(dto.getRating())
                .text(dto.getText())
                .build();

        reviewRepository.save(review);

        user.setScore(user.getScore() + POINTS_CREATE_REVIEW);
        userRepository.save(user);

        return toDTO(review, user);
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

        return toDTO(reviewRepository.save(review), user);
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

        user.setScore(Math.max(0, user.getScore() - POINTS_CREATE_REVIEW));
        userRepository.save(user);
    }

    // Toggle reversibile e separato
    // da status/moderazione — nessun impatto su punteggio o violationCount,
    // e non genera nessun Report (quindi non finisce mai nella coda admin)
    @Override
    @Transactional
    public ReviewResponse setHiddenByAuthor(String username, UUID reviewId, boolean hidden) {
        User user = getUser(username);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Non autorizzato");
        }

        review.setHiddenByAuthor(hidden);
        return toDTO(reviewRepository.save(review), user);
    }

    @Override
    public Page<ReviewResponse> getReviewsForMedia(Long tmdbId, ContentType contentType, String viewerUsername, boolean isAdmin, Pageable pageable) {
        return reviewRepository
                .findByTmdbIdAndContentTypeAndStatus(tmdbId, contentType, ReviewStatus.VISIBLE, viewerUsername, isAdmin, pageable)
                .map(r -> toDTO(r, r.getUser()));
    }

    @Override
    public Page<ReviewResponse> getUserReviews(String username, String viewerUsername, boolean isAdmin, Pageable pageable) {
        User user = getUser(username);
        // Caricare tutte le WatchEntry in una sola query per evitare N+1
        java.util.Map<String, WatchEntry> entryMap = new java.util.HashMap<>();
        watchEntryRepository.findAllByUser(user).forEach(e ->
                entryMap.put(e.getTmdbId() + "_" + e.getContentType(), e)
        );
        return reviewRepository.findByUser(user, ReviewStatus.VISIBLE, viewerUsername, isAdmin, pageable)
                .map(r -> toDTOWithMap(r, entryMap));
    }

    // ── helpers ──

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    private ReviewResponse toDTOWithMap(Review r, java.util.Map<String, WatchEntry> entryMap) {
        WatchEntry entry = entryMap.get(r.getTmdbId() + "_" + r.getContentType());
        return ReviewResponse.builder()
                .id(r.getId())
                .username(r.getUser().getUsername())
                .tmdbId(r.getTmdbId())
                .contentType(r.getContentType())
                .rating(r.getRating())
                .text(r.getText())
                .status(r.getStatus())
                .hiddenByAuthor(r.isHiddenByAuthor())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .title(entry != null ? entry.getTitle() : null)
                .posterPath(entry != null ? entry.getPosterPath() : null)
                .build();
    }

    private ReviewResponse toDTO(Review r, User user) {
        // Cerca title e posterPath nella WatchEntry dell'utente per questo contenuto
        String title = null;
        String posterPath = null;
        var entry = watchEntryRepository
                .findByUserAndTmdbIdAndContentType(user, r.getTmdbId(), r.getContentType())
                .orElse(null);
        if (entry != null) {
            title = entry.getTitle();
            posterPath = entry.getPosterPath();
        }

        return ReviewResponse.builder()
                .id(r.getId())
                .username(r.getUser().getUsername())
                .tmdbId(r.getTmdbId())
                .contentType(r.getContentType())
                .rating(r.getRating())
                .text(r.getText())
                .status(r.getStatus())
                .hiddenByAuthor(r.isHiddenByAuthor())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .title(title)
                .posterPath(posterPath)
                .build();
    }
}
