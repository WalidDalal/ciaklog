package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.ReportActionRequest;
import com.project.ciaklog.dto.request.ReportRequest;
import com.project.ciaklog.dto.response.ReportResponse;
import com.project.ciaklog.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // User/Admin — segnala una recensione
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReportRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.createReport(userDetails.getUsername(), dto));
    }

    // Solo Admin — lista segnalazioni, filtrabile per status (opzionale)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponse>> getReports(
            @RequestParam(required = false) com.project.ciaklog.entity.ReportStatus status) {
        return ResponseEntity.ok(reportService.getReports(status));
    }

    // Solo Admin — risolve una segnalazione (APPROVED/PAUSED/REJECTED) tramite body
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> resolveReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody ReportActionRequest dto) {
        return ResponseEntity.ok(reportService.resolveReport(id, dto.getAction(), userDetails.getUsername()));
    }
}