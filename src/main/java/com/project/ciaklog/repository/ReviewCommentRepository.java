package com.project.ciaklog.repository;

import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.ReviewComment;
import com.project.ciaklog.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewCommentRepository extends JpaRepository<ReviewComment, UUID> {

    // JOIN FETCH author — evita LazyInitializationException in toDTO()
    // Fix (auto-nascondimento autore): esclusa anche dalla lista pubblica se
    // hiddenByAuthor = true, pur restando VISIBLE come status
    @Query("""
            SELECT c FROM ReviewComment c
            JOIN FETCH c.author
            WHERE c.review = :review AND c.status = :status
              AND c.hiddenByAuthor = false
            """)
    Page<ReviewComment> findByReviewAndStatus(
            @Param("review") Review review,
            @Param("status") ReviewStatus status,
            Pageable pageable);

    // Fix (cascata moderazione): versione non paginata, usata da ReportServiceImpl
    // per nascondere/rimuovere tutte le risposte quando la recensione madre sparisce
    List<ReviewComment> findAllByReviewAndStatus(Review review, ReviewStatus status);
}
