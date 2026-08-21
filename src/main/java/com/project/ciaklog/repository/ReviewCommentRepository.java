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

    // JOIN FETCH author — evita LazyInitializationException in toDTO().
    // I commenti hiddenByAuthor restano visibili al proprio autore (per
    // mostrare "solo tu la vedi") e all'Admin, ma non ad altri viewer.
    // Lo status HIDDEN (2+ segnalazioni pendenti) è incluso solo per
    // l'Admin, che deve poterli vedere per decidere.
    @Query("""
            SELECT c FROM ReviewComment c
            JOIN FETCH c.author
            WHERE c.review = :review
              AND (c.status = :status OR (:isAdmin = true AND c.status = com.project.ciaklog.entity.ReviewStatus.HIDDEN))
              AND (c.hiddenByAuthor = false OR c.author.username = :viewerUsername OR :isAdmin = true)
              AND (c.hiddenBySuspension = false OR :isAdmin = true)
              AND (c.hiddenByDeletion = false OR :isAdmin = true)
            """)
    Page<ReviewComment> findByReviewAndStatus(
            @Param("review") Review review,
            @Param("status") ReviewStatus status,
            @Param("viewerUsername") String viewerUsername,
            @Param("isAdmin") boolean isAdmin,
            Pageable pageable);

    // Versione non paginata, usata da ReportServiceImpl
    // per nascondere/rimuovere tutte le risposte quando la recensione madre sparisce
    List<ReviewComment> findAllByReviewAndStatus(Review review, ReviewStatus status);

    // Usata da AdminServiceImpl per nascondere/ripristinare in blocco le
    // risposte di un utente quando viene sospeso/riabilitato
    List<ReviewComment> findAllByAuthor(com.project.ciaklog.entity.User author);

    // Quante risposte l'utente ha scritto sotto le recensioni di altri —
    // REMOVED escluse per coerenza con le altre statistiche
    long countByAuthorAndStatusNot(com.project.ciaklog.entity.User author, ReviewStatus status);
}
