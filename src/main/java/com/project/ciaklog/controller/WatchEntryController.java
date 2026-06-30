package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.WatchEntryRequest;
import com.project.ciaklog.dto.response.WatchEntryResponse;
import com.project.ciaklog.entity.WatchStatus;
import com.project.ciaklog.service.WatchEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class WatchEntryController {

    private final WatchEntryService watchEntryService;

    // GET /api/library?status=WATCHED (opzionale)
    @GetMapping
    public ResponseEntity<Page<WatchEntryResponse>> getLibrary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) WatchStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(watchEntryService.getUserLibrary(userDetails.getUsername(), status, pageable));
    }

    @PostMapping
    public ResponseEntity<WatchEntryResponse> addToLibrary(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WatchEntryRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(watchEntryService.addToLibrary(userDetails.getUsername(), dto));
    }

    // PUT /api/library/{id}?status=WATCHED
    @PutMapping("/{id}")
    public ResponseEntity<WatchEntryResponse> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @RequestParam WatchStatus status) {
        return ResponseEntity.ok(watchEntryService.updateStatus(userDetails.getUsername(), id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromLibrary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        watchEntryService.removeFromLibrary(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
