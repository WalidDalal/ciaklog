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

    // Conteggi per stato (AiServiceImpl)
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    // Varianti non paginate: getReports() raggruppa i risultati per bersaglio
    // (review/commento) in Java e pagina i GRUPPI, non le righe, altrimenti
    // segnalazioni sullo stesso bersaglio finirebbero su pagine diverse.
    // ORDER BY esplicito necessario: senza, il DB non garantisce lo stesso
    // ordine tra due chiamate, e con timestamp identici tra segnalazioni
    // (comune nei dati di test) la stessa pagina può restituire contenuti
    // diversi a chiamate successive.
    List<Report> findByStatusAndReviewIsNotNullOrderByCreatedAtDescIdAsc(ReportStatus status);
    List<Report> findByStatusAndReviewCommentIsNotNullOrderByCreatedAtDescIdAsc(ReportStatus status);
    List<Report> findByReviewIsNotNullOrderByCreatedAtDescIdAsc();
    List<Report> findByReviewCommentIsNotNullOrderByCreatedAtDescIdAsc();
    List<Report> findByStatusOrderByCreatedAtDescIdAsc(ReportStatus status);

    boolean existsByReporterAndReview(User reporter, Review review);

    List<Report> findByReview(Review review);

    long countByReview(Review review);

    // Solo le PENDING contano per la soglia di auto-hide — le REJECTED
    // storiche non devono farla scattare
    long countByReviewAndStatus(Review review, ReportStatus status);

    List<Report> findAllByReviewIn(List<Review> reviews);

    // Segnalazioni PENDING sui contenuti di un utente — usate per
    // l'archiviazione automatica quando l'account diventa
    // DELETED/PERMANENTLY_SUSPENDED
    List<Report> findAllByReviewInAndStatus(List<Review> reviews, ReportStatus status);
    List<Report> findAllByReviewCommentInAndStatus(List<ReviewComment> comments, ReportStatus status);

    boolean existsByReporterAndReviewComment(User reporter, ReviewComment reviewComment);

    List<Report> findByReviewComment(ReviewComment reviewComment);

    long countByReviewComment(ReviewComment reviewComment);

    long countByReviewCommentAndStatus(ReviewComment reviewComment, ReportStatus status);

    // Per la card operativa "X segnalazioni oggi"
    long countByCreatedAtAfter(java.time.LocalDateTime since);
}
