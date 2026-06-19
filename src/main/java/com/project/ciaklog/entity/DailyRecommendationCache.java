package com.project.ciaklog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "daily_recommendation_cache")
public class DailyRecommendationCache {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "suggestions_json", nullable = false, columnDefinition = "TEXT")
    private String suggestionsJson;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Garantisce che generatedAt non sia mai null al primo salvataggio,
    // anche se il Service dimentica di impostarlo esplicitamente.
    @PrePersist
    private void prePersist() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}