package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.request.WatchEntryRequest;
import com.project.ciaklog.dto.response.WatchEntryResponse;
import com.project.ciaklog.entity.User;
import com.project.ciaklog.entity.WatchEntry;
import com.project.ciaklog.entity.WatchStatus;
import com.project.ciaklog.exception.*;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.repository.WatchEntryRepository;
import com.project.ciaklog.service.WatchEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchEntryServiceImpl implements WatchEntryService {

    private static final int MAX_WATCHING = 3;

    private final WatchEntryRepository watchEntryRepository;
    private final UserRepository userRepository;

    @Override
    public WatchEntryResponse addToLibrary(String username, WatchEntryRequest dto) {
        User user = getUser(username);

        if (watchEntryRepository.existsByUserAndTmdbIdAndContentType(user, dto.getTmdbId(), dto.getContentType())) {
            throw new DuplicateResourceException("Contenuto già presente in libreria");
        }

        if (dto.getStatus() == WatchStatus.WATCHING) {
            validateWatchingLimit(user);
        }

        WatchEntry entry = WatchEntry.builder()
                .user(user)
                .tmdbId(dto.getTmdbId())
                .contentType(dto.getContentType())
                .title(dto.getTitle())
                .posterPath(dto.getPosterPath())
                .releaseYear(dto.getReleaseYear())
                .genres(dto.getGenres())
                .status(dto.getStatus())
                .currentSeason(dto.getCurrentSeason())
                .watchedDate(dto.getStatus() == WatchStatus.WATCHED ? LocalDate.now() : null)
                .lastStatusUpdate(LocalDateTime.now())
                .build();

        return toDTO(watchEntryRepository.save(entry));
    }

    @Override
    public WatchEntryResponse updateStatus(String username, UUID entryId, WatchStatus newStatus) {
        User user = getUser(username);
        WatchEntry entry = watchEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenuto non trovato in libreria"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Non autorizzato");        }

        if (newStatus == WatchStatus.WATCHING && entry.getStatus() != WatchStatus.WATCHING) {
            validateWatchingLimit(user);
        }

        // Imposta watchedDate solo nel momento in cui si transita VERSO WATCHED
        // (non se era già WATCHED, per non sovrascrivere la data originale)
        if (newStatus == WatchStatus.WATCHED && entry.getStatus() != WatchStatus.WATCHED) {
            entry.setWatchedDate(LocalDate.now());
        }

        entry.setStatus(newStatus);
        entry.setLastStatusUpdate(LocalDateTime.now());

        return toDTO(watchEntryRepository.save(entry));
    }

    @Override
    public void removeFromLibrary(String username, UUID entryId) {
        User user = getUser(username);
        WatchEntry entry = watchEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenuto non trovato in libreria"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Non autorizzato");        }

        watchEntryRepository.delete(entry);
    }

    @Override
    public Page<WatchEntryResponse> getUserLibrary(String username, WatchStatus status, Pageable pageable) {
        User user = getUser(username);
        if (status != null) {
            return watchEntryRepository.findByUserAndStatus(user, status, pageable).map(this::toDTO);
        }
        return watchEntryRepository.findByUser(user, pageable).map(this::toDTO);
    }

    // ── helpers ──

    private void validateWatchingLimit(User user) {
        long count = watchEntryRepository.countByUserAndStatus(user, WatchStatus.WATCHING);
        if (count >= MAX_WATCHING) {
            throw new LimitExceededException(
                    "Hai già 3 contenuti in visione — completane uno o rimettilo in TO_WATCH prima di aggiungerne altri");
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    private WatchEntryResponse toDTO(WatchEntry e) {
        return WatchEntryResponse.builder()
                .id(e.getId())
                .tmdbId(e.getTmdbId())
                .contentType(e.getContentType())
                .title(e.getTitle())
                .posterPath(e.getPosterPath())
                .releaseYear(e.getReleaseYear())
                .status(e.getStatus())
                .currentSeason(e.getCurrentSeason())
                .watchedDate(e.getWatchedDate())
                .lastStatusUpdate(e.getLastStatusUpdate())
                .build();
    }
}
