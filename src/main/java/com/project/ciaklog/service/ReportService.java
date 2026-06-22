package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.ReportRequest;
import com.project.ciaklog.dto.response.ReportResponse;
import com.project.ciaklog.entity.ReportStatus;

import java.util.List;
import java.util.UUID;

public interface ReportService {
    ReportResponse createReport(String username, ReportRequest dto);
    List<ReportResponse> getPendingReports();
    ReportResponse resolveReport(UUID reportId, ReportStatus newStatus, String adminUsername);
}
