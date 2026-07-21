package com.project.ciaklog.repository;

import com.project.ciaklog.entity.Role;
import com.project.ciaklog.entity.User;
import com.project.ciaklog.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Verifica username duplicato escludendo l'utente corrente — usato in updateCredentials
    boolean existsByUsernameAndIdNot(String username, UUID id);

    // Top 5 utenti per score — esclude gli admin
    List<User> findTop5ByRoleNotOrderByScoreDesc(Role role);

    // Fix: la ricerca utenti nella dashboard admin non era mai collegata
    // al backend — il controller riceveva `search` ma non lo passava al
    // service. Cerca su username O email, case-insensitive
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email, Pageable pageable);
    // Fix (Home Admin, deciso): utenti in stato SUSPENDED (2ª violazione,
    // sospensione temporanea) — candidati a riammissione manuale via reinstateUser()
    long countByStatus(UserStatus status);
}