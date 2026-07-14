package com.project.ciaklog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "tmdb_id", "content_type"})
        },
        indexes = {
            @Index(name = "idx_review_tmdb_type_status", columnList = "tmdb_id, content_type, status"),
            @Index(name = "idx_review_user_status", columnList = "user_id, status")
        }
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    // Vincolo 1-5 validato nel ReviewRequest (@Min/@Max), non qui — coerente
    // con la scelta di tenere le validazioni sui DTO e non sulle Entity.
    @Column(nullable = false)
    private Integer rating;

    // Obbligatorio (validato con @NotBlank in ReviewRequest)
    @Column(nullable = false, length = 1000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.VISIBLE;

    // Fix (auto-nascondimento autore, deciso): campo separato da `status` di
    // proposito — nascondere una propria recensione non è una violazione,
    // non tocca il punteggio/violationCount, e non deve MAI finire nella coda
    // di moderazione admin (che legge solo i Report, non questo campo).
    // Reversibile dall'autore in qualsiasi momento, a differenza di REMOVED.
    @Column(name = "hidden_by_author", nullable = false)
    @Builder.Default
    private boolean hiddenByAuthor = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}