package com.project.ciaklog.controller;

import com.project.ciaklog.dto.response.AdminUserResponse;
import com.project.ciaklog.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(adminService.listUsers(pageable));
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<Void> suspendUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        adminService.suspendUser(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/reinstate")
    public ResponseEntity<Void> reinstateUser(@PathVariable UUID id) {
        adminService.reinstateUser(id);
        return ResponseEntity.noContent().build();
    }
}