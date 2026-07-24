package com.project.ciaklog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

// Storico delle sospensioni manuali da admin (non da segnalazione) — a
// differenza di User.suspensionReason, che tiene solo il motivo ATTUALE e
// viene azzerato alla riabilitazione, questa riga resta per sempre: serve
// perché anche dopo aver riabilitato l'utente, il drawer admin deve poter
// mostrare "sospensione manuale per X" nello storico violazioni, esattamente
// come già succede per le violazioni approvate da segnalazione.
@Entity
@Table(name = "manual_suspension_logs")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ManualSuspensionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(nullable = false, length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
