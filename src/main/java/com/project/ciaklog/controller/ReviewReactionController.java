package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.ReactionRequest;
import com.project.ciaklog.dto.response.ReactionSummaryResponse;
import com.project.ciaklog.service.ReviewReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewReactionController {

    private final ReviewReactionService reactionService;

    // ── Recensioni ──

    @GetMapping("/api/reviews/{reviewId}/reaction")
    public ResponseEntity<ReactionSummaryResponse> getReviewReaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID reviewId) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(reactionService.getReviewSummary(username, reviewId));
    }

    @PutMapping("/api/reviews/{reviewId}/reaction")
    public ResponseEntity<ReactionSummaryResponse> setReviewReaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReactionRequest dto) {
        return ResponseEntity.ok(reactionService.setReactionOnReview(userDetails.getUsername(), reviewId, dto.getType()));
    }

    @DeleteMapping("/api/reviews/{reviewId}/reaction")
    public ResponseEntity<ReactionSummaryResponse> removeReviewReaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID reviewId) {
        return ResponseEntity.ok(reactionService.removeReactionFromReview(userDetails.getUsername(), reviewId));
    }

    // ── Risposte ──

    @GetMapping("/api/comments/{commentId}/reaction")
    public ResponseEntity<ReactionSummaryResponse> getCommentReaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID commentId) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(reactionService.getCommentSummary(username, commentId));
    }

    @PutMapping("/api/comments/{commentId}/reaction")
    public ResponseEntity<ReactionSummaryResponse> setCommentReaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID commentId,
            @Valid @RequestBody ReactionRequest dto) {
        return ResponseEntity.ok(reactionService.setReactionOnComment(userDetails.getUsername(), commentId, dto.getType()));
    }

    @DeleteMapping("/api/comments/{commentId}/reaction")
    public ResponseEntity<ReactionSummaryResponse> removeCommentReaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID commentId) {
        return ResponseEntity.ok(reactionService.removeReactionFromComment(userDetails.getUsername(), commentId));
    }
}
