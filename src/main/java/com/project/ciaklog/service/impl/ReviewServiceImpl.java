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

        // Fix (🔴 trovato nei test funzionali): deleteReview fa un soft delete
        // (status → REMOVED, la riga resta nel DB), ma questo controllo usava
        // existsByUserAndTmdbIdAndContentType (senza filtro sullo status) —
        // quindi chi eliminava la propria recensione e provava a riscriverne
        // una nuova sullo stesso titolo restava bloccato per sempre con
        // "Hai già recensito questo contenuto", anche se per lui/lei risultava
        // cancellata. Stessa variante AndStatusNot già usata altrove nel
        // codice (es. WatchEntryServiceImpl) per lo stesso tipo di controllo.
        if (reviewRepository.existsByUserAndTmdbIdAndContentTypeAndStatusNot(user, dto.getTmdbId(), contentType, ReviewStatus.REMOVED)) {
            throw new DuplicateResourceException("Hai già recensito questo contenuto");
        }

        // Fix (🔴 regola violata, trovato nei test funzionali: "non può mai
        // esistere un'entry WATCHED senza recensione, né una recensione senza
        // entry WATCHED"): prima si richiedeva che l'entry fosse GIÀ WATCHED
        // per poter recensire — costringendo un passaggio a due tempi (segna
        // Visto, POI recensisci, magari su una pagina diversa) che lasciava
        // una finestra in cui l'entry restava WATCHED senza recensione per
        // sempre, se l'utente cambiava pagina prima di scrivere la
        // recensione. Ora è questo metodo stesso a far scattare il passaggio
        // a WATCHED, DOPO aver validato tutto (testo/rating), nella stessa
        // transazione: se la validazione fallisce non cambia nulla, se la
        // recensione va a buon fine l'entry diventa WATCHED nello stesso
        // istante — non esiste più uno stato intermedio salvato sul DB.
        // Basta che il contenuto sia in libreria (in un qualunque stato),
        // non più che sia già WATCHED. Il passaggio diretto a WATCHED tramite
        // l'endpoint di update status separato e tramite l'aggiunta diretta
        // alla libreria sono stati bloccati (vedi WatchEntryServiceImpl) —
        // l'unico modo per arrivare a WATCHED è passare da qui.
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
    public ReviewResponse getMyReviewForMedia(String username, Long tmdbId, ContentType contentType) {
        // Fix (Dettaglio Film/Serie): indipendente dalla paginazione, vedi
        // commento sull'interfaccia ReviewService per il motivo.
        // Fix (bug in produzione — 500/LazyInitializationException): usa la
        // variante con JOIN FETCH r.user (vedi commento sulla query nel
        // repository) — toDTO() legge r.getUser().getUsername(), non lo
        // "user" passato qui come secondo argomento, quindi serve che
        // r.getUser() sia già inizializzato quando si esce da questo metodo
        // (non è @Transactional, la sessione Hibernate si chiude alla fine
        // della query).
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
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .title(title)
                .posterPath(posterPath)
                .build();
    }
}
