package com.project.ciaklog.dto.request;

import com.project.ciaklog.entity.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Fix (AI più centrale): "Chiedi su questo film" — sessione SEPARATA dalla
// chat generale, stateless (nessun sessionId/history), legata a un titolo
// specifico. Niente memoria: ogni domanda è indipendente.
@Getter
@Setter
public class MovieQuestionRequest {

    @NotNull(message = "tmdbId obbligatorio")
    private Long tmdbId;

    @NotNull(message = "contentType obbligatorio")
    private ContentType contentType;

    @NotBlank(message = "question obbligatoria")
    @Size(max = 300, message = "Domanda troppo lunga (max 300 caratteri)")
    private String question;
}
