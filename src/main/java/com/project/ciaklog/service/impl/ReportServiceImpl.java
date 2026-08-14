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
            // Contava tutte le
            // segnalazioni storiche, incluse REJECTED da valutazioni passate
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
            // Fix (Logica Moderazione — stessa correzione delle recensioni)
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
        // Fix (dashboard admin — filtro per tipo bersaglio): filtro applicato
        // qui, nella query, PRIMA della paginazione — così vale su tutte le
        // pagine e non solo su quella caricata in un dato momento.
        //
        // Fix (dashboard admin — gruppi di segnalazioni spezzati tra le
        // pagine): prima si paginava direttamente sulle RIGHE di Report (una
        // per segnalazione), quindi 2 segnalazioni sullo stesso bersaglio
        // potevano finire su pagine diverse — es. pagina 1 mostra "2
        // segnalazioni" (parziali) su una review, pagina 2 ne mostra altre
        // sulla stessa review come se fosse un caso diverso. Ora si carica la
        // lista completa (filtrata per status/targetType, senza paginazione
        // DB), si raggruppa per bersaglio (review o commento), si ordina ogni
        // gruppo per la segnalazione più recente al suo interno, e SOLO A
        // QUESTO PUNTO si pagina — sui gruppi, non sulle righe. Un gruppo
        // finisce quindi sempre intero in una sola pagina.
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
        // Fix (🔴 FIX — "6 recensioni, pagina 1 di 2"): PageImpl standard
        // ricalcola DA SOLO il totale nel costruttore se rileva
        // offset+pageSize > total (qui: 0+10 > 6, essendo 6 il numero di
        // GRUPPI) — assume che total fosse sbagliato e lo sovrascrive con
        // offset + content.size(). Ma "content" qui è la lista delle RIGHE
        // di segnalazione appiattite dai gruppi di questa pagina (es. 12
        // righe per 6 gruppi, se alcune recensioni hanno più di una
        // segnalazione approvata) — non una riga per gruppo. Quell'euristica
        // di PageImpl assume invece content.size() <= pageSize, quindi
        // "corregge" il nostro totale corretto (6 gruppi) con uno sbagliato
        // (12, il numero di righe), dando ceil(12/10)=2 pagine invece di 1.
        // GroupedReportPage ignora quel ricalcolo e riespone sempre il vero
        // totale di gruppi passato esplicitamente.
        return new GroupedReportPage<>(dtos, pageable, totalGroups);
    }

    // Estratto da getReports() — stessa identica logica di fetch + raggruppamento
    // per bersaglio, riusata anche da getReportsSummary() per non duplicarla
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

    // Card admin — totale segnalazioni (righe) + totale bersagli distinti
    // (gruppi), per lo status/tipo filtrato in dashboard, indipendente dalla
    // paginazione — così la card può mostrare "N segnalazioni totali di X
    // elementi" invece del solo numero di pagina, per qualunque filtro attivo
    // (non solo PENDING, come faceva prima la card operativa)
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

        // Quando le segnalazioni sullo stesso bersaglio
        // hanno motivi diversi, l'admin sceglie quale è quello valido — lo
        // applichiamo qui a QUESTO report così tutti quelli approvati insieme
        // nello stesso gruppo finiscono coerenti sullo stesso motivo (invece
        // di lasciare che sia il primo trovato più avanti a "vincere" a caso)
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

    // Il -15 base resta identico a
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
            userRepository.save(offender);
            hideAllContentForSuspendedUser(offender);
            // Solo qui (sospensione PERMANENTE, irreversibile) archiviamo le
            // segnalazioni PENDING rimaste sui suoi contenuti — vedi
            // AdminServiceImpl.suspendUser per la stessa logica sul percorso
            // di sospensione manuale. NON alla sospensione temporanea
            // (ramo violationCount==2 sotto), che è reversibile.
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

    // Trovato in revisione: questa era la SECONDA via per sospendere un
    // utente (raggiungendo la soglia di violazioni approvando segnalazioni),
    // separata da AdminServiceImpl.suspendUser (sospensione manuale diretta)
    // — solo quest'ultima nascondeva in blocco recensioni/risposte
    // dell'utente sospeso, questa via restava scoperta e le lasciava
    // visibili. Stessa logica, stesso flag hiddenBySuspension.
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
                // Un utente eliminato/sospeso permanentemente resta comunque
                // legato a segnalazioni passate come segnalante — non
                // tocchiamo la segnalazione, ma diamo il contesto all'Admin
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
                    // Se l'autore della recensione è eliminato o sospeso
                    // permanentemente, il contenuto è già nascosto per
                    // sempre (hiddenByDeletion/hiddenBySuspension, senza
                    // possibilità di riabilitazione) — approvare/rifiutare
                    // qui non cambia nulla nella pratica, ma lasciamo
                    // comunque la decisione all'Admin invece di risolvere
                    // in automatico (un contenuto può avere più motivi)
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

    // Fix (🔴 FIX — "6 recensioni, pagina 1 di 2"): sottoclasse di PageImpl
    // che ignora il ricalcolo automatico del totale fatto dal costruttore di
    // PageImpl (vedi commento sopra, nel punto in cui viene istanziata) e
    // riespone sempre il vero numero di gruppi passato esplicitamente, sia
    // in getTotalElements() sia in getTotalPages() (quest'ultimo ricalcolato
    // qui con la dimensione pagina RICHIESTA — pageable.getPageSize() — e
    // non con getSize()/content.size() come farebbe l'implementazione
    // originale ereditata).
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
