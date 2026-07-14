package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MovieQuestionResponse {
    private String answer;
    // Fix (gestione spoiler): true se la risposta rivela finale/eventi chiave —
    // il frontend la mostra dietro un click di conferma invece che subito
    private boolean containsSpoiler;
}
