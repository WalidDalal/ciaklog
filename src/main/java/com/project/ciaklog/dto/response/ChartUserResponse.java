package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChartUserResponse {
    private String username;

    // Fix (🟡 avatar tutti dello stesso colore in classifica): mancava questo
    // campo — il frontend usava un indice di posizione nel podio al posto del
    // vero colore profilo, e con pochi pareggi quell'indice era quasi sempre
    // 0 per tutti, risultando nello stesso colore per (quasi) tutti gli utenti.
    private String profileColor;

    // Punteggio persistito su User.score — aggiornato ad ogni azione rilevante (recensione, violazione, sospensione)
    private Integer score;

    private Integer reviewCount;

    // Posizione in classifica — utenti con lo stesso score hanno lo stesso rank (1°, 2°, 2°, 4°)
    private Integer rank;
}
