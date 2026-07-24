package com.project.ciaklog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// L'utente scrive appunti sparsi su un film/serie appena
// visto, l'AI li struttura in una recensione ben scritta MANTENENDO il suo tono
// e le sue opinioni — non ne inventa di nuove, non la scrive al posto suo.
@Getter
@Setter
public class StructureReviewRequest {

    @NotBlank(message = "rawNotes obbligatorio")
    @Size(max = 1000, message = "Appunti troppo lunghi (max 1000 caratteri)")
    private String rawNotes;

    // Titolo del film/serie, solo per dare contesto al prompt — opzionale
    private String movieTitle;
}
