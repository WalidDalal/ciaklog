package com.project.ciaklog.repository;

import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.entity.User;
import com.project.ciaklog.entity.WatchEntry;
import com.project.ciaklog.entity.WatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchEntryRepository extends JpaRepository<WatchEntry, UUID> {

    Page<WatchEntry> findByUser(User user, Pageable pageable);
    Page<WatchEntry> findByUserAndStatus(User user, WatchStatus status, Pageable pageable);

    long countByUserAndStatus(User user, WatchStatus status);

    boolean existsByUserAndTmdbIdAndContentType(User user, Long tmdbId, ContentType contentType);

    Optional<WatchEntry> findByUserAndTmdbIdAndContentType(User user, Long tmdbId, ContentType contentType);

    // Rinominato da findByUserAndStatus per evitare conflitto di firma con quello paginato
    List<WatchEntry> findAllByUserAndStatus(User user, WatchStatus status);

    // Tutte le entry di un utente — usato per evitare N+1 in getUserReviews
    List<WatchEntry> findAllByUser(User user);
}