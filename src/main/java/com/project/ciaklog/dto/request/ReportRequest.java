package com.project.ciaklog.dto.request;

import com.project.ciaklog.entity.ReportReasonCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {

    @NotNull(message = "reviewId obbligatorio")
    private java.util.UUID reviewId;

    @NotNull(message = "reasonCategory obbligatoria")
    private ReportReasonCategory reasonCategory;

    @Size(max = 500, message = "Testo massimo 500 caratteri")
    private String reasonText; // obbligatorio se reasonCategory = OTHER — validato nel Service
}