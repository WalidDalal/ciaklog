package com.project.ciaklog.service;

import com.project.ciaklog.dto.response.AdminUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminService {
    Page<AdminUserResponse> listUsers(Pageable pageable);
    void suspendUser(UUID userId, String adminUsername);
    void reinstateUser(UUID userId);
}