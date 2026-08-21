package com.project.ciaklog.entity;

public enum ReportStatus {
    PENDING,
    APPROVED,
    REJECTED,
    // Quando l'autore del contenuto segnalato diventa DELETED o
    // PERMANENTLY_SUSPENDED, il contenuto è già nascosto per sempre —
    // le segnalazioni PENDING passano qui invece di restare in coda.
    // Non si applica quando è il segnalante (non l'autore) a sparire.
    ARCHIVED
}
