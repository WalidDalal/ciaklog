package com.project.ciaklog.entity;

// Fix (moderazione risposte): distingue se una segnalazione punta a una
// Review o a una ReviewComment — usato nella dashboard admin per non avere
// un elenco unico ambiguo dove non si capisce cosa si sta esaminando.
public enum ReportTargetType {
    REVIEW,
    COMMENT
}
