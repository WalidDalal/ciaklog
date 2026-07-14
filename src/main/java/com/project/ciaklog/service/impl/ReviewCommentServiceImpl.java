package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.request.ReviewCommentRequest;
import com.project.ciaklog.dto.response.ReviewCommentResponse;
import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.ReviewComment;
import com.project.ciaklog.entity.ReviewStatus;
import com.project.ciaklog.entity.Role;
import com.project.ciaklog.entity.User;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.exception.ForbiddenException;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.ReviewCommentRepository;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.service.ReviewCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Step 1 della feature "Risposte alle recensioni": solo CRUD base.
// Moderazione (segnalazioni, auto-hide, cascata, "nascondi direttamente"),
// punteggio da reazioni e auto-nascondimento autore arrivano negli step successivi.
@Service
@RequiredArgsConstructor
public class ReviewCommentServiceImpl implements ReviewCommentService {

    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReviewCommentResponse createComment(String username, UUID reviewId, ReviewCommentRequest dto) {
        User user = getUser(username);

        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Gli amministratori non possono pubblicare risposte");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));

        if (review.getStatus() != ReviewStatus.VISIBLE) {
            throw new BusinessRuleException("Non è possibile rispondere a una recensione non visibile");
        }

        ReviewComment comment = ReviewComment.builder()
                .review(review)
                .author(user)
                .text(dto.getText())
                .build();

        return toDTO(reviewCommentRepository.save(comment));
    }

    @Override
    @Transactional
    public ReviewCommentResponse updateComment(String username, UUID commentId, ReviewCommentRequest dto) {
        User user = getUser(username);
        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Risposta non trovata"));

        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("Non autorizzato a modificare questa risposta");
        }

        comment.setText(dto.getText());

        return toDTO(reviewCommentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(String username, UUID commentId) {
        User user = getUser(username);
        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Risposta non trovata"));

        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("Non autorizzato a eliminare questa risposta");
        }

        comment.setStatus(ReviewStatus.REMOVED);
        reviewCommentRepository.save(comment);
    }

    // Fix (auto-nascondimento autore, deciso): toggle reversibile, separato
    // da status/moderazione — non tocca il punteggio e non genera nessun Report
    @Override
    @Transactional
    public ReviewCommentResponse setHiddenByAuthor(String username, UUID commentId, boolean hidden) {
        User user = getUser(username);
        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Risposta non trovata"));

        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("Non autorizzato");
        }

        comment.setHiddenByAuthor(hidden);
        return toDTO(reviewCommentRepository.save(comment));
    }

    @Override
    public Page<ReviewCommentResponse> getCommentsForReview(UUID reviewId, Pageable pageable) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));

        return reviewCommentRepository.findByReviewAndStatus(review, ReviewStatus.VISIBLE, pageable)
                .map(this::toDTO);
    }

    // ── helpers ──

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    private ReviewCommentResponse toDTO(ReviewComment c) {
        return ReviewCommentResponse.builder()
                .id(c.getId())
                .reviewId(c.getReview().getId())
                .authorUsername(c.getAuthor().getUsername())
                .text(c.getText())
                .status(c.getStatus())
                .hiddenByAuthor(c.isHiddenByAuthor())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
