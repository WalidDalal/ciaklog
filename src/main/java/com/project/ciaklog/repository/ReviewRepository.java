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
    // Fix (auto-nascondimento autore): esclusa dalla lista pubblica se
    // hiddenByAuthor = true — MA l'autore stesso deve continuare a vederla
    // (altrimenti perderebbe il modo di ripristinarla: niente più bottone
    // "Mostra di nuovo", perché l'elemento non arriverebbe proprio più).
    // viewerUsername è null per i visitatori anonimi — non matcha mai
    // nessun autore, quindi i nascosti restano nascosti per loro.
    // ⚠️ Questo filtro era già stato aggiunto una volta e si è perso in un
    // giro di modifiche successive — occhio a non perderlo di nuovo
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.user
            WHERE r.tmdbId = :tmdbId AND r.contentType = :contentType AND r.status = :status
              AND (r.hiddenByAuthor = false OR r.user.username = :viewerUsername)
            """)
    Page<Review> findByTmdbIdAndContentTypeAndStatus(
            @Param("tmdbId") Long tmdbId,
            @Param("contentType") ContentType contentType,
            @Param("status") ReviewStatus status,
            @Param("viewerUsername") String viewerUsername,
            Pageable pageable);

    // JOIN FETCH user — evita LazyInitializationException in toDTO()
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.user
            WHERE r.user = :user
            """)
    Page<Review> findByUser(@Param("user") User user, Pageable pageable);

    // Tutte le review di un utente senza filtro status — usata dall'admin
    List<Review> findAllByUser(User user);

    boolean existsByUserAndTmdbIdAndContentType(User user, Long tmdbId, ContentType contentType);

    // Fix (coerenza stato/recensione): serve per bloccare l'uscita da VISTO se
    // esiste già una recensione attiva — una REMOVED non deve contare, altrimenti
    // chi ha eliminato la propria recensione resterebbe bloccato per sempre
    boolean existsByUserAndTmdbIdAndContentTypeAndStatusNot(
            User user, Long tmdbId, ContentType contentType, ReviewStatus status);

    Optional<Review> findByUserAndTmdbIdAndContentType(User user, Long tmdbId, ContentType contentType);

    // Fix (auto-nascondimento autore): usata per calcoli statistici (media voti),
    // qui l'eccezione per il proprietario non serve — una recensione auto-nascosta
    // non deve influenzare la media mostrata a tutti, punto, indipendentemente da chi guarda
    @Query("SELECT r FROM Review r WHERE r.tmdbId = :tmdbId AND r.contentType = :contentType AND r.status = 'VISIBLE' AND r.hiddenByAuthor = false")
    List<Review> findVisibleByTmdbIdAndContentType(@Param("tmdbId") Long tmdbId, @Param("contentType") ContentType contentType);

    @Query("""
            SELECT r.tmdbId, r.contentType, COUNT(r), AVG(r.rating), MAX(r.createdAt)
            FROM Review r
            WHERE r.status = com.project.ciaklog.entity.ReviewStatus.VISIBLE
              AND r.hiddenByAuthor = false
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
              AND r.createdAt >= :since
            GROUP BY r.tmdbId, r.contentType
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> findTrendingGrouped(@Param("since") LocalDateTime since);
}