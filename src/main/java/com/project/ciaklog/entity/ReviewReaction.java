package com.project.ciaklog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

// Reazione emoji su una recensione o una risposta (esattamente un bersaglio,
// stesso pattern dual-target di Report). NESSUNA moderazione — un'emoji non
// porta contenuto dannoso (deciso). Un utente ha una sola reazione per
// elemento: sceglierne un'altra sostituisce la precedente (upsert), non la
// duplica.
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "review_reactions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "review_id"}),
                @UniqueConstraint(columnNames = {"user_id", "review_comment_id"})
        },
        indexes = {
                @Index(name = "idx_reaction_review_id", columnList = "review_id"),
                @Index(name = "idx_reaction_review_comment_id", columnList = "review_comment_id")
        }
)
public class ReviewReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_comment_id")
    private ReviewComment reviewComment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
