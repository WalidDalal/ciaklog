package com.project.ciaklog.repository;

import com.project.ciaklog.entity.AiRecommendationLog;
import com.project.ciaklog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiRecommendationLogRepository extends JpaRepository<AiRecommendationLog, UUID> {

    // Per recuperare lo storico sessione AI di un utente (memoria conversazione)
    Optional<AiRecommendationLog> findByUserAndSessionId(User user, String sessionId);

    // Per eventuali pulizie o debug
    List<AiRecommendationLog> findByUser(User user);
}