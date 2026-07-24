package com.project.ciaklog.controller;

import com.project.ciaklog.dto.response.WrappedResponse;
import com.project.ciaklog.service.WrappedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Solo utenti normali, stessa restrizione
// delle altre funzioni personali (l'Admin non ha libreria/recensioni, quindi
// non avrebbe comunque nulla da mostrare — coerente con le altre restrizioni)
@RestController
@RequestMapping("/api/wrapped")
@RequiredArgsConstructor
public class WrappedController {

    private final WrappedService wrappedService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WrappedResponse> getWrapped(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(wrappedService.getWrapped(userDetails.getUsername()));
    }
}
