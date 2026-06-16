// ReportResponseDTO.java
package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ReportReasonCategory;
import com.project.ciaklog.entity.ReportStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReportResponseDTO {
    private UUID id;
    private UUID reviewId;
    private String reporterUsername;
    private ReportReasonCategory reasonCategory;
    private String reasonText;
    private ReportStatus status;
    private LocalDateTime createdAt;
}