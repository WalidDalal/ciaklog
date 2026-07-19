package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.ReviewCommentRequest;
import com.project.ciaklog.dto.response.ReviewCommentResponse;
import com.project.ciaklog.service.ReviewCommentService;
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
@RequiredArgsConstructor
public class ReviewCommentController {

    private final ReviewCommentService reviewCommentService;

    // Fix (auto-nascondimento autore): stesso principio delle recensioni —
    // l'autore deve continuare a vedere le proprie risposte nascoste.
    // Fix (dashboard admin — commenti nascosti): un Admin deve poter vedere
    // anche le risposte nascoste dagli autori, per poter valutare le segnalazioni
    @GetMapping("/api/reviews/{reviewId}/comments")
    public ResponseEntity<Page<ReviewCommentResponse>> getCommentsForReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID reviewId,
            Pageable pageable) {
        String viewerUsername = userDetails != null ? userDetails.getUsername() : null;
        boolean isAdmin = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(reviewCommentService.getCommentsForReview(reviewId, viewerUsername, isAdmin, pageable));
    }

    @PostMapping("/api/reviews/{reviewId}/comments")
    public ResponseEntity<ReviewCommentResponse> createComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewCommentRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewCommentService.createComment(userDetails.getUsername(), reviewId, dto));
    }

    @PutMapping("/api/comments/{id}")
    public ResponseEntity<ReviewCommentResponse> updateComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody ReviewCommentRequest dto) {
        return ResponseEntity.ok(reviewCommentService.updateComment(userDetails.getUsername(), id, dto));
    }

    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        reviewCommentService.deleteComment(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // Fix (auto-nascondimento autore, deciso): reversibile, separato dalla moderazione
    @PatchMapping("/api/comments/{id}/visibility")
    public ResponseEntity<ReviewCommentResponse> setHidden(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @RequestParam boolean hidden) {
        return ResponseEntity.ok(reviewCommentService.setHiddenByAuthor(userDetails.getUsername(), id, hidden));
    }
}
