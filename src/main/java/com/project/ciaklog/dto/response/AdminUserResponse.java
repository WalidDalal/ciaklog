package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.Role;
import com.project.ciaklog.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

// DTO snello per la tabella Gestione Utenti (Admin) — niente bio/generi/sta-guardando,
// che richiederebbero query aggiuntive per ogni riga.
@Getter
@Builder
public class AdminUserResponse {
    private UUID id;
    private String username;
    private UserStatus status;
    private int violationCount;
    // Serve al frontend per nascondere "Sospendi" sulle
    // righe di altri Admin — un Admin non può sospendere né sé stesso né altri Admin
    private Role role;
}
