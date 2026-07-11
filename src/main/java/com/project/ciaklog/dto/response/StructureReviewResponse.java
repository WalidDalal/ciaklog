package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StructureReviewResponse {
    private String text; // testo strutturato, l'utente lo rivede/modifica prima di pubblicare
}
