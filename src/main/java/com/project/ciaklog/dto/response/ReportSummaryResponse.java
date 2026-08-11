package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

// Card admin — segnalazioni totali per lo status/tipo bersaglio filtrato
// attualmente in dashboard, sia come numero di RIGHE (segnalazioni singole)
// sia come numero di GRUPPI (bersagli distinti, es. una recensione con 2
// segnalazioni approvate da utenti diversi conta come 1 elemento qui)
@Getter
@Builder
public class ReportSummaryResponse {
    private long totalReports;
    private long totalTargets;
}
