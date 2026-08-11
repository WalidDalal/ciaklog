package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.response.AdminOperationalStatsResponse;
import com.project.ciaklog.dto.response.AdminUserDetailResponse;
import com.project.ciaklog.dto.response.AdminUserResponse;
import com.project.ciaklog.entity.*;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.exception.ResourceNotFoundException;
import com.project.ciaklog.repository.ReportRepository;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final com.project.ciaklog.repository.ReviewCommentRepository reviewCommentRepository;
    private final com.project.ciaklog.repository.ManualSuspensionLogRepository manualSuspensionLogRepository;

    @Override
    public Page<AdminUserResponse> listUsers(Pageable pageable, String search) {
        // La ricerca non era mai collegata — il parametro arrivava dal
        // controller ma veniva ignorato qui, sempre e solo findAll()
        if (search != null && !search.isBlank()) {
            return userRepository
                    .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable)
                    .map(this::toAdminDTO);
        }
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

        List<AdminUserDetailResponse.ViolationItem> reportViolations = allReviews.stream()
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

        // Trovato in revisione: una sospensione manuale non crea nessun
        // Report, quindi restava invisibile nello storico violazioni (solo
        // il numero saliva, senza dettaglio). Aggiunto qui, unito e
        // riordinato insieme alle violazioni da segnalazione approvata.
        List<AdminUserDetailResponse.ViolationItem> suspensionViolations =
                manualSuspensionLogRepository.findAllByUser(user).stream()
                        .map(log -> AdminUserDetailResponse.ViolationItem.builder()
                                .category("SOSPENSIONE MANUALE")
                                .reasonText(log.getReason() + (log.getAdmin() != null ? " (da " + log.getAdmin().getUsername() + ")" : ""))
                                .reviewText("—")
                                .date(log.getCreatedAt())
                                .build())
                        .collect(Collectors.toList());

        List<AdminUserDetailResponse.ViolationItem> violations = java.util.stream.Stream
                .concat(reportViolations.stream(), suspensionViolations.stream())
                .sorted(Comparator.comparing(AdminUserDetailResponse.ViolationItem::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(maskEmail(user.getEmail()))
                .status(user.getStatus())
                .violationCount(user.getViolationCount())
                .reviewCount(reviewCount)
                .reportCount(reportCount)
                .createdAt(user.getCreatedAt())
                .violations(violations)
                .role(user.getRole())
                .suspensionReason(user.getSuspensionReason())
                .build();
    }

    // Fix (dashboard admin — privacy): la mail completa non dovrebbe essere
    // visibile nel drawer admin — solo la prima lettera, poi asterischi,
    // poi il dominio (es. "m****@esempio.com"). Nessuna reale necessità
    // amministrativa di vedere l'indirizzo per intero da qui.
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.isEmpty()) return email;
        return local.charAt(0) + "****" + domain;
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
        // Un Admin non può sospendere un altro Admin —
        // prima la regola valeva solo per "te stesso"
        if (target.getRole() == Role.ADMIN) {
            throw new BusinessRuleException("Non puoi sospendere un altro Admin");
        }
        if (target.getStatus() == UserStatus.PERMANENTLY_SUSPENDED) {
            throw new BusinessRuleException("Utente già sospeso permanentemente");
        }
        // Fix (trovato in revisione): non c'era nessun controllo che impedisse
        // di sospendere di nuovo un utente già SUSPENDED — un admin poteva
        // farlo ripetutamente, incrementando ogni volta violationCount fino a
        // farlo scattare a PERMANENTLY_SUSPENDED senza una vera nuova violazione
        // di mezzo. Per riabilitarlo o valutare un'escalation reale, passa
        // prima da reinstateUser oppure da una segnalazione approvata.
        if (target.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessRuleException("Utente già sospeso — riabilitalo prima di poterlo sospendere di nuovo");
        }

        target.setViolationCount(target.getViolationCount() + 1);
        target.setSuspensionReason(reason);

        // Trovato in revisione: alla riabilitazione suspensionReason viene
        // azzerato — corretto per "stato attuale", ma così si perdeva la
        // storia (un utente riabilitato risultava con violationCount
        // incrementato ma NESSUN dettaglio a spiegarlo, perché non è stata
        // creata nessuna segnalazione). Questo log invece resta per sempre.
        manualSuspensionLogRepository.save(ManualSuspensionLog.builder()
                .user(target)
                .admin(admin)
                .reason(reason)
                .build());

        if (target.getViolationCount() >= 3) {
            target.setStatus(UserStatus.PERMANENTLY_SUSPENDED);
            target.setScore(0);
        } else {
            target.setStatus(UserStatus.SUSPENDED);
            target.setScore(Math.max(0, target.getScore() - 20));
        }

        userRepository.save(target);

        // Nascondi in blocco tutte le recensioni/risposte VISIBLE dell'utente
        // sospeso — sia sospensione temporanea che permanente, per decisione
        // esplicita: più drastico e coerente con "utente sospeso", senza far
        // passare ogni singolo contenuto per l'approvazione di un report.
        // hiddenBySuspension è un flag SEPARATO da hiddenByAuthor apposta:
        // se l'utente aveva già nascosto qualcosa di suo prima della
        // sospensione, quel nascondimento personale resta intatto e non
        // viene "ripristinato per errore" alla riabilitazione.
        List<Review> ownReviews = reviewRepository.findAllByUser(target);
        for (Review r : ownReviews) {
            if (r.getStatus() == ReviewStatus.VISIBLE && !r.isHiddenBySuspension()) {
                r.setHiddenBySuspension(true);
            }
        }
        reviewRepository.saveAll(ownReviews);

        List<ReviewComment> ownComments = reviewCommentRepository.findAllByAuthor(target);
        for (ReviewComment c : ownComments) {
            if (c.getStatus() == ReviewStatus.VISIBLE && !c.isHiddenBySuspension()) {
                c.setHiddenBySuspension(true);
            }
        }
        reviewCommentRepository.saveAll(ownComments);
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
        target.setSuspensionReason(null);
        userRepository.save(target);

        // Ripristina SOLO ciò che era nascosto per la sospensione — se
        // l'utente aveva già nascosto qualcosa di suo (hiddenByAuthor) prima
        // di essere sospeso, quello resta nascosto: sono due flag
        // indipendenti, tocchiamo solo hiddenBySuspension qui
        List<Review> ownReviews = reviewRepository.findAllByUser(target);
        for (Review r : ownReviews) {
            if (r.isHiddenBySuspension()) {
                r.setHiddenBySuspension(false);
            }
        }
        reviewRepository.saveAll(ownReviews);

        List<ReviewComment> ownComments = reviewCommentRepository.findAllByAuthor(target);
        for (ReviewComment c : ownComments) {
            if (c.isHiddenBySuspension()) {
                c.setHiddenBySuspension(false);
            }
        }
        reviewCommentRepository.saveAll(ownComments);
    }

    // Card operativa leggera, non una dashboard
    // ricopiata — solo segnalazioni di oggi e utenti da controllare
    @Override
    public AdminOperationalStatsResponse getOperationalStats() {
        java.time.LocalDateTime midnight = java.time.LocalDate.now().atStartOfDay();

        return AdminOperationalStatsResponse.builder()
                .totalUsers(userRepository.count())
                .reportsToday(reportRepository.countByCreatedAtAfter(midnight))
                .usersToReview(userRepository.countByStatus(UserStatus.SUSPENDED))
                .build();
    }

    private AdminUserResponse toAdminDTO(User u) {
        return AdminUserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .status(u.getStatus())
                .violationCount(u.getViolationCount())
                .role(u.getRole())
                .build();
    }
}