package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserProfileResponse {
    private String username;
    private String bio;
    private List<String> topGenres;

    // Lista contenuti WATCHING aggiornati negli ultimi 30gg (max 5)
    // Vuota se l'utente non sta guardando nulla o tutti gli aggiornamenti sono vecchi
    private List<WatchingItem> watching;

    @Getter
    @Builder
    public static class WatchingItem {
        private String title;
        private Integer season; // null per i film
    }
}
