package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.ReviewRequest;
import com.project.ciaklog.dto.request.ReviewUpdateRequest;
import com.project.ciaklog.dto.response.ReviewResponse;
import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // Serve sapere CHI guarda per far vedere
    // all'autore la propria recensione anche se l'ha nascosta — userDetails è
    // nullable qui (endpoint pubblico, un visitatore anonimo può guardarla)
    // Un Admin deve
    // vedere anche quelle nascoste dall'autore o con status HIDDEN
    @GetMapping("/media/{contentType}/{tmdbId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsForMedia(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable ContentType contentType,
            @PathVariable Long tmdbId,
            Pageable pageable) {
        String viewerUsername = userDetails != null ? userDetails.getUsername() : null;
        boolean isAdmin = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(reviewService.getReviewsForMedia(tmdbId, contentType, viewerUsername, isAdmin, pageable));
    }

    //
    // userDetails è nullable, un profilo pubblico è visibile anche da anonimo —
    // ma quando c'è, serve per far vedere all'autore le proprie nascoste e
    // all'Admin quelle in HIDDEN, esattamente come già succede per la pagina film
    @GetMapping("/user/{username}")
    public ResponseEntity<Page<ReviewResponse>> getUserReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String username,
            Pageable pageable) {
        String viewerUsername = userDetails != null ? userDetails.getUsername() : null;
        boolean isAdmin = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(reviewService.getUserReviews(username, viewerUsername, isAdmin, pageable));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(userDetails.getUsername(), dto));
    }

    // PUT usa ReviewUpdateRequest: tmdbId e contentType non servono, la review è già identificata dall'id
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody ReviewUpdateRequest dto) {
        return ResponseEntity.ok(reviewService.updateReview(userDetails.getUsername(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        reviewService.deleteReview(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // Reversibile, separato dalla
    // moderazione — l'autore nasconde/rimostra la propria recensione senza
    // penalità e senza generare nessuna segnalazione
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ReviewResponse> setHidden(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @RequestParam boolean hidden) {
        return ResponseEntity.ok(reviewService.setHiddenByAuthor(userDetails.getUsername(), id, hidden));
    }
}
