package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.response.ReactionSummaryResponse;
import com.project.ciaklog.entity.*;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.ReviewCommentRepository;
import com.project.ciaklog.repository.ReviewReactionRepository;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.service.ReviewReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewReactionServiceImpl implements ReviewReactionService {

    private final ReviewReactionRepository reactionRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReactionSummaryResponse setReactionOnReview(String username, UUID reviewId, ReactionType type) {
        User user = getUser(username);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));

        // Fix: non farmabile — non ci si può reagire da soli (stesso principio
        // del divieto di auto-segnalazione)
        if (review.getUser().getId().equals(user.getId())) {
            throw new BusinessRuleException("Non puoi reagire alla tua recensione");
        }

        Optional<ReviewReaction> existing = reactionRepository.findByUserAndReview(user, review);
        if (existing.isPresent()) {
            // Cambio tipo: sostituisce la precedente, NESSUN punto in più
            // all'autore (è comunque una sola reazione da questo utente)
            existing.get().setType(type);
            reactionRepository.save(existing.get());
        } else {
            reactionRepository.save(ReviewReaction.builder().user(user).review(review).type(type).build());
            adjustAuthorScore(review.getUser(), +1);
        }

        return buildReviewSummary(user, review);
    }

    @Override
    @Transactional
    public ReactionSummaryResponse removeReactionFromReview(String username, UUID reviewId) {
        User user = getUser(username);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));

        reactionRepository.findByUserAndReview(user, review).ifPresent(r -> {
            reactionRepository.delete(r);
            adjustAuthorScore(review.getUser(), -1);
        });

        return buildReviewSummary(user, review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReactionSummaryResponse getReviewSummary(String username, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));
        User user = (username != null) ? getUser(username) : null;
        return buildReviewSummary(user, review);
    }

    @Override
    @Transactional
    public ReactionSummaryResponse setReactionOnComment(String username, UUID commentId, ReactionType type) {
        User user = getUser(username);
        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Risposta non trovata"));

        if (comment.getAuthor().getId().equals(user.getId())) {
            throw new BusinessRuleException("Non puoi reagire alla tua risposta");
        }

        Optional<ReviewReaction> existing = reactionRepository.findByUserAndReviewComment(user, comment);
        if (existing.isPresent()) {
            existing.get().setType(type);
            reactionRepository.save(existing.get());
        } else {
            reactionRepository.save(ReviewReaction.builder().user(user).reviewComment(comment).type(type).build());
            adjustAuthorScore(comment.getAuthor(), +1);
        }

        return buildCommentSummary(user, comment);
    }

    @Override
    @Transactional
    public ReactionSummaryResponse removeReactionFromComment(String username, UUID commentId) {
        User user = getUser(username);
        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Risposta non trovata"));

        reactionRepository.findByUserAndReviewComment(user, comment).ifPresent(r -> {
            reactionRepository.delete(r);
            adjustAuthorScore(comment.getAuthor(), -1);
        });

        return buildCommentSummary(user, comment);
    }

    @Override
    @Transactional(readOnly = true)
    public ReactionSummaryResponse getCommentSummary(String username, UUID commentId) {
        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Risposta non trovata"));
        User user = (username != null) ? getUser(username) : null;
        return buildCommentSummary(user, comment);
    }

    // ── helpers ──

    // Fix: +1 punto per ogni reazione RICEVUTA (deciso) — mai sotto zero
    private void adjustAuthorScore(User author, int delta) {
        author.setScore(Math.max(0, author.getScore() + delta));
        userRepository.save(author);
    }

    private ReactionSummaryResponse buildReviewSummary(User user, Review review) {
        Map<ReactionType, Long> counts = toCountMap(reactionRepository.countByReviewGroupedByType(review));
        ReactionType mine = (user != null)
                ? reactionRepository.findByUserAndReview(user, review).map(ReviewReaction::getType).orElse(null)
                : null;
        return ReactionSummaryResponse.builder().counts(counts).myReaction(mine).build();
    }

    private ReactionSummaryResponse buildCommentSummary(User user, ReviewComment comment) {
        Map<ReactionType, Long> counts = toCountMap(reactionRepository.countByReviewCommentGroupedByType(comment));
        ReactionType mine = (user != null)
                ? reactionRepository.findByUserAndReviewComment(user, comment).map(ReviewReaction::getType).orElse(null)
                : null;
        return ReactionSummaryResponse.builder().counts(counts).myReaction(mine).build();
    }

    private Map<ReactionType, Long> toCountMap(java.util.List<ReviewReactionRepository.ReactionCount> rows) {
        Map<ReactionType, Long> map = new EnumMap<>(ReactionType.class);
        for (ReviewReactionRepository.ReactionCount row : rows) {
            map.put(ReactionType.valueOf(row.getType()), row.getCount());
        }
        return map;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }
}
