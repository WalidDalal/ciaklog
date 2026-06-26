package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.request.ReportRequest;
import com.project.ciaklog.dto.response.ReportResponse;
import com.project.ciaklog.entity.*;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.exception.DuplicateResourceException;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.ReportRepository;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    public ReportResponse createReport(String username, ReportRequest dto) {
        User reporter = getUser(username);
        Review review = reviewRepository.findById(dto.getReviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));

        if (review.getUser().getId().equals(reporter.getId())) {
            throw new BusinessRuleException("Non puoi segnalare la tua recensione");
        }

        if (reportRepository.existsByReporterAndReview(reporter, review)) {
            throw new DuplicateResourceException("Hai già segnalato questa recensione");
        }

        if (dto.getReasonCategory() == ReportReasonCategory.OTHER &&
                (dto.getReasonText() == null || dto.getReasonText().isBlank())) {
            throw new BusinessRuleException("Il motivo è obbligatorio quando la categoria è OTHER");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .review(review)
                .reasonCategory(dto.getReasonCategory())
                .reasonText(dto.getReasonText())
                .build();

        return toDTO(reportRepository.save(report));
    }

    @Override
    public List<ReportResponse> getReports(ReportStatus status) {
        // Se status è null restituisce tutti, altrimenti filtra
        List<Report> reports = (status != null)
                ? reportRepository.findByStatus(status)
                : reportRepository.findAll();
        return reports.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ReportResponse resolveReport(UUID reportId, ReportStatus newStatus, String adminUsername) {
        User admin = getUser(adminUsername);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Segnalazione non trovata"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BusinessRuleException("Segnalazione già risolta (stato attuale: " + report.getStatus() + ")");
        }

        report.setStatus(newStatus);
        report.setResolvedBy(admin);

        Review review = report.getReview();

        if (newStatus == ReportStatus.APPROVED) {
            review.setStatus(ReviewStatus.REMOVED);
            reviewRepository.save(review);

            User offender = review.getUser();
            offender.setViolationCount(offender.getViolationCount() + 1);

            if (offender.getViolationCount() >= 3) {
                offender.setStatus(UserStatus.PERMANENTLY_SUSPENDED);
            } else if (offender.getViolationCount() == 2) {
                offender.setStatus(UserStatus.SUSPENDED);
            }
            userRepository.save(offender);

        } else if (newStatus == ReportStatus.PAUSED) {
            review.setStatus(ReviewStatus.HIDDEN);
            reviewRepository.save(review);

        } else if (newStatus == ReportStatus.REJECTED) {
            review.setStatus(ReviewStatus.VISIBLE);
            reviewRepository.save(review);
        }

        return toDTO(reportRepository.save(report));
    }

    // ── helpers ──

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    private ReportResponse toDTO(Report r) {
        return ReportResponse.builder()
                .id(r.getId())
                .reviewId(r.getReview().getId())
                .reporterUsername(r.getReporter().getUsername())
                .reasonCategory(r.getReasonCategory())
                .reasonText(r.getReasonText())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}