package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.entity.ReviewStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReviewResponse {
    private UUID id;
    private String username;
    private Long tmdbId;
    private ContentType contentType;
    private Integer rating;
    private String text;
    private ReviewStatus status;
    // Visibile solo per sapere se è nascosta —
    // l'endpoint pubblico (getReviewsForMedia) già esclude questi elementi a
    // monte, quindi in pratica arriva valorizzato solo dalle chiamate own-profile
    private boolean hiddenByAuthor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Arricchiti dalla WatchEntry dell'utente — usati nel profilo per mostrare titolo e poster
    // invece di "#tmdbId". Possono essere null se l'utente ha rimosso il contenuto dalla libreria.
    private String title;
    private String posterPath;
}