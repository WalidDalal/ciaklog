package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.DeleteAccountRequest;
import com.project.ciaklog.dto.request.UpdateCredentialsRequest;
import com.project.ciaklog.dto.response.AuthResponse;
import com.project.ciaklog.dto.response.UserProfileResponse;
import com.project.ciaklog.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Pubblico — profilo di qualsiasi utente. @AuthenticationPrincipal può
    // essere null (permitAll, il filtro JWT valorizza il principal solo
    // se arriva un token valido). Un profilo non ACTIVE (sospeso/eliminato)
    // è visibile solo ad admin o all'interessato stesso — vedi getPublicProfile
    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getPublicProfile(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails viewer) {
        boolean isAdmin = viewer != null && viewer.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isSelf = viewer != null && viewer.getUsername().equals(username);
        return ResponseEntity.ok(userService.getPublicProfile(username, isAdmin || isSelf));
    }

    // Protetto — modifica credenziali dell'utente autenticato
    // Se è cambiato l'username restituisce 200 + nuovo token; altrimenti 204
    @PutMapping("/me")
    public ResponseEntity<AuthResponse> updateCredentials(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateCredentialsRequest dto) {

        AuthResponse response = userService.updateCredentials(userDetails.getUsername(), dto);

        if (response != null) {
            // Username cambiato: il client deve aggiornare il token
            return ResponseEntity.ok(response);
        }

        // Solo password cambiata: nessun nuovo token necessario
        return ResponseEntity.noContent().build();
    }

    // Eliminazione account — anonimizza i dati dell'utente
    // Richiede la password come conferma finale, non solo il modal
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeleteAccountRequest dto) {
        userService.deleteAccount(userDetails.getUsername(), dto.getPassword());
        return ResponseEntity.noContent().build();
    }
}