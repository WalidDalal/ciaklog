package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.request.UpdateCredentialsRequest;
import com.project.ciaklog.dto.response.UserProfileResponse;
import com.project.ciaklog.entity.User;
import com.project.ciaklog.entity.WatchEntry;
import com.project.ciaklog.entity.WatchStatus;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.exception.DuplicateResourceException;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.repository.WatchEntryRepository;
import com.project.ciaklog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WatchEntryRepository watchEntryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileResponse getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        // Top 3 generi — calcolati dai contenuti in libreria per frequenza
        List<WatchEntry> allEntries = watchEntryRepository.findByUser(user, org.springframework.data.domain.Pageable.unpaged()).getContent();

        List<String> topGenres = allEntries.stream()
                .filter(e -> e.getGenres() != null && !e.getGenres().isBlank())
                .flatMap(e -> Arrays.stream(e.getGenres().split(",")))
                .map(String::trim)
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // "Sta guardando" — solo se lastStatusUpdate negli ultimi 30gg.
        // Filtro applicato qui, lato backend: se sono passati più di 30gg,
        // watchingTitle/watchingSeason restano null e nessun timestamp è esposto
        // nella response (Fix DTO #5, opzione A).
        List<WatchEntry> watching = watchEntryRepository.findAllByUserAndStatus(user, WatchStatus.WATCHING);
        WatchEntry activeWatching = watching.stream()
                .filter(e -> e.getLastStatusUpdate() != null &&
                        e.getLastStatusUpdate().isAfter(LocalDateTime.now().minusDays(30)))
                .findFirst()
                .orElse(null);

        return UserProfileResponse.builder()
                .username(user.getUsername())
                .bio(user.getBio())
                .topGenres(topGenres.isEmpty() ? Collections.emptyList() : topGenres)
                .watchingTitle(activeWatching != null ? activeWatching.getTitle() : null)
                .watchingSeason(activeWatching != null ? activeWatching.getCurrentSeason() : null)
                .build();
    }

    @Override
    public void updateCredentials(String username, UpdateCredentialsRequest dto) {
        if (dto.getUsername() == null && dto.getNewPassword() == null) {
            throw new BusinessRuleException("Almeno un campo da aggiornare è obbligatorio");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new DuplicateResourceException("Username non disponibile");
            }
            user.setUsername(dto.getUsername());
        }

        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            if (dto.getCurrentPassword() == null ||
                    !passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
                throw new BusinessRuleException("Password attuale non corretta");
            }
            user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        }

        userRepository.save(user);
    }
}
