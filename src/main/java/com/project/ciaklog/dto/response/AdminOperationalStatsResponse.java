package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

// Fix (Home Admin, deciso): card operativa leggera, non una dashboard
// ricopiata — solo i numeri che servono per decidere cosa fare oggi
@Getter
@Builder
public class AdminOperationalStatsResponse {
    private long totalUsers;
    private long reportsToday;
    // Utenti SUSPENDED (2ª violazione) in attesa di una decisione manuale
    // dell'admin — riammetterli o no, via reinstateUser() già esistente
    private long usersToReview;
}
