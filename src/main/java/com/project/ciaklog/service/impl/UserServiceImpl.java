package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.request.UpdateCredentialsRequest;
import com.project.ciaklog.dto.response.AuthResponse;
import com.project.ciaklog.dto.response.UserProfileResponse;
import com.project.ciaklog.entity.User;
import com.project.ciaklog.entity.WatchEntry;
import com.project.ciaklog.entity.WatchStatus;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.exception.DuplicateResourceException;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.repository.WatchEntryRepository;
import com.project.ciaklog.entity.ReviewStatus;
import com.project.ciaklog.security.JwtService;
import com.project.ciaklog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WatchEntryRepository watchEntryRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public UserProfileResponse getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        List<WatchEntry> allEntries = watchEntryRepository.findAllByUser(user);

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

        List<WatchEntry> watching = watchEntryRepository.findAllByUserAndStatus(user, WatchStatus.WATCHING);
        List<UserProfileResponse.WatchingItem> watchingItems = watching.stream()
                .filter(e -> e.getLastStatusUpdate() != null &&
                        e.getLastStatusUpdate().isAfter(LocalDateTime.now().minusDays(30)))
                .limit(5)
                .map(e -> UserProfileResponse.WatchingItem.builder()
                        .title(e.getTitle())
                        .season(e.getCurrentSeason())
                        .build())
                .collect(Collectors.toList());

        // Fix (Impostazioni — card riepilogo account): riusa gli stessi dati
        // già calcolabili qui, invece di un endpoint dedicato
        long totalReviews = reviewRepository.findAllByUser(user).stream()
                .filter(r -> r.getStatus() != ReviewStatus.REMOVED)
                .count();
        String memberSince = user.getCreatedAt() != null
                ? capitalize(user.getCreatedAt().getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN))
                    + " " + user.getCreatedAt().getYear()
                : null;

        return UserProfileResponse.builder()
                .username(user.getUsername())
                .bio(user.getBio())
                .topGenres(topGenres.isEmpty() ? Collections.emptyList() : topGenres)
                .watching(watchingItems)
                .memberSince(memberSince)
                .score(user.getScore())
                .totalReviews(totalReviews)
                .build();
    }

    private String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    @Transactional
    public AuthResponse updateCredentials(String username, UpdateCredentialsRequest dto) {
        if (dto.getUsername() == null && dto.getNewPassword() == null && dto.getBio() == null) {
            throw new BusinessRuleException("Almeno un campo da aggiornare è obbligatorio");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        boolean usernameChanged = false;

        if (dto.getUsername() != null && !dto.getUsername().isBlank()
                && !dto.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new DuplicateResourceException("Username non disponibile");
            }
            user.setUsername(dto.getUsername());
            usernameChanged = true;
        }

        if (dto.getBio() != null) {
            user.setBio(dto.getBio().isBlank() ? null : dto.getBio().trim());
        }

        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            if (dto.getCurrentPassword() == null ||
                    !passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
                throw new BusinessRuleException("Password attuale non corretta");
            }
            user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        }

        userRepository.save(user);

        // Se l'username è cambiato il vecchio token è invalido (subject diverso) —
        // generiamo un nuovo token e lo restituiamo al client
        if (usernameChanged) {
            String newToken = jwtService.generateToken(user);
            return AuthResponse.builder()
                    .token(newToken)
                    .username(user.getUsername())
                    .role(user.getRole())
                    .build();
        }

        // Solo password cambiata: il token esistente rimane valido
        return null;
    }

    @Override
    @Transactional
    public void deleteAccount(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        // Fix: richiedere la password come secondo fattore prima di
        // un'azione irreversibile, non solo la conferma via modal
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessRuleException("Password non corretta");
        }

        // Soft delete: anonimizza i dati invece di eliminare fisicamente
        // per mantenere integrità referenziale con recensioni e segnalazioni
        user.setUsername("deleted_" + user.getId().toString().substring(0, 8));
        user.setEmail("deleted_" + user.getId() + "@deleted.com");
        user.setPasswordHash("[DELETED]");
        user.setBio(null);
        userRepository.save(user);
    }
}