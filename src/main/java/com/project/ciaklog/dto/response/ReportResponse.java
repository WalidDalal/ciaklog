package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ReportReasonCategory;
import com.project.ciaklog.entity.ReportStatus;
import com.project.ciaklog.entity.ReportTargetType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReportResponse {
    private UUID id;

    // Fix (moderazione risposte): distingue se il bersaglio è una recensione
    // o una risposta — la dashboard admin usa questo per non essere ambigua
    private ReportTargetType targetType;

    // Popolati quando targetType = REVIEW
    private UUID reviewId;
    private String reviewAuthorUsername;
    private String reviewText;
    private Integer reviewRating;

    // Popolati quando targetType = COMMENT
    private UUID reviewCommentId;
    private UUID parentReviewId; // per navigare/raggruppare sotto la recensione madre
    private String commentAuthorUsername;
    private String commentText;

    private String reporterUsername;
    private ReportReasonCategory reasonCategory;
    private String reasonText;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolvedByUsername;
}