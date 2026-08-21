package com.project.ciaklog.entity;

public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    PERMANENTLY_SUSPENDED,
    // deleteAccount() anonimizza username/email/password ma prima lo
    // status restava ACTIVE — irreversibile, nessun reinstateUser
    DELETED,
}
