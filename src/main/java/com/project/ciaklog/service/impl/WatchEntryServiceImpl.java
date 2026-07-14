package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.request.WatchEntryRequest;
import com.project.ciaklog.dto.response.TmdbDetailResponse;
import com.project.ciaklog.dto.response.WatchEntryResponse;
import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.entity.Role;
import com.project.ciaklog.entity.User;
import com.project.ciaklog.entity.WatchEntry;
import com.project.ciaklog.entity.WatchStatus;
import com.project.ciaklog.exception.*;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.repository.WatchEntryRepository;
import com.project.ciaklog.entity.ReviewStatus;
import com.project.ciaklog.service.TmdbService;
import com.project.ciaklog.service.WatchEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchEntryServiceImpl implements WatchEntryService {

    private static final int MAX_WATCHING = 3;

    private final WatchEntryRepository watchEntryRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final TmdbService tmdbService;

    @Override
    @Transactional
    public WatchEntryResponse addToLibrary(String username, WatchEntryRequest dto) {
        User user = getUser(username);

        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Gli amministratori non possono aggiungere contenuti alla libreria");
        }

        if (watchEntryRepository.existsByUserAndTmdbIdAndContentType(user, dto.getTmdbId(), dto.getContentType())) {
            throw new DuplicateResourceException("Contenuto già presente in libreria");
        }

        if (dto.getStatus() == WatchStatus.WATCHING) {
            validateWatchingLimit(user);
        }

        // Dati recuperati server-side da TMDB — il client invia solo tmdbId + contentType
        TmdbDetailResponse detail = tmdbService.getDetail(dto.getTmdbId(), dto.getContentType());

        WatchEntry entry = WatchEntry.builder()
                .user(user)
                .tmdbId(dto.getTmdbId())
                .contentType(dto.getContentType())
                .title(detail.getTitle())
                .posterPath(detail.getPosterPath())
                .releaseYear(detail.getReleaseYear())
                .genres(detail.getGenres() != null ? String.join(",", detail.getGenres()) : null)
                .status(dto.getStatus())
                .currentSeason(dto.getCurrentSeason())
                .watchedDate(dto.getStatus() == WatchStatus.WATCHED ? LocalDate.now() : null)
                .lastStatusUpdate(LocalDateTime.now())
                .build();

        return toDTO(watchEntryRepository.save(entry));
    }

    @Override
    @Transactional
    public WatchEntryResponse updateStatus(String username, UUID entryId, WatchStatus newStatus, Integer currentSeason) {
        User user = getUser(username);

        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Gli amministratori non possono modificare la libreria");
        }

        WatchEntry entry = watchEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenuto non trovato in libreria"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Non autorizzato");
        }

        if (newStatus == WatchStatus.WATCHING && entry.getStatus() != WatchStatus.WATCHING) {
            validateWatchingLimit(user);
        }

        // Fix (bug coerenza stato/recensione): prima si poteva tornare a
        // TO_WATCH/WATCHING dopo aver recensito, lasciando una recensione
        // "orfana" collegata a un contenuto non più segnato come VISTO.
        // Blocca il cambio invece di lasciare lo stato incoerente — se
        // l'utente vuole davvero rivederlo, deve prima eliminare la recensione.
        if (entry.getStatus() == WatchStatus.WATCHED && newStatus != WatchStatus.WATCHED) {
            boolean hasActiveReview = reviewRepository.existsByUserAndTmdbIdAndContentTypeAndStatusNot(
                    user, entry.getTmdbId(), entry.getContentType(), ReviewStatus.REMOVED);
            if (hasActiveReview) {
                throw new BusinessRuleException(
                        "Hai già recensito questo contenuto — elimina la recensione prima di cambiare stato");
            }
        }

        if (newStatus == WatchStatus.WATCHED && entry.getStatus() != WatchStatus.WATCHED) {
            entry.setWatchedDate(LocalDate.now());
        }

        // Fix: prima non era possibile aggiornare la stagione corrente di un
        // titolo già "In Visione" — bisognava rimuoverlo e riaggiungerlo da capo.
        if (currentSeason != null) {
            validateSeason(entry, currentSeason);
            entry.setCurrentSeason(currentSeason);
        }

        entry.setStatus(newStatus);
        entry.setLastStatusUpdate(LocalDateTime.now());

        return toDTO(watchEntryRepository.save(entry));
    }

    @Override
    @Transactional
    public WatchEntryResponse updateSeason(String username, UUID entryId, Integer currentSeason) {
        User user = getUser(username);

        WatchEntry entry = watchEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenuto non trovato in libreria"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Non autorizzato");
        }

        validateSeason(entry, currentSeason);
        entry.setCurrentSeason(currentSeason);
        entry.setLastStatusUpdate(LocalDateTime.now());

        return toDTO(watchEntryRepository.save(entry));
    }

    // Fix: la stagione corrente non aveva NESSUN limite — si poteva impostare
    // 0, un numero negativo, o 999 anche se la serie ne ha solo 3. Verifica
    // sia il minimo (>= 1) sia un tetto di sicurezza fisso (1-50, nel caso TMDB
    // non risponda), oltre al massimo reale da TMDB quando disponibile
    private static final int SEASON_HARD_CAP = 50;

    private void validateSeason(WatchEntry entry, Integer season) {
        if (season < 1 || season > SEASON_HARD_CAP) {
            throw new BusinessRuleException("Il numero di stagione deve essere tra 1 e " + SEASON_HARD_CAP);
        }
        if (entry.getContentType() == ContentType.TV) {
            try {
                TmdbDetailResponse detail = tmdbService.getDetail(entry.getTmdbId(), ContentType.TV);
                if (detail.getNumberOfSeasons() != null && season > detail.getNumberOfSeasons()) {
                    throw new BusinessRuleException(
                            "Questa serie ha solo " + detail.getNumberOfSeasons() + " stagioni");
                }
            } catch (BusinessRuleException e) {
                throw e; // rilancia il vero errore di validazione
            } catch (Exception e) {
                // TMDB irraggiungibile o contenuto non più trovato: non blocchiamo
                // l'utente per un problema esterno — resta comunque valido il
                // limite di sicurezza 1-50 verificato sopra
            }
        }
    }

    @Override
    @Transactional
    public void removeFromLibrary(String username, UUID entryId) {
        User user = getUser(username);
        WatchEntry entry = watchEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenuto non trovato in libreria"));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Non autorizzato");
        }

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