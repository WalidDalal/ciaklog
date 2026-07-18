package com.project.ciaklog.dto.request;

import com.project.ciaklog.entity.ReportReasonCategory;
import com.project.ciaklog.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportActionRequest {

    @NotNull(message = "L'azione è obbligatoria")
    private ReportStatus action;

    // Fix (dashboard admin): quando un gruppo di segnalazioni sullo stesso
    // bersaglio ha motivi diversi (es. SPAM e INAPPROPRIATE_CONTENT), l'admin
    // sceglie quale motivo è quello valido tra quelli effettivamente usati
    // dagli utenti — opzionale, usato solo su action = APPROVED
    private ReportReasonCategory finalReasonCategory;
}