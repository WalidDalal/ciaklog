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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    @Column(nullable = false)
    private String title;

    private String posterPath;

    private Integer releaseYear;

    // CSV di generi, risolti dai genre id TMDB al momento dell'aggiunta
    private String genres;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WatchStatus status;

    // Solo se mediaType = TV e status = WATCHING; null per i film
    private Integer currentSeason;

    private LocalDate watchedDate;

    // Aggiornato manualmente dal Service ad ogni rigenerazione (non @CreationTimestamp)
    @Column(nullable = false)
    private LocalDateTime lastStatusUpdate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}