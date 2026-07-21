package com.project.ciaklog.service;

import com.project.ciaklog.dto.response.AdminOperationalStatsResponse;
import com.project.ciaklog.dto.response.AdminUserDetailResponse;
import com.project.ciaklog.dto.response.AdminUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminService {
    Page<AdminUserResponse> listUsers(Pageable pageable, String search);
    AdminUserDetailResponse getUserDetail(UUID userId);
    void suspendUser(UUID userId, String adminUsername, String reason);
    void reinstateUser(UUID userId);

    // Card operativa leggera
    AdminOperationalStatsResponse getOperationalStats();
}