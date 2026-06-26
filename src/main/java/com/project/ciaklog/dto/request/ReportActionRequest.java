package com.project.ciaklog.dto.request;

import com.project.ciaklog.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportActionRequest {

    @NotNull(message = "L'azione è obbligatoria")
    private ReportStatus action;
}