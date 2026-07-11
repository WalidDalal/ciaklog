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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Transactional
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

        Report saved = reportRepository.save(report);

        // Auto-hide: 1a segnalazione -> resta VISIBLE (solo in dashboard admin).
        // 2a segnalazione (da utente diverso, già garantito dal check anti-duplicato sopra)
        // sulla stessa recensione -> passa a HIDDEN, sparisce da film/serie ma resta
        // visibile in dashboard admin per la revisione finale.
        if (review.getStatus() == ReviewStatus.VISIBLE) {
            long totalReports = reportRepository.countByReview(review);
            if (totalReports >= 2) {
                review.setStatus(ReviewStatus.HIDDEN);
                reviewRepository.save(review);
            }
        }

        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReports(ReportStatus status, Pageable pageable) {
        Page<Report> reports = (status != null)
                ? reportRepository.findByStatus(status, pageable)
                : reportRepository.findAll(pageable);
        return reports.map(this::toDTO);
    }

    @Override
    @Transactional
    public ReportResponse resolveReport(UUID reportId, ReportStatus newStatus, String adminUsername) {
        User admin = getUser(adminUsername);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Segnalazione non trovata"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BusinessRuleException("Segnalazione già risolta (stato attuale: " + report.getStatus() + ")");
        }

        report.setStatus(newStatus);
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());

        Review review = report.getReview();

        if (newStatus == ReportStatus.APPROVED) {
            // Guard: se la review è già REMOVED (rimossa da una segnalazione precedente),
            // non penalizzare di nuovo l'autore — aggiorna solo lo stato del report
            if (review.getStatus() != ReviewStatus.REMOVED) {
                review.setStatus(ReviewStatus.REMOVED);
                reviewRepository.save(review);

                User offender = review.getUser();
                offender.setViolationCount(offender.getViolationCount() + 1);

                if (offender.getViolationCount() >= 3) {
                    // Terza violazione: sospensione permanente, score azzerato
                    offender.setStatus(UserStatus.PERMANENTLY_SUSPENDED);
                    offender.setScore(0);
                } else if (offender.getViolationCount() == 2) {
                    // Seconda violazione: sospensione temporanea, penale -20
                    offender.setStatus(UserStatus.SUSPENDED);
                    offender.setScore(Math.max(0, offender.getScore() - 20));
                } else {
                    // Prima violazione: solo penale -15
                    offender.setScore(Math.max(0, offender.getScore() - 15));
                }

                userRepository.save(offender);
            }

        } else if (newStatus == ReportStatus.REJECTED) {
            // Fix: se la review era stata auto-nascosta (HIDDEN) per via di più
            // segnalazioni, rifiutarne UNA sola non deve rimetterla visibile se
            // esistono ancora altre segnalazioni PENDING sulla stessa recensione.
            boolean hasOtherPending = reportRepository.findByReview(review).stream()
                    .anyMatch(r -> r.getStatus() == ReportStatus.PENDING);

            if (review.getStatus() != ReviewStatus.REMOVED && !hasOtherPending) {
                review.setStatus(ReviewStatus.VISIBLE);
                reviewRepository.save(review);
            }
        }

        return toDTO(reportRepository.save(report));
    }

    // ── helpers ──

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    private ReportResponse toDTO(Report r) {
        Review review = r.getReview();
        return ReportResponse.builder()
                .id(r.getId())
                .reviewId(review.getId())
                .reporterUsername(r.getReporter().getUsername())
                .reviewAuthorUsername(review.getUser().getUsername())
                .reviewText(review.getText())
                .reviewRating(review.getRating())
                .reasonCategory(r.getReasonCategory())
                .reasonText(r.getReasonText())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .resolvedAt(r.getResolvedAt())
                .resolvedByUsername(r.getResolvedBy() != null ? r.getResolvedBy().getUsername() : null)
                .build();
    }
}