package com.project.ciaklog.repository;

import com.project.ciaklog.entity.User;
import com.project.ciaklog.entity.WatchEntry;
import com.project.ciaklog.entity.MediaType;
import com.project.ciaklog.entity.WatchStatus;
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
public interface WatchEntryRepository extends JpaRepository<WatchEntry, UUID> {

    // Libreria utente paginata, filtrabile per stato
    Page<WatchEntry> findByUser(User user, Pageable pageable);
    Page<WatchEntry> findByUserAndStatus(User user, WatchStatus status, Pageable pageable);

    // Per la regola "max 3 WATCHING per utente" (Regola 1)
    long countByUserAndStatus(User user, WatchStatus status);

    // Per verificare duplicati (UC4/FA1 — 409 se già in libreria)
    boolean existsByUserAndTmdbIdAndMediaType(User user, Long tmdbId, MediaType mediaType);

    // Per recuperare una entry specifica (es. al cambio stato o eliminazione)
    Optional<WatchEntry> findByUserAndTmdbIdAndMediaType(User user, Long tmdbId, MediaType mediaType);

    // Per il profilo pubblico — contenuti "Sta guardando" (max 3, solo WATCHING)
    List<WatchEntry> findByUserAndStatus(User user, WatchStatus status);

    @Query("SELECT w FROM WatchEntry w WHERE w.user = :user AND w.status = 'WATCHING' AND w.lastStatusUpdate >= :cutoff")
    List<WatchEntry> findActiveWatching(@Param("user") User user, @Param("cutoff") LocalDateTime cutoff);
}