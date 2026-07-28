package com.project.ciaklog.repository;

import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.ReviewStatus;
import com.project.ciaklog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // JOIN FETCH user — evita LazyInitializationException in toDTO()
    // Esclusa dalla lista pubblica se
    // hiddenByAuthor = true — MA l'autore stesso deve continuare a vederla
    // (altrimenti perderebbe il modo di ripristinarla: niente più bottone
    // "Mostra di nuovo", perché l'elemento non arriverebbe proprio più).
    // viewerUsername è null per i visitatori anonimi — non matcha mai
    // nessun autore, quindi i nascosti restano nascosti per loro.
    // Attenzione: questo filtro era già stato aggiunto una volta e si è perso in un
    // giro di modifiche successive — occhio a non perderlo di nuovo
    //
    // Un Admin deve
    // vedere anche le recensioni nascoste dall'autore E quelle con status
    // HIDDEN (2+ segnalazioni, in attesa di decisione), altrimenti "Vedi nel
    // contesto" dalla dashboard porterebbe a una pagina dove la recensione
    // segnalata semplicemente non compare
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.user
            WHERE r.tmdbId = :tmdbId AND r.contentType = :contentType
              AND (r.status = :status OR (:isAdmin = true AND r.status = com.project.ciaklog.entity.ReviewStatus.HIDDEN))
              AND (r.hiddenByAuthor = false OR r.user.username = :viewerUsername OR :isAdmin = true)
              AND (r.hiddenBySuspension = false OR :isAdmin = true)
            """)
    Page<Review> findByTmdbIdAndContentTypeAndStatus(
            @Param("tmdbId") Long tmdbId,
            @Param("contentType") ContentType contentType,
            @Param("status") ReviewStatus status,
            @Param("viewerUsername") String viewerUsername,
            @Param("isAdmin") boolean isAdmin,
            Pageable pageable);

    // JOIN FETCH user — evita LazyInitializationException in toDTO()
    // Questa query
    // non aveva NESSUN filtro su status o hiddenByAuthor, a differenza della
    // query gemella per la pagina film (findByTmdbIdAndContentTypeAndStatus),
    // che quel filtro ce l'ha da tempo (con un commento che avvisa "si è già
    // perso una volta, occhio a non perderlo di nuovo" — successo di nuovo,
    // ma su questa query invece). Risultato: recensioni RIMOSSE per violazione
    // e nascoste dall'autore erano visibili nel profilo pubblico a chiunque,
    // incluso un visitatore anonimo. Stesso filtro allineato qui, incluso
    // il caso admin (vede anche HIDDEN e nascoste dall'autore).
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.user
            WHERE r.user = :user
              AND (r.status = :status OR (:isAdmin = true AND r.status = com.project.ciaklog.entity.ReviewStatus.HIDDEN))
              AND (r.hiddenByAuthor = false OR r.user.username = :viewerUsername OR :isAdmin = true)
              AND (r.hiddenBySuspension = false OR :isAdmin = true)
            """)
    Page<Review> findByUser(
            @Param("user") User user,
            @Param("status") ReviewStatus status,
            @Param("viewerUsername") String viewerUsername,
            @Param("isAdmin") boolean isAdmin,
            Pageable pageable);

    // Tutte le review di un utente senza filtro status — usata dall'admin
    List<Review> findAllByUser(User user);

    boolean existsByUserAndTmdbIdAndContentType(User user, Long tmdbId, ContentType contentType);

    // Serve per bloccare l'uscita da VISTO se
    // esiste già una recensione attiva — una REMOVED non deve contare, altrimenti
    // chi ha eliminato la propria recensione resterebbe bloccato per sempre
    boolean existsByUserAndTmdbIdAndContentTypeAndStatusNot(
            User user, Long tmdbId, ContentType contentType, ReviewStatus status);

    Optional<Review> findByUserAndTmdbIdAndContentType(User user, Long tmdbId, ContentType contentType);

    // Fix (endpoint GET /reviews/media/{type}/{id}/mine — bug in produzione,
    // LazyInitializationException): a differenza delle query gemelle qui sopra
    // (findByTmdbIdAndContentTypeAndStatus), questa non faceva JOIN FETCH r.user
    // — toDTO() chiama r.getUser().getUsername(), quindi fuori transazione
    // (il metodo del service non è @Transactional) l'accesso al proxy lazy
    // falliva con 500. Query dedicata con fetch esplicito, usata solo da
    // getMyReviewForMedia per non toccare gli altri usi del metodo sopra
    // (createReview, ecc. — dove il proxy lazy non serve).
    @Query(
            "SELECT r FROM Review r JOIN FETCH r.user WHERE r.user = :user AND r.tmdbId = :tmdbId AND r.contentType = :contentType")
    Optional<Review> findByUserAndTmdbIdAndContentTypeFetchUser(
            @Param("user") User user, @Param("tmdbId") Long tmdbId, @Param("contentType") ContentType contentType);

    // Usata per calcoli statistici (media voti),
    // qui l'eccezione per il proprietario non serve — una recensione auto-nascosta
    // non deve influenzare la media mostrata a tutti, punto, indipendentemente da chi guarda
    @Query("SELECT r FROM Review r WHERE r.tmdbId = :tmdbId AND r.contentType = :contentType AND r.status = 'VISIBLE' AND r.hiddenByAuthor = false AND r.hiddenBySuspension = false")
    List<Review> findVisibleByTmdbIdAndContentType(@Param("tmdbId") Long tmdbId, @Param("contentType") ContentType contentType);

    @Query("""
            SELECT r.tmdbId, r.contentType, COUNT(r), AVG(r.rating), MAX(r.createdAt)
            FROM Review r
            WHERE r.status = com.project.ciaklog.entity.ReviewStatus.VISIBLE
              AND r.hiddenByAuthor = false
              AND r.hiddenBySuspension = false
              AND r.contentType = :contentType
            GROUP BY r.tmdbId, r.contentType
            HAVING COUNT(r) >= :minVotes
            """)
    List<Object[]> findAggregatedByContentType(
            @Param("contentType") ContentType contentType,
            @Param("minVotes") long minVotes);

    @Query("""
            SELECT r.tmdbId, r.contentType, COUNT(r)
            FROM Review r
            WHERE r.status = com.project.ciaklog.entity.ReviewStatus.VISIBLE
              AND r.hiddenByAuthor = false
              AND r.hiddenBySuspension = false
              AND (r.createdAt >= :since OR r.updatedAt >= :since)
            GROUP BY r.tmdbId, r.contentType
            ORDER BY COUNT(r) DESC
            """)
    // Fix (Homepage — "più discussi"): prima contava solo createdAt >= since,
    // quindi una recensione scritta mesi fa ma MODIFICATA negli ultimi 7 giorni
    // non contribuiva mai al trending, anche se è un'interazione recente vera
    // e propria. updatedAt viene aggiornato da @UpdateTimestamp su ogni save
    // (Review.java), quindi è affidabile: ora basta l'uno O l'altro.
    List<Object[]> findTrendingGrouped(@Param("since") LocalDateTime since);
}
