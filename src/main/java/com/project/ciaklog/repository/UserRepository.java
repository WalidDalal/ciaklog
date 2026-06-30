package com.project.ciaklog.repository;

import com.project.ciaklog.entity.Role;
import com.project.ciaklog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // Top 5 utenti per score — esclude gli admin, evita findAll() + N query
    List<User> findTop5ByRoleNotOrderByScoreDesc(Role role);
}