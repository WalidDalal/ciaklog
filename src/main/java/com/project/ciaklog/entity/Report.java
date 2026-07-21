package com.project.ciaklog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "reports",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"reporter_id", "review_id"}),
                // Fix (moderazione risposte): stesso vincolo anti-duplicato, applicato
                // anche quando il bersaglio è una risposta invece di una recensione
                @UniqueConstraint(columnNames = {"reporter_id", "review_comment_id"})
        },
        indexes = {
            @Index(name = "idx_report_status", columnList = "status"),
            @Index(name = "idx_report_review_id", columnList = "review_id"),
            @Index(name = "idx_report_review_comment_id", columnList = "review_comment_id")
        }
)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_category", nullable = false)
    private ReportReasonCategory reasonCategory;

    @Column(name = "reason_text", length = 500)
    private String reasonText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Fix: campo mancante — necessario per registrare quando la segnalazione è stata risolta
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // Fix (moderazione risposte): una segnalazione ha ESATTAMENTE UN bersaglio,
    // o una Review o una ReviewComment (mai entrambi, mai nessuno — validato
    // in ReportServiceImpl). Entrambe nullable per lo stesso motivo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_comment_id")
    private ReviewComment reviewComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy;

    // Snapshot del testo del bersaglio (recensione o risposta) al momento
    // della segnalazione. L'autore può modificarlo dopo — questo resta
    // com'era quando è stato segnalato, per confronto con la versione attuale
    @Column(name = "reported_text", columnDefinition = "TEXT")
    private String reportedText;
}