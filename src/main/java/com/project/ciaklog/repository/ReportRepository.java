package com.project.ciaklog.repository;

import com.project.ciaklog.entity.Report;
import com.project.ciaklog.entity.ReportStatus;
import com.project.ciaklog.entity.Review;
import com.project.ciaklog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    // Dashboard Admin — lista segnalazioni per stato, paginata
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    // Dashboard Admin — tutte le segnalazioni, paginata
    Page<Report> findAll(Pageable pageable);

    // Per verificare segnalazione duplicata (UC10/FA2 — 409)
    boolean existsByReporterAndReview(User reporter, Review review);

    // Per trovare tutte le segnalazioni su una review (utile per debug/storico)
    List<Report> findByReview(Review review);
}