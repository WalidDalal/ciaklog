package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ReportReasonCategory;
import com.project.ciaklog.entity.ReportStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReportResponse {
    private UUID id;
    private UUID reviewId;
    private String reporterUsername;

    // Dati recensione segnalata — per mostrarli nella dashboard admin
    private String reviewAuthorUsername;
    private String reviewText;
    private Integer reviewRating;

    private ReportReasonCategory reasonCategory;
    private String reasonText;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolvedByUsername;
}