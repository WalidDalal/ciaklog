package com.project.ciaklog.repository;

import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.ReviewComment;
import com.project.ciaklog.entity.ReviewReaction;
import com.project.ciaklog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, UUID> {

    Optional<ReviewReaction> findByUserAndReview(User user, Review review);

    Optional<ReviewReaction> findByUserAndReviewComment(User user, ReviewComment reviewComment);

    // Conteggi per tipo — usati per il riepilogo mostrato sotto ogni recensione/risposta
    @Query("SELECT r.type AS type, COUNT(r) AS count FROM ReviewReaction r WHERE r.review = :review GROUP BY r.type")
    List<ReactionCount> countByReviewGroupedByType(@Param("review") Review review);

    @Query("SELECT r.type AS type, COUNT(r) AS count FROM ReviewReaction r WHERE r.reviewComment = :comment GROUP BY r.type")
    List<ReactionCount> countByReviewCommentGroupedByType(@Param("comment") ReviewComment comment);

    // Fix (coerenza punteggio alla rimozione, Step futuro): conteggio totale
    // reazioni ricevute — calcolato al volo via COUNT, MAI un contatore salvato
    // a parte (deciso)
    long countByReview(Review review);

    long countByReviewComment(ReviewComment reviewComment);

    interface ReactionCount {
        String getType();
        Long getCount();
    }
}
