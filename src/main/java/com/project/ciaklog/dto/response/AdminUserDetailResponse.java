package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// DTO dettagliato per il drawer laterale Admin — caricato on-demand su GET /admin/users/{id}
@Getter
@Builder
public class AdminUserDetailResponse {
    private UUID id;
    private String username;
    private String email;
    private UserStatus status;
    private int violationCount;
    private int reviewCount;
    private int reportCount; // segnalazioni ricevute sulle sue recensioni
    private LocalDateTime createdAt;
    private List<ViolationItem> violations;

    @Getter
    @Builder
    public static class ViolationItem {
        private String category;    // categoria della segnalazione (es. SPAM, INAPPROPRIATE_CONTENT)
        private String reasonText;  // motivo scritto dal segnalatore (può essere null)
        private String reviewText;  // testo della recensione rimossa
        private LocalDateTime date;
    }
}