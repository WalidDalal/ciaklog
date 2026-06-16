package com.project.ciaklog.repository;

import com.project.ciaklog.entity.MediaType;
import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.ReviewStatus;
import com.project.ciaklog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Recensioni pubbliche (VISIBLE) per un film/serie — paginata (UC3, pagina dettaglio)
    Page<Review> findByTmdbIdAndMediaTypeAndStatus(Long tmdbId, MediaType mediaType, ReviewStatus status, Pageable pageable);

    // Tutte le recensioni di un utente — paginata (profilo pubblico)
    Page<Review> findByUser(User user, Pageable pageable);

    // Per verificare duplicati (UC6/FA2 — 409 se già recensito)
    boolean existsByUserAndTmdbIdAndMediaType(User user, Long tmdbId, MediaType mediaType);

    // Per recuperare la recensione esistente (modifica, UC7)
    Optional<Review> findByUserAndTmdbIdAndMediaType(User user, Long tmdbId, MediaType mediaType);

    // Per la weighted average (classifiche) — query custom
    @Query("SELECT r FROM Review r WHERE r.tmdbId = :tmdbId AND r.mediaType = :mediaType AND r.status = 'VISIBLE'")
    java.util.List<Review> findVisibleByTmdbIdAndMediaType(@Param("tmdbId") Long tmdbId, @Param("mediaType") MediaType mediaType);
    long countByUser(User user);
}