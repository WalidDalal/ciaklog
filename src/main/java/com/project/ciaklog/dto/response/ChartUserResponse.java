package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChartUserResponse {
    private String username;

    // Punteggio classifica: score = count(review) + bonus * count(review con text non vuoto)
    // — vedi Regola di Business 5. Calcolato nel ChartService.
    private Integer score;

    private Integer reviewCount;
}