package com.project.ciaklog.controller;

import com.project.ciaklog.dto.response.TmdbDetailResponse;
import com.project.ciaklog.dto.response.TmdbSearchResponse;
import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.service.TmdbService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tmdb")
@RequiredArgsConstructor
@Validated
public class TmdbController {

    private final TmdbService tmdbService;

    // Pubblico — ricerca per titolo
    @GetMapping("/search")
    public ResponseEntity<TmdbSearchResponse> search(
            @RequestParam @NotBlank @Size(min = 2, max = 100) String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.search(q, type, page));
    }

    // Pubblico — dettaglio film o serie
    // ContentType nel path: Spring lancia 400 automaticamente su valore non valido
    // (es. /api/tmdb/FILM/123 -> 400, non più un generico 500)
    @GetMapping("/{contentType}/{tmdbId}")
    public ResponseEntity<TmdbDetailResponse> getDetail(
            @PathVariable ContentType contentType,
            @PathVariable Long tmdbId) {
        return ResponseEntity.ok(tmdbService.getDetail(tmdbId, contentType));
    }
}