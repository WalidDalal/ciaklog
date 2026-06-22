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
        uniqueConstraints = @UniqueConstraint(columnNames = {"reporter_id", "review_id"})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /**
     * Admin che ha gestito la segnalazione. Contratto: resta {@code null} finché
     * {@link #status} è {@link ReportStatus#PENDING}; viene valorizzato dal
     * ReportService nel momento in cui un Admin risolve la segnalazione
     * (APPROVED, REJECTED o PAUSED). Non validato application.properties livello di entity/DB:
     * la coerenza va garantita nel Service.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy;
}