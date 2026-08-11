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
import com.project.ciaklog.repository.ReviewCommentRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.repository.WatchEntryRepository;
import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.ReviewComment;
import com.project.ciaklog.entity.ReviewStatus;
import com.project.ciaklog.entity.UserStatus;
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
    private final ReviewCommentRepository reviewCommentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Stessa palette di AVATAR_COLORS in ProfilePage.jsx — l'utente sceglie
    // da un set fisso, non un colore arbitrario, per coerenza col design e
    // per evitare che chiunque possa mandare un valore CSS non valido/malevolo.
    private static final List<String> ALLOWED_PROFILE_COLORS = List.of(
            "var(--accent)", "#3b82f6", "#22c55e", "#f59e0b", "#8b5cf6", "#ec4899", "#14b8a6", "#eab308"
    );

    @Override
    public UserProfileResponse getPublicProfile(String username, boolean canViewHidden) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        // Fix (dashboard admin — profilo di utenti sospesi/eliminati ancora
        // visionabile a chiunque): un profilo di un utente non ACTIVE non è
        // "non trovato" per un admin (deve poterlo aprire per il contesto di
        // moderazione, es. dal link nella dashboard segnalazioni) né per il
        // diretto interessato (se ha ancora un token valido durante una
        // sospensione temporanea) — ma per chiunque altro sì, stesso
        // trattamento riservato a un utente inesistente, per non rivelare
        // nemmeno che l'account esiste/è sospeso.
        if (user.getStatus() != UserStatus.ACTIVE && !canViewHidden) {
            throw new ResourceNotFoundException("Utente non trovato");
        }

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

        // Riusa gli stessi dati
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
                .profileColor(user.getProfileColor())
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
        if (dto.getUsername() == null && dto.getNewPassword() == null && dto.getBio() == null
                && dto.getProfileColor() == null) {
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

        if (dto.getProfileColor() != null) {
            if (dto.getProfileColor().isBlank()) {
                user.setProfileColor(null); // "" = torna al colore automatico
            } else if (!ALLOWED_PROFILE_COLORS.contains(dto.getProfileColor())) {
                throw new BusinessRuleException("Colore profilo non valido");
            } else {
                user.setProfileColor(dto.getProfileColor());
            }
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

        // Richiedere la password come secondo fattore prima di
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
        // Fix (dashboard admin — recensioni di utenti eliminati visibili a
        // chiunque): lo status restava ACTIVE dopo l'eliminazione, e le
        // recensioni/risposte non venivano toccate — restavano VISIBLE e
        // interamente pubbliche, con solo l'username anonimizzato a fare da
        // indizio. Stesso meccanismo già usato per la sospensione
        // (hiddenBySuspension): qui è irreversibile, quindi non serve nessun
        // "reinstate" — hiddenByDeletion resta true per sempre.
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);

        List<Review> ownReviews = reviewRepository.findAllByUser(user);
        for (Review r : ownReviews) {
            if (r.getStatus() == ReviewStatus.VISIBLE) {
                r.setHiddenByDeletion(true);
            }
        }
        reviewRepository.saveAll(ownReviews);

        List<ReviewComment> ownComments = reviewCommentRepository.findAllByAuthor(user);
        for (ReviewComment c : ownComments) {
            if (c.getStatus() == ReviewStatus.VISIBLE) {
                c.setHiddenByDeletion(true);
            }
        }
        reviewCommentRepository.saveAll(ownComments);
    }
}