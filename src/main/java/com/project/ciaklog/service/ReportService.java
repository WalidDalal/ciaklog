package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.ReportRequest;
import com.project.ciaklog.dto.response.ReportResponse;
import com.project.ciaklog.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReportService {
    ReportResponse createReport(String username, ReportRequest dto);
    Page<ReportResponse> getReports(ReportStatus status, Pageable pageable);
    ReportResponse resolveReport(UUID reportId, ReportStatus newStatus, String adminUsername);
}