package com.project.ciaklog.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewUpdateRequest {

    @NotNull(message = "Il voto è obbligatorio")
    @Min(value = 1, message = "Il voto minimo è 1")
    @Max(value = 5, message = "Il voto massimo è 5")
    private Integer rating;

    @NotBlank(message = "Testo recensione obbligatorio")
    @Size(max = 1000, message = "Testo massimo 1000 caratteri")
    private String text;
}