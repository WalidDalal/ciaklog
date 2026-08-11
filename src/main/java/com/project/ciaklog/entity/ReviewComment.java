package com.project.ciaklog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

// Risposta ad una recensione. Stesso schema di stato di Review (VISIBLE/HIDDEN/REMOVED)
// per riusare la stessa logica di moderazione (report, auto-hide, admin) — vedi Step 2.
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "review_comments",
        indexes = {
            @Index(name = "idx_comment_review_status", columnList = "review_id, status"),
            @Index(name = "idx_comment_author", columnList = "author_id")
        }
)
public class ReviewComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Obbligatorio (validato con @NotBlank in ReviewCommentRequest)
    @Column(nullable = false, length = 500)
    private String text;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.VISIBLE;

    // Campo separato da `status`,
    // stesso principio di Review — non è una violazione, non tocca il
    // punteggio, non finisce nella coda admin, reversibile dall'autore
    @Column(name = "hidden_by_author", nullable = false)
    @Builder.Default
    private boolean hiddenByAuthor = false;

    // Stesso principio di Review.hiddenBySuspension — flag indipendente,
    // così la riabilitazione non ri-mostra anche ciò che l'utente aveva
    // nascosto lui stesso prima di essere sospeso
    @Column(name = "hidden_by_suspension", nullable = false)
    @Builder.Default
    private boolean hiddenBySuspension = false;

    // Stesso principio di Review.hiddenByDeletion — irreversibile, mai
    // riportato a false (l'eliminazione dell'account non si annulla)
    @Column(name = "hidden_by_deletion", nullable = false)
    @Builder.Default
    private boolean hiddenByDeletion = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
}
