package com.project.ciaklog.controller;

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

    // Pubblico — profilo pubblico di qualsiasi utente
    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getPublicProfile(@PathVariable String username) {
        return ResponseEntity.ok(userService.getPublicProfile(username));
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
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteAccount(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}