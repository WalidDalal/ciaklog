package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.ReviewCommentRequest;
import com.project.ciaklog.dto.response.ReviewCommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewCommentService {
    ReviewCommentResponse createComment(String username, UUID reviewId, ReviewCommentRequest dto);
    ReviewCommentResponse updateComment(String username, UUID commentId, ReviewCommentRequest dto);
    void deleteComment(String username, UUID commentId);
    Page<ReviewCommentResponse> getCommentsForReview(UUID reviewId, Pageable pageable);

    // Fix (auto-nascondimento autore, deciso): toggle reversibile, separato
    // da status/moderazione, nessun impatto sul punteggio
    ReviewCommentResponse setHiddenByAuthor(String username, UUID commentId, boolean hidden);
}
