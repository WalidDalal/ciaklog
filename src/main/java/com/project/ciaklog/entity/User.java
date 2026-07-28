package com.project.ciaklog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(length = 200)
    private String bio;

    // Colore avatar scelto manualmente dall'utente in Impostazioni (uno dei
    // valori della palette AVATAR_COLORS lato frontend). Null = nessuna scelta
    // esplicita, si usa il colore automatico derivato dallo username.
    @Column(name = "profile_color", length = 30)
    private String profileColor;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private int violationCount = 0;

    // Motivo dell'ultima sospensione manuale (da admin, non da segnalazione
    // approvata) — prima veniva raccolto dal form ma mai salvato: il drawer
    // admin mostrava un violationCount incrementato senza nessun dettaglio
    // ad accompagnarlo. Azzerato alla riabilitazione.
    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

    /**
     * Punteggio classifica — aggiornato in modo persistito ad ogni evento:
     * +10 pubblica recensione
     * -15 tua recensione rimossa da segnalazione
     * -20 sospensione temporanea
     *  0  azzerato in caso di sospensione permanente
     * L'admin non accumula punti (Role.ADMIN).
     */
    @Column(nullable = false)
    @Builder.Default
    private int score = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}