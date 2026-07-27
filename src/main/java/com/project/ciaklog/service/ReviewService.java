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

    // Fix (Dettaglio Film/Serie — recensioni troncate a 20): la pagina film
    // ora pagina davvero le recensioni (non le carica più tutte in un colpo
    // solo, di default 20 senza "carica altre"), quindi la propria
    // recensione ("myReview") non si può più cercare dentro la sola pagina
    // caricata — potrebbe non esserci. Query dedicata e indipendente dalla
    // paginazione, che sfrutta l'indice univoco utente+media già esistente
    // (findByUserAndTmdbIdAndContentType, usata anche per evitare doppie
    // recensioni in createReview).
    ReviewResponse getMyReviewForMedia(String username, Long tmdbId, ContentType contentType);
    Page<ReviewResponse> getUserReviews(String username, String viewerUsername, boolean isAdmin, Pageable pageable);

    // Toggle reversibile, separato
    // da status/moderazione, nessun impatto sul punteggio
    ReviewResponse setHiddenByAuthor(String username, UUID reviewId, boolean hidden);
}
