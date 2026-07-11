package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChartUserResponse {
    private String username;

    // Punteggio persistito su User.score — aggiornato ad ogni azione rilevante (recensione, violazione, sospensione)
    private Integer score;

    private Integer reviewCount;

    // Posizione in classifica — utenti con lo stesso score hanno lo stesso rank (1°, 2°, 2°, 4°)
    private Integer rank;
}
