package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.ReportRequest;
import com.project.ciaklog.dto.response.ReportResponse;
import com.project.ciaklog.entity.ReportStatus;
import com.project.ciaklog.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReportService {
    ReportResponse createReport(String username, ReportRequest dto);
    Page<ReportResponse> getReports(ReportStatus status, ReportTargetType targetType, Pageable pageable);

    // Card admin — totale segnalazioni (righe) + totale bersagli distinti
    // (gruppi) per lo status/tipo filtrato, indipendente dalla paginazione
    com.project.ciaklog.dto.response.ReportSummaryResponse getReportsSummary(ReportStatus status, ReportTargetType targetType);
    ReportResponse resolveReport(UUID reportId, ReportStatus newStatus, String adminUsername, com.project.ciaklog.entity.ReportReasonCategory finalReasonCategory);

    // "Nascondi direttamente" — l'admin rimuove una recensione/risposta senza
    // aspettare una segnalazione. Motivo SEMPRE obbligatorio (validato nel service),
    // stessa penalità di un report approvato (deciso)
    ReportResponse adminHide(String adminUsername, ReportRequest dto);
}