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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

        // Esattamente uno tra reviewId e reviewCommentId
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
                .reportedText(review.getText())
                .build();
        Report saved = reportRepository.save(report);

        if (review.getStatus() == ReviewStatus.VISIBLE) {
            // Solo le PENDING contano per la soglia — non le REJECTED storiche
            long pendingReports = reportRepository.countByReviewAndStatus(review, ReportStatus.PENDING);
            if (pendingReports >= 2) {
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
                .reportedText(comment.getText())
                .build();
        Report saved = reportRepository.save(report);

        if (comment.getStatus() == ReviewStatus.VISIBLE) {
            long pendingReports = reportRepository.countByReviewCommentAndStatus(comment, ReportStatus.PENDING);
            if (pendingReports >= 2) {
                comment.setStatus(ReviewStatus.HIDDEN);
                reviewCommentRepository.save(comment);
            }
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReports(ReportStatus status, ReportTargetType targetType, Pageable pageable) {
        // Le segnalazioni vengono raggruppate per bersaglio (review/commento)
        // e paginate sui GRUPPI, non sulle righe — altrimenti segnalazioni
        // sullo stesso bersaglio potrebbero finire su pagine diverse.
        List<List<Report>> groups = fetchGroupedReports(status, targetType);
        int totalGroups = groups.size();
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int from = Math.min(page * size, totalGroups);
        int to = Math.min(from + size, totalGroups);

        List<Report> pageContent = new ArrayList<>();
        for (List<Report> g : groups.subList(from, to)) {
            pageContent.addAll(g);
        }

        List<ReportResponse> dtos = pageContent.stream().map(this::toDTO).collect(Collectors.toList());
        // GroupedReportPage bypassa il ricalcolo automatico che PageImpl fa
        // nel suo costruttore quando offset+pageSize > total: qui "total" è
        // il numero di GRUPPI, ma "content" può contenere più righe di
        // quante sono le pagine (gruppi con più segnalazioni), facendo
        // scattare quel ricalcolo sbagliato.
        return new GroupedReportPage<>(dtos, pageable, totalGroups);
    }

    // Riusata da getReportsSummary() per non duplicare fetch + raggruppamento
    private List<List<Report>> fetchGroupedReports(ReportStatus status, ReportTargetType targetType) {
        List<Report> all;
        if (targetType == ReportTargetType.REVIEW) {
            all = (status != null)
                    ? reportRepository.findByStatusAndReviewIsNotNullOrderByCreatedAtDescIdAsc(status)
                    : reportRepository.findByReviewIsNotNullOrderByCreatedAtDescIdAsc();
        } else if (targetType == ReportTargetType.COMMENT) {
            all = (status != null)
                    ? reportRepository.findByStatusAndReviewCommentIsNotNullOrderByCreatedAtDescIdAsc(status)
                    : reportRepository.findByReviewCommentIsNotNullOrderByCreatedAtDescIdAsc();
        } else {
            all = (status != null)
                    ? reportRepository.findByStatusOrderByCreatedAtDescIdAsc(status)
                    : reportRepository.findAll();
        }

        LinkedHashMap<String, List<Report>> grouped = new LinkedHashMap<>();
        for (Report r : all) {
            String key = r.getReviewComment() != null
                    ? "comment_" + r.getReviewComment().getId()
                    : "review_" + r.getReview().getId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<List<Report>> groups = new ArrayList<>(grouped.values());
        for (List<Report> g : groups) {
            g.sort(Comparator.comparing(Report::getCreatedAt).reversed());
        }
        groups.sort((a, b) -> {
            int cmp = b.get(0).getCreatedAt().compareTo(a.get(0).getCreatedAt());
            return cmp != 0 ? cmp : a.get(0).getId().compareTo(b.get(0).getId());
        });
        return groups;
    }

    // Conteggio segnalazioni (righe) e bersagli distinti (gruppi), per la
    // card operativa in dashboard — indipendente dalla paginazione
    @Override
    @Transactional(readOnly = true)
    public com.project.ciaklog.dto.response.ReportSummaryResponse getReportsSummary(ReportStatus status, ReportTargetType targetType) {
        List<List<Report>> groups = fetchGroupedReports(status, targetType);
        long totalReports = groups.stream().mapToLong(List::size).sum();
        return com.project.ciaklog.dto.response.ReportSummaryResponse.builder()
                .totalReports(totalReports)
                .totalTargets(groups.size())
                .build();
    }

    @Override
    @Transactional
    public ReportResponse resolveReport(UUID reportId, ReportStatus newStatus, String adminUsername, ReportReasonCategory finalReasonCategory) {
        User admin = getUser(adminUsername);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Segnalazione non trovata"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BusinessRuleException("Segnalazione già risolta (stato attuale: " + report.getStatus() + ")");
        }

        report.setStatus(newStatus);
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());

        // Se le segnalazioni nello stesso gruppo hanno motivi diversi,
        // l'admin sceglie quello valido per tutte
        if (newStatus == ReportStatus.APPROVED && finalReasonCategory != null) {
            report.setReasonCategory(finalReasonCategory);
        }

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

    // -15 alla prima violazione, con un punto di sconto per reazione ricevuta
    // dal contenuto rimosso (contate al volo, non un contatore salvato).
    // Il bonus si applica solo alla 1a violazione, non alle sanzioni sull'utente.
    private void penalizeOffender(User offender, long reactionBonus) {
        offender.setViolationCount(offender.getViolationCount() + 1);

        if (offender.getViolationCount() >= 3) {
            offender.setStatus(UserStatus.PERMANENTLY_SUSPENDED);
            offender.setScore(0);
            userRepository.save(offender);
            hideAllContentForSuspendedUser(offender);
            // Sospensione permanente = irreversibile, quindi archiviamo le
            // segnalazioni PENDING rimaste sui suoi contenuti (vedi
            // AdminServiceImpl.suspendUser per lo stesso passaggio sulla via
            // manuale). Non succede alla sospensione temporanea sotto.
            archivePendingReportsForUnavailableAuthor(offender);
        } else if (offender.getViolationCount() == 2) {
            offender.setStatus(UserStatus.SUSPENDED);
            offender.setScore(Math.max(0, offender.getScore() - 20));
            userRepository.save(offender);
            hideAllContentForSuspendedUser(offender);
        } else {
            long delta = -15 + reactionBonus;
            offender.setScore((int) Math.max(0, offender.getScore() + delta));
            userRepository.save(offender);
        }
    }

    // Nasconde recensioni/risposte di un utente sospeso — usata sia qui
    // (sospensione da violazioni) sia da AdminServiceImpl (sospensione
    // manuale), stesso flag hiddenBySuspension
    private void hideAllContentForSuspendedUser(User user) {
        List<Review> ownReviews = reviewRepository.findAllByUser(user);
        for (Review r : ownReviews) {
            if (r.getStatus() == ReviewStatus.VISIBLE && !r.isHiddenBySuspension()) {
                r.setHiddenBySuspension(true);
            }
        }
        reviewRepository.saveAll(ownReviews);

        List<ReviewComment> ownComments = reviewCommentRepository.findAllByAuthor(user);
        for (ReviewComment c : ownComments) {
            if (c.getStatus() == ReviewStatus.VISIBLE && !c.isHiddenBySuspension()) {
                c.setHiddenBySuspension(true);
            }
        }
        reviewCommentRepository.saveAll(ownComments);
    }

    // "Nascondi direttamente" — crea un Report con reporter = admin, già
    // risolto APPROVED, riusando la stessa logica (penalità, cascata, audit)
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
                .reportedText(review.getText())
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
                .reportedText(comment.getText())
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

    @Override
    @Transactional
    public void archivePendingReportsForUnavailableAuthor(User author) {
        List<Review> reviews = reviewRepository.findAllByUser(author);
        if (!reviews.isEmpty()) {
            List<Report> pending = reportRepository.findAllByReviewInAndStatus(reviews, ReportStatus.PENDING);
            pending.forEach(r -> {
                r.setStatus(ReportStatus.ARCHIVED);
                r.setResolvedAt(LocalDateTime.now());
                // resolvedBy resta null: non è una decisione di un admin,
                // è una conseguenza automatica della sparizione dell'account
            });
            reportRepository.saveAll(pending);
        }

        List<ReviewComment> comments = reviewCommentRepository.findAllByAuthor(author);
        if (!comments.isEmpty()) {
            List<Report> pending = reportRepository.findAllByReviewCommentInAndStatus(comments, ReportStatus.PENDING);
            pending.forEach(r -> {
                r.setStatus(ReportStatus.ARCHIVED);
                r.setResolvedAt(LocalDateTime.now());
            });
            reportRepository.saveAll(pending);
        }
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
                .resolvedByUsername(r.getResolvedBy() != null ? r.getResolvedBy().getUsername() : null)
                // Un account sparito resta legato a segnalazioni passate;
                // non risolviamo automaticamente, ma diamo il contesto all'Admin
                .reporterAccountUnavailable(r.getReporter().getStatus() != UserStatus.ACTIVE);

        if (r.getReview() != null) {
            Review review = r.getReview();
            builder.targetType(ReportTargetType.REVIEW)
                    .tmdbId(review.getTmdbId())
                    .contentType(review.getContentType())
                    .reviewId(review.getId())
                    .reviewAuthorUsername(review.getUser().getUsername())
                    .reviewText(review.getText())
                    .reviewRating(review.getRating())
                    .targetRemoved(review.getStatus() == ReviewStatus.REMOVED)
                    .targetHidden(review.getStatus() == ReviewStatus.HIDDEN)
                    // Autore eliminato/sospeso permanentemente: contenuto già
                    // nascosto per sempre, ma la decisione resta all'Admin
                    .targetAuthorAccountUnavailable(review.getUser().getStatus() != UserStatus.ACTIVE)
                    .reportedText(r.getReportedText())
                    .targetEdited(r.getReportedText() != null && !r.getReportedText().equals(review.getText()));
        } else {
            ReviewComment comment = r.getReviewComment();
            // Il tmdbId/contentType di una risposta
            // sono quelli del film/serie della sua recensione madre, non suoi propri
            builder.targetType(ReportTargetType.COMMENT)
                    .tmdbId(comment.getReview().getTmdbId())
                    .contentType(comment.getReview().getContentType())
                    .reviewCommentId(comment.getId())
                    .parentReviewId(comment.getReview().getId())
                    .commentAuthorUsername(comment.getAuthor().getUsername())
                    .commentText(comment.getText())
                    .targetRemoved(comment.getStatus() == ReviewStatus.REMOVED)
                    .targetHidden(comment.getStatus() == ReviewStatus.HIDDEN)
                    .targetAuthorAccountUnavailable(comment.getAuthor().getStatus() != UserStatus.ACTIVE)
                    .reportedText(r.getReportedText())
                    .targetEdited(r.getReportedText() != null && !r.getReportedText().equals(comment.getText()));
        }

        return builder.build();
    }

    // Bypassa il ricalcolo automatico del totale che PageImpl fa nel suo
    // costruttore quando offset+pageSize > total, e riespone sempre il vero
    // numero di gruppi passato esplicitamente.
    private static class GroupedReportPage<T> extends PageImpl<T> {
        private final long realTotal;

        GroupedReportPage(List<T> content, Pageable pageable, long realTotal) {
            super(content, pageable, realTotal);
            this.realTotal = realTotal;
        }

        @Override
        public long getTotalElements() {
            return realTotal;
        }

        @Override
        public int getTotalPages() {
            int size = getPageable().getPageSize();
            return size == 0 ? 1 : (int) Math.ceil((double) realTotal / (double) size);
        }
    }
}
