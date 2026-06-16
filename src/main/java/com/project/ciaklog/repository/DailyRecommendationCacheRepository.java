package com.project.ciaklog.repository;

import com.project.ciaklog.entity.DailyRecommendationCache;
import com.project.ciaklog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyRecommendationCacheRepository extends JpaRepository<DailyRecommendationCache, UUID> {

    // Per recuperare la cache giornaliera di un utente
    Optional<DailyRecommendationCache> findByUser(User user);
}