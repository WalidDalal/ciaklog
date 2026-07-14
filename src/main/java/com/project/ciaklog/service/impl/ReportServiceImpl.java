package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.request.ReportRequest;
import com.project.ciaklog.dto.response.ReportResponse;
import com.project.ciaklog.entity.*;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.exception.DuplicateResourceException;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.ReportRepository;
import com.project.ciaklog.repository.ReviewCommentRepository;
import com.project.ciaklog.repository.ReviewReactionRepository;
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

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewReactionRepository reviewReactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReportResponse createReport(String username, ReportRequest dto) {
        User reporter = getUser(username);

        // Fix (moderazione risposte): esattamente uno tra reviewId e reviewCommentId
        boolean hasReview = dto.getReviewId() != null;
        boolean hasComment = dto.getReviewCommentId() != null;
        if (hasReview == hasComment) {
            throw new BusinessRuleException("Specifica esattamente un bersaglio: reviewId oppure reviewCommentId");
        }

        if (dto.getReasonCategory() == ReportReasonCategory.OTHER &&
                (dto.getReasonText() == null || dto.getReasonText().isBlank())) {
            throw new BusinessRuleException("Il motivo è obbligatorio quando la categoria è OTHER");
        }

        Report saved = hasReview
                ? createReviewReport(reporter, dto)
                : createCommentReport(reporter, dto);

        return toDTO(saved);
    }

    private Report createReviewReport(User reporter, ReportRequest dto) {
        Review review = reviewRepository.findById(dto.getReviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));

        if (review.getUser().getId().equals(reporter.getId())) {
            throw new BusinessRuleException("Non puoi segnalare la tua recensione");
        }
        if (reportRepository.existsByReporterAndReview(reporter, review)) {
            throw new DuplicateResourceException("Hai già segnalato questa recensione");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .review(review)
                .reasonCategory(dto.getReasonCategory())
                .reasonText(dto.getReasonText())
                .build();
        Report saved = reportRepository.save(report);

        if (review.getStatus() == ReviewStatus.VISIBLE) {
            long totalReports = reportRepository.countByReview(review);
            if (totalReports >= 2) {
                review.setStatus(ReviewStatus.HIDDEN);
                reviewRepository.save(review);
                cascadeHideComments(review, ReviewStatus.HIDDEN);
            }
        }

        return saved;
    }

    private Report createCommentReport(User reporter, ReportRequest dto) {
        ReviewComment comment = reviewCommentRepository.findById(dto.getReviewCommentId())
                .orElseThrow(() -> new ResourceNotFoundException("Risposta non trovata"));

        if (comment.getAuthor().getId().equals(reporter.getId())) {
            throw new BusinessRuleException("Non puoi segnalare la tua risposta");
        }
        if (reportRepository.existsByReporterAndReviewComment(reporter, comment)) {
            throw new DuplicateResourceException("Hai già segnalato questa risposta");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reviewComment(comment)
                .reasonCategory(dto.getReasonCategory())
                .reasonText(dto.getReasonText())
                .build();
        Report saved = reportRepository.save(report);

        if (comment.getStatus() == ReviewStatus.VISIBLE) {
            long totalReports = reportRepository.countByReviewComment(comment);
            if (totalReports >= 2) {
                comment.setStatus(ReviewStatus.HIDDEN);
                reviewCommentRepository.save(comment);
            }
        }

        return saved;
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

        if (report.getReview() != null) {
            resolveReviewTarget(report, newStatus);
        } else {
            resolveCommentTarget(report, newStatus);
        }

        return toDTO(reportRepository.save(report));
    }

    private void resolveReviewTarget(Report report, ReportStatus newStatus) {
        Review review = report.getReview();

        if (newStatus == ReportStatus.APPROVED) {
            if (review.getStatus() != ReviewStatus.REMOVED) {
                review.setStatus(ReviewStatus.REMOVED);
                reviewRepository.save(review);
                cascadeHideComments(review, ReviewStatus.REMOVED);
                penalizeOffender(review.getUser(), reviewReactionRepository.countByReview(review));
            }

        } else if (newStatus == ReportStatus.REJECTED) {
            boolean hasOtherPending = reportRepository.findByReview(review).stream()
                    .anyMatch(r -> r.getStatus() == ReportStatus.PENDING);

            if (review.getStatus() != ReviewStatus.REMOVED && !hasOtherPending) {
                review.setStatus(ReviewStatus.VISIBLE);
                reviewRepository.save(review);
            }
        }
    }

    private void resolveCommentTarget(Report report, ReportStatus newStatus) {
        ReviewComment comment = report.getReviewComment();

        if (newStatus == ReportStatus.APPROVED) {
            if (comment.getStatus() != ReviewStatus.REMOVED) {
                comment.setStatus(ReviewStatus.REMOVED);
                reviewCommentRepository.save(comment);
                penalizeOffender(comment.getAuthor(), reviewReactionRepository.countByReviewComment(comment));
            }

        } else if (newStatus == ReportStatus.REJECTED) {
            boolean hasOtherPending = reportRepository.findByReviewComment(comment).stream()
                    .anyMatch(r -> r.getStatus() == ReportStatus.PENDING);

            if (comment.getStatus() != ReviewStatus.REMOVED && !hasOtherPending) {
                comment.setStatus(ReviewStatus.VISIBLE);
                reviewCommentRepository.save(comment);
            }
        }
    }

    private void cascadeHideComments(Review review, ReviewStatus newStatus) {
        List<ReviewComment> comments = reviewCommentRepository.findAllByReviewAndStatus(review, ReviewStatus.VISIBLE);
        comments.forEach(c -> c.setStatus(newStatus));
        reviewCommentRepository.saveAll(comments);
    }

    // Fix (coerenza punteggio alla rimozione): il -15 base resta identico a
    // prima, ma ora si aggiunge indietro 1 punto per ogni reazione ricevuta
    // dal contenuto rimosso — un contenuto molto apprezzato dalla community
    // pesa meno nella sanzione. Contate al volo (COUNT), MAI un contatore
    // salvato a parte (deciso). Il bonus si applica SOLO alla 1a violazione:
    // il -20 di sospensione e lo 0 di sospensione permanente restano sanzioni
    // sull'utente nel suo complesso, non sul singolo contenuto rimosso
    private void penalizeOffender(User offender, long reactionBonus) {
        offender.setViolationCount(offender.getViolationCount() + 1);

        if (offender.getViolationCount() >= 3) {
            offender.setStatus(UserStatus.PERMANENTLY_SUSPENDED);
            offender.setScore(0);
        } else if (offender.getViolationCount() == 2) {
            offender.setStatus(UserStatus.SUSPENDED);
            offender.setScore(Math.max(0, offender.getScore() - 20));
        } else {
            long delta = -15 + reactionBonus;
            offender.setScore((int) Math.max(0, offender.getScore() + delta));
        }

        userRepository.save(offender);
    }

    // Fix: "Nascondi direttamente" — crea un Report con reporter = admin, già
    // risolto APPROVED. Riusa al 100% la logica esistente (penalità, cascata,
    // audit trail nella stessa dashboard) invece di duplicarla altrove.
    @Override
    @Transactional
    public ReportResponse adminHide(String adminUsername, ReportRequest dto) {
        User admin = getUser(adminUsername);

        boolean hasReview = dto.getReviewId() != null;
        boolean hasComment = dto.getReviewCommentId() != null;
        if (hasReview == hasComment) {
            throw new BusinessRuleException("Specifica esattamente un bersaglio: reviewId oppure reviewCommentId");
        }
        if (dto.getReasonCategory() == null) {
            throw new BusinessRuleException("Il motivo è obbligatorio per nascondere un contenuto");
        }
        if (dto.getReasonCategory() == ReportReasonCategory.OTHER &&
                (dto.getReasonText() == null || dto.getReasonText().isBlank())) {
            throw new BusinessRuleException("Il motivo è obbligatorio quando la categoria è OTHER");
        }

        Report report = hasReview
                ? adminHideReview(admin, dto)
                : adminHideComment(admin, dto);

        return toDTO(report);
    }

    private Report adminHideReview(User admin, ReportRequest dto) {
        Review review = reviewRepository.findById(dto.getReviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Recensione non trovata"));

        Report report = Report.builder()
                .reporter(admin)
                .review(review)
                .reasonCategory(dto.getReasonCategory())
                .reasonText(dto.getReasonText())
                .status(ReportStatus.APPROVED)
                .resolvedBy(admin)
                .resolvedAt(LocalDateTime.now())
                .build();
        Report saved = reportRepository.save(report);

        // Guard: se già REMOVED (es. da un report precedente), non penalizzare di nuovo
        if (review.getStatus() != ReviewStatus.REMOVED) {
            review.setStatus(ReviewStatus.REMOVED);
            reviewRepository.save(review);
            cascadeHideComments(review, ReviewStatus.REMOVED);
            penalizeOffender(review.getUser(), reviewReactionRepository.countByReview(review));
        }

        return saved;
    }

    private Report adminHideComment(User admin, ReportRequest dto) {
        ReviewComment comment = reviewCommentRepository.findById(dto.getReviewCommentId())
                .orElseThrow(() -> new ResourceNotFoundException("Risposta non trovata"));

        Report report = Report.builder()
                .reporter(admin)
                .reviewComment(comment)
                .reasonCategory(dto.getReasonCategory())
                .reasonText(dto.getReasonText())
                .status(ReportStatus.APPROVED)
                .resolvedBy(admin)
                .resolvedAt(LocalDateTime.now())
                .build();
        Report saved = reportRepository.save(report);

        if (comment.getStatus() != ReviewStatus.REMOVED) {
            comment.setStatus(ReviewStatus.REMOVED);
            reviewCommentRepository.save(comment);
            penalizeOffender(comment.getAuthor(), reviewReactionRepository.countByReviewComment(comment));
        }

        return saved;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    private ReportResponse toDTO(Report r) {
        ReportResponse.ReportResponseBuilder builder = ReportResponse.builder()
                .id(r.getId())
                .reporterUsername(r.getReporter().getUsername())
                .reasonCategory(r.getReasonCategory())
                .reasonText(r.getReasonText())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .resolvedAt(r.getResolvedAt())
                .resolvedByUsername(r.getResolvedBy() != null ? r.getResolvedBy().getUsername() : null);

        if (r.getReview() != null) {
            Review review = r.getReview();
            builder.targetType(ReportTargetType.REVIEW)
                    .tmdbId(review.getTmdbId())
                    .contentType(review.getContentType())
                    .reviewId(review.getId())
                    .reviewAuthorUsername(review.getUser().getUsername())
                    .reviewText(review.getText())
                    .reviewRating(review.getRating());
        } else {
            ReviewComment comment = r.getReviewComment();
            // Fix (dashboard admin, Step 6): il tmdbId/contentType di una risposta
            // sono quelli del film/serie della sua recensione madre, non suoi propri
            builder.targetType(ReportTargetType.COMMENT)
                    .tmdbId(comment.getReview().getTmdbId())
                    .contentType(comment.getReview().getContentType())
                    .reviewCommentId(comment.getId())
                    .parentReviewId(comment.getReview().getId())
                    .commentAuthorUsername(comment.getAuthor().getUsername())
                    .commentText(comment.getText());
        }

        return builder.build();
    }
}
