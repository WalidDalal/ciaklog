package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.response.AdminUserResponse;
import com.project.ciaklog.entity.User;
import com.project.ciaklog.entity.UserStatus;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;

    @Override
    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toAdminDTO);
    }

    @Override
    public void suspendUser(UUID userId, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trovato"));
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        if (target.getId().equals(admin.getId())) {
            throw new BusinessRuleException("Non puoi sospendere te stesso");
        }
        if (target.getStatus() == UserStatus.PERMANENTLY_SUSPENDED) {
            throw new BusinessRuleException("Utente già sospeso permanentemente");
        }

        target.setStatus(UserStatus.SUSPENDED);
        userRepository.save(target);
    }

    @Override
    public void reinstateUser(UUID userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        if (target.getStatus() == UserStatus.PERMANENTLY_SUSPENDED) {
            throw new BusinessRuleException("Utente sospeso permanentemente — non riabilitabile");
        }

        target.setStatus(UserStatus.ACTIVE);
        userRepository.save(target);
    }

    private AdminUserResponse toAdminDTO(User u) {
        return AdminUserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .status(u.getStatus())
                .violationCount(u.getViolationCount())
                .build();
    }
}