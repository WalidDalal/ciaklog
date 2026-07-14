package com.project.ciaklog.controller;

import com.project.ciaklog.dto.request.AiChatRequest;
import com.project.ciaklog.dto.request.MovieQuestionRequest;
import com.project.ciaklog.dto.request.StructureCommentRequest;
import com.project.ciaklog.dto.request.StructureReviewRequest;
import com.project.ciaklog.dto.response.AiChatResponse;
import com.project.ciaklog.dto.response.DailyRecommendationResponse;
import com.project.ciaklog.dto.response.MovieQuestionResponse;
import com.project.ciaklog.dto.response.ReviewOpinionResponse;
import com.project.ciaklog.dto.response.StructureReviewResponse;
import com.project.ciaklog.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    // Fix (AI più centrale): "recensione a botta calda" — solo utenti normali,
    // stessa restrizione delle altre funzioni AI orientate all'utente
    @PostMapping("/structure-review")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<StructureReviewResponse> structureReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody StructureReviewRequest dto) {
        return ResponseEntity.ok(aiService.structureReview(userDetails.getUsername(), dto));
    }

    // Fix (AI dentro le risposte, deciso): stesso principio applicato alle
    // risposte — solo utenti normali, stessa restrizione delle altre funzioni
    // AI orientate all'utente
    @PostMapping("/structure-comment")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<StructureReviewResponse> structureComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody StructureCommentRequest dto) {
        return ResponseEntity.ok(aiService.structureComment(userDetails.getUsername(), dto));
    }

    // Fix (AI più centrale): "Chiedi su questo film" — sessione separata dalla
    // chat generale, stateless, solo utenti normali (non l'Admin, coerente
    // con tutte le altre restrizioni: niente libreria/recensioni/chat per lui)
    @PostMapping("/movie-question")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MovieQuestionResponse> answerMovieQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MovieQuestionRequest dto) {
        return ResponseEntity.ok(aiService.answerMovieQuestion(userDetails.getUsername(), dto));
    }

    // Fix (AI che replica a una recensione negativa): SOLO su richiesta
    // esplicita — l'utente clicca un bottone dedicato sulla PROPRIA recensione
    // con voto basso, non scatta mai da sola. Stessa restrizione USER.
    @PostMapping("/reviews/{reviewId}/opinion")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewOpinionResponse> getAiOpinionOnReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID reviewId) {
        return ResponseEntity.ok(aiService.getAiOpinionOnReview(userDetails.getUsername(), reviewId));
    }
}