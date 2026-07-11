package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.ReportActionRequest;
import com.project.ciaklog.dto.request.ReportRequest;
import com.project.ciaklog.dto.response.ReportResponse;
import com.project.ciaklog.entity.ReportStatus;
import com.project.ciaklog.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // User/Admin — segnala una recensione o una risposta (esattamente uno tra
    // reviewId/reviewCommentId nel body, validato nel service)
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReportRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.createReport(userDetails.getUsername(), dto));
    }

    // Solo Admin — lista segnalazioni paginata, filtrabile per status
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reportService.getReports(status, pageable));
    }

    // Solo Admin — risolve una segnalazione (APPROVED/REJECTED)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> resolveReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody ReportActionRequest dto) {
        return ResponseEntity.ok(reportService.resolveReport(id, dto.getAction(), userDetails.getUsername()));
    }
}