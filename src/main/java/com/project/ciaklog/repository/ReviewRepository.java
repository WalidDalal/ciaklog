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

    // Recensioni pubbliche (VISIBLE) per un film/serie — paginata (UC3, pagina dettaglio)
    Page<Review> findByTmdbIdAndContentTypeAndStatus(Long tmdbId, ContentType contentType, ReviewStatus status, Pageable pageable);

    // Tutte le recensioni di un utente — paginata (profilo pubblico)
    Page<Review> findByUser(User user, Pageable pageable);

    // Per verificare duplicati (UC6/FA2 — 409 se già recensito)
    boolean existsByUserAndTmdbIdAndContentType(User user, Long tmdbId, ContentType contentType);

    // Per recuperare la recensione esistente (modifica, UC7)
    Optional<Review> findByUserAndTmdbIdAndContentType(User user, Long tmdbId, ContentType contentType);

    // Per la weighted average in getTrending() — carica le review di un singolo tmdbId
    @Query("SELECT r FROM Review r WHERE r.tmdbId = :tmdbId AND r.contentType = :contentType AND r.status = 'VISIBLE'")
    List<Review> findVisibleByTmdbIdAndContentType(@Param("tmdbId") Long tmdbId, @Param("contentType") ContentType contentType);

    // Classifica film/serie: dati aggregati in una sola query, evita findAll()
    // Restituisce: [tmdbId (Long), contentType (String), count (Long), avgRating (Double), maxCreatedAt (LocalDateTime)]
    @Query("""
            SELECT r.tmdbId, r.contentType, COUNT(r), AVG(r.rating), MAX(r.createdAt)
            FROM Review r
            WHERE r.status = com.project.ciaklog.entity.ReviewStatus.VISIBLE
              AND r.contentType = :contentType
            GROUP BY r.tmdbId, r.contentType
            HAVING COUNT(r) >= :minVotes
            """)
    List<Object[]> findAggregatedByContentType(
            @Param("contentType") ContentType contentType,
            @Param("minVotes") long minVotes);

    // Trending: recensioni recenti aggregate — evita findAll() + N query annidate
    // Restituisce: [tmdbId (Long), contentType (String), weeklyCount (Long)]
    @Query("""
            SELECT r.tmdbId, r.contentType, COUNT(r)
            FROM Review r
            WHERE r.status = com.project.ciaklog.entity.ReviewStatus.VISIBLE
              AND r.createdAt >= :since
            GROUP BY r.tmdbId, r.contentType
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> findTrendingGrouped(@Param("since") LocalDateTime since);
}