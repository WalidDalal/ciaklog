package com.project.ciaklog.service;

import com.project.ciaklog.dto.response.ReactionSummaryResponse;
import com.project.ciaklog.entity.ReactionType;

import java.util.UUID;

public interface ReviewReactionService {
    ReactionSummaryResponse setReactionOnReview(String username, UUID reviewId, ReactionType type);
    ReactionSummaryResponse removeReactionFromReview(String username, UUID reviewId);
    ReactionSummaryResponse getReviewSummary(String username, UUID reviewId);

    ReactionSummaryResponse setReactionOnComment(String username, UUID commentId, ReactionType type);
    ReactionSummaryResponse removeReactionFromComment(String username, UUID commentId);
    ReactionSummaryResponse getCommentSummary(String username, UUID commentId);
}
