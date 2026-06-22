package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

// DTO snello per la tabella Gestione Utenti (Admin) — niente bio/generi/sta-guardando,
// che richiederebbero query aggiuntive per ogni riga (vedi Fix Controller #7).
@Getter
@Builder
public class AdminUserResponse {
    private UUID id;
    private String username;
    private UserStatus status;
    private int violationCount;
}
