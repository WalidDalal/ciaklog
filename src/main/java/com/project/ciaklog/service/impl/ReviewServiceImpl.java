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

import java.time.LocalDate;

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

        // Il vincolo unique su reviews è (user_id, tmdb_id, content_type)
        // SENZA lo status — MySQL/InnoDB non supporta indici unique
        // parziali. Se esiste già una riga REMOVED (soft-delete), va
        // resuscitata con un update invece di inserirne una nuova,
        // altrimenti va in conflitto col vincolo.
        if (reviewRepository.existsByUserAndTmdbIdAndContentTypeAndStatusNot(user, dto.getTmdbId(), contentType, ReviewStatus.REMOVED)) {
            throw new DuplicateResourceException("Hai già recensito questo contenuto");
        }

        // Non può mai esistere un'entry WATCHED senza recensione, né una
        // recensione senza entry WATCHED — questo metodo stesso fa scattare
        // il passaggio a WATCHED, dopo la validazione, nella stessa
        // transazione. Basta che il contenuto sia in libreria in un
        // qualunque stato; il passaggio diretto a WATCHED tramite update
        // status o aggiunta diretta è bloccato altrove (WatchEntryServiceImpl).
        WatchEntry entry = watchEntryRepository
                .findByUserAndTmdbIdAndContentType(user, dto.getTmdbId(), contentType)
                .orElseThrow(() -> new BusinessRuleException("Aggiungi questo contenuto alla libreria prima di recensirlo"));

        // Testo obbligatorio solo per le recensioni di contenuti visti
        if (dto.getText() == null || dto.getText().isBlank()) {
            throw new BusinessRuleException("Il testo della recensione è obbligatorio per i contenuti visti");
        }

        if (entry.getStatus() != WatchStatus.WATCHED) {
            entry.setStatus(WatchStatus.WATCHED);
            entry.setWatchedDate(LocalDate.now());
            watchEntryRepository.save(entry);
        }

        Review review = reviewRepository.findByUserAndTmdbIdAndContentType(user, dto.getTmdbId(), contentType)
                .orElseGet(() -> Review.builder()
                        .user(user)
                        .tmdbId(dto.getTmdbId())
                        .contentType(contentType)
                        .build());
        review.setRating(dto.getRating());
        review.setText(dto.getText());
        review.setStatus(ReviewStatus.VISIBLE);
        // Riga resuscitata da una vecchia eliminazione: azzera anche gli
        // altri flag di visibilità, altrimenti la nuova recensione
        // resterebbe nascosta per un motivo ormai non più valido
        review.setHiddenByAuthor(false);
        review.setHiddenBySuspension(false);
        review.setHiddenByDeletion(false);

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

        // Eliminando la recensione, l'entry WATCHED viene rimossa
        // completamente invece di retrocedere a un altro stato
        WatchEntry entry = watchEntryRepository
                .findByUserAndTmdbIdAndContentType(user, review.getTmdbId(), review.getContentType())
                .orElse(null);
        if (entry != null && entry.getStatus() == WatchStatus.WATCHED) {
            watchEntryRepository.delete(entry);
        }

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
    public ReviewResponse getMyReviewForMedia(String username, Long tmdbId, ContentType contentType) {
        // Usa la variante con JOIN FETCH r.user (vedi repository) — non
        // essendo @Transactional, r.getUser() deve arrivare già inizializzato
        User user = getUser(username);
        return reviewRepository.findByUserAndTmdbIdAndContentTypeFetchUser(user, tmdbId, contentType)
                .map(r -> toDTO(r, user))
                .orElse(null);
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
                .hiddenBySuspension(r.isHiddenBySuspension())
                .hiddenByDeletion(r.isHiddenByDeletion())
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
                .hiddenBySuspension(r.isHiddenBySuspension())
                .hiddenByDeletion(r.isHiddenByDeletion())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .title(title)
                .posterPath(posterPath)
                .build();
    }
}
