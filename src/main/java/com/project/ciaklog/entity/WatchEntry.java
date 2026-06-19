package com.project.ciaklog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "watch_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tmdb_id", "media_type"})
)
public class WatchEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Column(name = "media_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContentType mediaType;

    @Column(nullable = false)
    private String title;

    private String posterPath;

    // CSV di generi, risolti dai genre id TMDB al momento dell'aggiunta
    private String genres;

    private Integer releaseYear;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WatchStatus status;

    // Solo se mediaType = TV e status = WATCHING; null per i film
    private Integer currentSeason;

    private LocalDate watchedDate;

    @Column(nullable = false)
    private LocalDateTime lastStatusUpdate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Garantisce che lastStatusUpdate non sia mai null al primo salvataggio,
    // anche se il Service dimentica di impostarlo esplicitamente.
    @PrePersist
    private void prePersist() {
        if (lastStatusUpdate == null) {
            lastStatusUpdate = LocalDateTime.now();
        }
    }
}