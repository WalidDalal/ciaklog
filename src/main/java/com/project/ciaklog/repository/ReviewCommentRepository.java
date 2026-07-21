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
    // Esclusa dalla lista pubblica se
    // hiddenByAuthor = true — MA l'autore stesso deve continuare a vederla
    // quando è lui a guardare il thread (altrimenti l'indicatore "solo tu la
    // vedi" non potrebbe mai comparire: l'elemento non arriverebbe proprio).
    // viewerUsername è null per i visitatori anonimi — non matcha mai
    // nessun autore, quindi i nascosti restano nascosti per loro.
    //
    // "nascosto" nel
    // contesto moderazione NON è hiddenByAuthor (quello è un toggle personale
    // dell'autore) — è status = HIDDEN, impostato automaticamente dopo 2+
    // segnalazioni in attesa di decisione admin. Il primo giro filtrava
    // comunque su c.status = :status (sempre VISIBLE, passato dal service),
    // quindi i commenti HIDDEN restavano esclusi anche per l'Admin, che è
    // esattamente il caso in cui deve poterli vedere per decidere. Ora, se
    // isAdmin, la clausola sullo status include anche HIDDEN.
    @Query("""
            SELECT c FROM ReviewComment c
            JOIN FETCH c.author
            WHERE c.review = :review
              AND (c.status = :status OR (:isAdmin = true AND c.status = com.project.ciaklog.entity.ReviewStatus.HIDDEN))
              AND (c.hiddenByAuthor = false OR c.author.username = :viewerUsername OR :isAdmin = true)
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

    // Quante risposte l'utente ha
    // scritto sotto le recensioni di altri — sostituisce la ridondanza tra
    // "Visti" e "Recensioni scritte" (sempre uguali una volta corretto il bug
    // di conteggio), REMOVED escluse per coerenza con le altre statistiche
    long countByAuthorAndStatusNot(com.project.ciaklog.entity.User author, ReviewStatus status);
}
