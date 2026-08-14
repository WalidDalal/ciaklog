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

    // Fix (dashboard admin — filtro per tipo bersaglio): il filtro
    // recensioni/risposte prima veniva applicato lato frontend sui soli
    // report già caricati in pagina, quindi funzionava solo sulla pagina
    // visualizzata in quel momento e non su tutte. Ora il filtro è nella
    // query, applicato PRIMA della paginazione — combinato con lo status.
    Page<Report> findByStatusAndReviewIsNotNull(ReportStatus status, Pageable pageable);
    Page<Report> findByStatusAndReviewCommentIsNotNull(ReportStatus status, Pageable pageable);
    Page<Report> findByReviewIsNotNull(Pageable pageable);
    Page<Report> findByReviewCommentIsNotNull(Pageable pageable);

    // Fix (dashboard admin — gruppi di segnalazioni spezzati tra le pagine):
    // le varianti Page qui sopra paginano per RIGA di segnalazione, non per
    // bersaglio (review/commento) — se una review ha 2 segnalazioni e altre 6
    // segnalazioni (su altri bersagli) la seguono, le 2 possono finire in una
    // pagina e altre segnalazioni sullo STESSO bersaglio in quella dopo,
    // mostrando conteggi incoerenti. Queste varianti List (non paginate) sono
    // usate per raggruppare per bersaglio in Java e paginare i GRUPPI, non le
    // righe — vedi ReportServiceImpl.getReports().
    // Fix (🔴 FIX — Segnalazioni Approvate: pagine incoerenti, es. "2 pagine
    // ma la prima ne mostra solo 5 e la seconda è vuota"): queste query non
    // avevano un ORDER BY esplicito. Senza di esso il DB NON garantisce lo
    // stesso ordine tra due chiamate identiche — e qui la lista viene
    // ricaricata e riordinata DA ZERO a ogni singola richiesta (pagina 1 e
    // pagina 2 sono due chiamate HTTP separate). Se l'ordine restituito dal
    // DB cambia anche solo leggermente tra le due (es. per timestamp con
    // valori identici tra più segnalazioni, molto comune con dati di test
    // creati in blocco), alcuni gruppi finiscono ordinati diversamente nelle
    // due chiamate: il totale (usato per calcolare il numero di pagine)
    // resta corretto, ma il CONTENUTO delle singole pagine no — alcuni
    // gruppi possono non comparire in nessuna pagina, altri comparire due
    // volte. Aggiunto un ORDER BY deterministico (data + id come tie-break
    // stabile) così lo stesso identico ordine viene restituito ad ogni
    // chiamata, indipendentemente da eventuali timestamp coincidenti.
    List<Report> findByStatusAndReviewIsNotNullOrderByCreatedAtDescIdAsc(ReportStatus status);
    List<Report> findByStatusAndReviewCommentIsNotNullOrderByCreatedAtDescIdAsc(ReportStatus status);
    List<Report> findByReviewIsNotNullOrderByCreatedAtDescIdAsc();
    List<Report> findByReviewCommentIsNotNullOrderByCreatedAtDescIdAsc();
    List<Report> findByStatusOrderByCreatedAtDescIdAsc(ReportStatus status);

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

    // Fix (dashboard admin — archiviazione automatica quando l'autore del
    // contenuto sparisce): serve trovare TUTTE le segnalazioni PENDING sui
    // contenuti (recensioni + risposte) di un utente, per archiviarle in
    // blocco quando il suo account diventa DELETED/PERMANENTLY_SUSPENDED.
    List<Report> findAllByReviewInAndStatus(List<Review> reviews, ReportStatus status);
    List<Report> findAllByReviewCommentInAndStatus(List<ReviewComment> comments, ReportStatus status);

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