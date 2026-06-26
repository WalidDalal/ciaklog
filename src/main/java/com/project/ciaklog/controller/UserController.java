package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.UpdateCredentialsRequest;
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
    @PutMapping("/me")
    public ResponseEntity<Void> updateCredentials(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateCredentialsRequest dto) {
        userService.updateCredentials(userDetails.getUsername(), dto);
        return ResponseEntity.noContent().build();
    }
}
