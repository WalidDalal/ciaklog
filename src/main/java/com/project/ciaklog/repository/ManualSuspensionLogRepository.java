package com.project.ciaklog.repository;

import com.project.ciaklog.entity.ManualSuspensionLog;
import com.project.ciaklog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManualSuspensionLogRepository extends JpaRepository<ManualSuspensionLog, java.util.UUID> {

    // JOIN FETCH admin — evita LazyInitializationException quando si legge
    // il nome dell'admin che ha sospeso, fuori dalla sessione Hibernate
    @Query("SELECT s FROM ManualSuspensionLog s JOIN FETCH s.user LEFT JOIN FETCH s.admin WHERE s.user = :user ORDER BY s.createdAt DESC")
    List<ManualSuspensionLog> findAllByUser(@Param("user") User user);
}
