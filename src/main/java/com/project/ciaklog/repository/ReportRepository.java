package com.project.ciaklog.repository;

import com.project.ciaklog.entity.Report;
import com.project.ciaklog.entity.ReportStatus;
import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.ReviewComment;
import com.project.ciaklog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    // Dashboard Admin — lista segnalazioni per stato, paginata
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    // Per verificare segnalazione duplicata
    boolean existsByReporterAndReview(User reporter, Review review);

    // Per trovare tutte le segnalazioni su una review
    List<Report> findByReview(Review review);

    // Conta il numero totale di segnalazioni ricevute da una review (per l'auto-hide)
    long countByReview(Review review);

    // CountByReview contava
    // TUTTE le segnalazioni storiche, incluse quelle già REJECTED in passato —
    // una recensione con 2 segnalazioni rifiutate mesi fa e 1 nuova PENDING
    // veniva auto-nascosta come se avesse 2+ segnalazioni attive, quando ne
    // ha solo 1 davvero da valutare. La soglia deve contare solo le PENDING.
    long countByReviewAndStatus(Review review, ReportStatus status);

    // Fix N+1: carica tutti i report per una lista di review in una sola query
    List<Report> findAllByReviewIn(List<Review> reviews);

    // Stessi metodi di sopra ma per ReviewComment
    boolean existsByReporterAndReviewComment(User reporter, ReviewComment reviewComment);

    List<Report> findByReviewComment(ReviewComment reviewComment);

    long countByReviewComment(ReviewComment reviewComment);

    // Fix (Logica Moderazione — stessa correzione per le risposte)
    long countByReviewCommentAndStatus(ReviewComment reviewComment, ReportStatus status);

    // Segnalazioni ricevute da mezzanotte di oggi,
    // per la card operativa "X segnalazioni oggi"
    long countByCreatedAtAfter(java.time.LocalDateTime since);
}