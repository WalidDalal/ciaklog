package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.response.AdminUserDetailResponse;
import com.project.ciaklog.dto.response.AdminUserResponse;
import com.project.ciaklog.entity.*;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.ReportRepository;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;

    @Override
    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toAdminDTO);
    }

    @Override
    public AdminUserDetailResponse getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        List<Review> allReviews = reviewRepository.findAllByUser(user);

        int reviewCount = (int) allReviews.stream()
                .filter(r -> r.getStatus() == ReviewStatus.VISIBLE)
                .count();

        // Fix N+1: carica tutti i report in una query sola invece di 1 query per review
        List<com.project.ciaklog.entity.Report> allReports = reportRepository.findAllByReviewIn(allReviews);

        int reportCount = allReports.size();

        // Raggruppa i report per review ID per accesso O(1) nello stream
        Map<UUID, List<com.project.ciaklog.entity.Report>> reportsByReviewId = allReports.stream()
                .collect(Collectors.groupingBy(rep -> rep.getReview().getId()));

        List<AdminUserDetailResponse.ViolationItem> violations = allReviews.stream()
                .filter(r -> r.getStatus() == ReviewStatus.REMOVED)
                .map(r -> {
                    List<com.project.ciaklog.entity.Report> reviewReports =
                            reportsByReviewId.getOrDefault(r.getId(), List.of());

                    var approvedReport = reviewReports.stream()
                            .filter(rep -> rep.getStatus() == ReportStatus.APPROVED)
                            .findFirst();

                    String category = approvedReport
                            .map(rep -> rep.getReasonCategory().name())
                            .orElse("Rimossa manualmente");
                    String reasonText = approvedReport
                            .map(rep -> rep.getReasonText())
                            .orElse(null);

                    return AdminUserDetailResponse.ViolationItem.builder()
                            .category(category)
                            .reasonText(reasonText)
                            .reviewText(r.getText() != null ? r.getText() : "—")
                            .date(r.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus())
                .violationCount(user.getViolationCount())
                .reviewCount(reviewCount)
                .reportCount(reportCount)
                .createdAt(user.getCreatedAt())
                .violations(violations)
                .build();
    }

    @Override
    @Transactional
    public void suspendUser(UUID userId, String adminUsername, String reason) {
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

        target.setViolationCount(target.getViolationCount() + 1);

        if (target.getViolationCount() >= 3) {
            target.setStatus(UserStatus.PERMANENTLY_SUSPENDED);
            target.setScore(0);
        } else {
            target.setStatus(UserStatus.SUSPENDED);
            target.setScore(Math.max(0, target.getScore() - 20));
        }

        userRepository.save(target);
    }

    @Override
    @Transactional
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