package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserProfileResponse {
    private String username;
    private String bio;
    private List<String> topGenres; // top 3 generi calcolati

    // watchingTitle/watchingSeason: null sia se l'utente non sta guardando nulla,
    // sia se l'ultimo aggiornamento risale application.properties più di 30gg fa — il filtro è applicato
    // nel UserService prima di costruire questa response (Fix DTO #5, opzione A).
    // Nessun timestamp esposto pubblicamente.
    private String watchingTitle;
    private Integer watchingSeason;
}
