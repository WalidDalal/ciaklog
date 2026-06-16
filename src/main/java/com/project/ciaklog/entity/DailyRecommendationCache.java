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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "suggestions_json", nullable = false, columnDefinition = "TEXT")
    private String suggestionsJson;

    // Aggiornato manualmente dal Service ad ogni rigenerazione (non @CreationTimestamp)
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}