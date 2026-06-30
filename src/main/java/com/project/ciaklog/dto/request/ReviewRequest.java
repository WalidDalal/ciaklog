package com.project.ciaklog.dto.request;

import com.project.ciaklog.entity.ContentType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {

    @NotNull(message = "tmdbId obbligatorio")
    private Long tmdbId;

    @NotNull(message = "contentType obbligatorio")
    private ContentType contentType;

    @NotNull(message = "Il voto è obbligatorio")
    @Min(value = 1, message = "Il voto minimo è 1")
    @Max(value = 5, message = "Il voto massimo è 5")
    private Integer rating;

    // Testo opzionale nel DTO — la validazione "obbligatorio se WATCHED"
    // è delegata a ReviewServiceImpl dove si conosce lo stato della WatchEntry
    @Size(max = 1000, message = "Testo massimo 1000 caratteri")
    private String text;
}