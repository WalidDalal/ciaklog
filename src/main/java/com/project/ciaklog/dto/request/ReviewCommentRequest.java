package com.project.ciaklog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCommentRequest {

    @NotBlank(message = "Il testo della risposta è obbligatorio")
    @Size(max = 500, message = "Testo massimo 500 caratteri")
    private String text;
}
