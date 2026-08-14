package com.project.ciaklog.entity;

public enum ReportStatus {
    PENDING,
    APPROVED,
    REJECTED,
    // Fix (dashboard admin — segnalazioni su contenuti di account spariti):
    // quando l'autore del contenuto segnalato diventa DELETED o
    // PERMANENTLY_SUSPENDED, il contenuto è già nascosto per sempre
    // (irreversibile) — nessuna decisione dell'Admin su queste segnalazioni
    // PENDING cambierebbe qualcosa nella pratica. Invece di lasciarle in
    // "In attesa" a fare rumore, o di forzare un APPROVED/REJECTED che
    // nessuno ha realmente deciso, passano qui automaticamente. Restano
    // consultabili (storico), semplicemente fuori dalla coda da lavorare.
    // NON si applica quando è il SEGNALANTE (non l'autore del contenuto) a
    // sparire — quel contenuto può appartenere a qualcun altro ancora
    // attivo e restare da moderare normalmente (vedi reporterAccountUnavailable).
    ARCHIVED
}