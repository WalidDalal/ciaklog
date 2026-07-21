package com.project.ciaklog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Stesso principio di StructureReviewRequest
// ma per una risposta — testo più breve, tono da commento non da recensione,
// con il contesto di COSA si sta rispondendo (non solo il titolo del film)
@Getter
@Setter
public class StructureCommentRequest {

    @NotBlank(message = "rawNotes obbligatorio")
    @Size(max = 500, message = "Appunti troppo lunghi (max 500 caratteri)")
    private String rawNotes;

    // Estratto della recensione a cui si sta rispondendo — opzionale, dà
    // contesto al prompt così il tono resta coerente col thread
    private String replyingToText;
}
