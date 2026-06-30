package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.AiChatRequest;
import com.project.ciaklog.dto.response.AiChatResponse;
import com.project.ciaklog.dto.response.DailyRecommendationResponse;
import com.project.ciaklog.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // Chat film/serie — solo utenti normali
    @PostMapping("/chat")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AiChatResponse> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AiChatRequest dto) {
        return ResponseEntity.ok(aiService.chat(userDetails.getUsername(), dto));
    }

    // Chat gestionale — solo admin
    @PostMapping("/chat/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AiChatResponse> chatAdmin(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AiChatRequest dto) {
        return ResponseEntity.ok(aiService.chatAdmin(userDetails.getUsername(), dto));
    }

    // Raccomandazione giornaliera — solo utenti normali
    @GetMapping("/daily")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DailyRecommendationResponse> getDaily(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(aiService.getDaily(userDetails.getUsername()));
    }
}