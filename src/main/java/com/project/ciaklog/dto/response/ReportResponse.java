package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.entity.ReportReasonCategory;
import com.project.ciaklog.entity.ReportStatus;
import com.project.ciaklog.entity.ReportTargetType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReportResponse {
    private UUID id;

    // Fix (moderazione risposte): distingue se il bersaglio è una recensione
    // o una risposta — la dashboard admin usa questo per non essere ambigua
    private ReportTargetType targetType;

    // Fix (dashboard admin, Step 6): per il link "Vedi nel contesto" — valorizzati
    // sempre, sia per REVIEW che per COMMENT (per il commento è del film/serie
    // a cui appartiene la sua recensione madre, non della recensione stessa)
    private Long tmdbId;
    private ContentType contentType;

    // Popolati quando targetType = REVIEW
    private UUID reviewId;
    private String reviewAuthorUsername;
    private String reviewText;
    private Integer reviewRating;

    // Popolati quando targetType = COMMENT
    private UUID reviewCommentId;
    private UUID parentReviewId; // per navigare/raggruppare sotto la recensione madre
    private String commentAuthorUsername;
    private String commentText;

    private String reporterUsername;
    private ReportReasonCategory reasonCategory;
    private String reasonText;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolvedByUsername;

    // Fix (dashboard admin): quando la segnalazione è stata approvata, il
    // contenuto è stato rimosso e "Vedi nel contesto" punta a qualcosa che
    // non esiste più — il frontend usa questo flag per nascondere il link
    // invece di mostrarlo comunque e farlo fallire in modo silenzioso
    private boolean targetRemoved;

    // Testo del bersaglio al momento della segnalazione — reviewText/commentText
    // sopra restano quelli ATTUALI (l'autore può averli modificati dopo).
    // targetEdited indica se le due versioni differiscono, per evidenziarlo in UI
    private String reportedText;
    private boolean targetEdited;
}