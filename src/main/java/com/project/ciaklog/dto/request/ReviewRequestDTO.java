// ReviewRequestDTO.java
package com.project.ciaklog.dto.request;

import com.project.ciaklog.entity.MediaType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequestDTO {

    @NotNull(message = "tmdbId obbligatorio")
    private Long tmdbId;

    @NotNull(message = "mediaType obbligatorio")
    private MediaType mediaType;

    @NotNull(message = "rating obbligatorio")
    @Min(value = 1, message = "Rating minimo 1")
    @Max(value = 5, message = "Rating massimo 5")
    private Integer rating;

    @Size(max = 1000, message = "Testo massimo 1000 caratteri")
    private String text; // opzionale
}