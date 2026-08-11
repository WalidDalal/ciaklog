package com.project.ciaklog.entity;

public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    PERMANENTLY_SUSPENDED,
    // Fix (dashboard admin — recensioni di utenti eliminati): deleteAccount()
    // anonimizzava già username/email/password ("soft delete"), ma lo status
    // restava ACTIVE — nessun modo esplicito di riconoscere un account
    // eliminato. Aggiunto per coerenza con SUSPENDED/PERMANENTLY_SUSPENDED,
    // anche se qui non serve alcun "reinstateUser": è irreversibile.
    DELETED,
}