package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.AiChatRequest;
import com.project.ciaklog.dto.response.AiChatResponse;
import com.project.ciaklog.dto.response.DailyRecommendationResponse;
import com.project.ciaklog.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // Chat libera in linguaggio naturale
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AiChatRequest dto) {
        return ResponseEntity.ok(aiService.chat(userDetails.getUsername(), dto));
    }

    // Raccomandazione giornaliera — cachata 24h
    @GetMapping("/daily")
    public ResponseEntity<DailyRecommendationResponse> getDaily(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(aiService.getDaily(userDetails.getUsername()));
    }
}
