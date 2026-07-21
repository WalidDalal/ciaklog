package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.SuspendRequest;
import com.project.ciaklog.dto.response.AdminOperationalStatsResponse;
import com.project.ciaklog.dto.response.AdminUserDetailResponse;
import com.project.ciaklog.dto.response.AdminUserResponse;
import com.project.ciaklog.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // Card operativa leggera per la Home, non una
    // dashboard ricopiata — segnalazioni di oggi + utenti da controllare
    @GetMapping("/operational-stats")
    public ResponseEntity<AdminOperationalStatsResponse> getOperationalStats() {
        return ResponseEntity.ok(adminService.getOperationalStats());
    }

    // Prima l'ordinamento era
    // sempre "username ascendente", fisso, senza nessun parametro accettato —
    // niente per cui il frontend potesse chiedere un ordinamento diverso.
    // Whitelist esplicita sui campi ordinabili (non passare sortBy diretto a
    // Sort.by(), altrimenti un nome di campo arbitrario/malevolo lo accetterebbe)
    private static final java.util.Map<String, String> SORTABLE_FIELDS = java.util.Map.of(
            "username", "username",
            "status", "status",
            "violationCount", "violationCount"
    );

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        String field = SORTABLE_FIELDS.getOrDefault(sortBy, "username");
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, field));
        return ResponseEntity.ok(adminService.listUsers(pageable, search));
    }

    // Dettaglio utente per il drawer laterale — caricato on-demand
    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getUserDetail(id));
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<Void> suspendUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody SuspendRequest dto) {
        adminService.suspendUser(id, userDetails.getUsername(), dto.getReason());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/reinstate")
    public ResponseEntity<Void> reinstateUser(@PathVariable UUID id) {
        adminService.reinstateUser(id);
        return ResponseEntity.noContent().build();
    }
}