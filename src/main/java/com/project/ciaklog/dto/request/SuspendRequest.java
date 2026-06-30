package com.project.ciaklog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SuspendRequest {

    @NotBlank(message = "Il motivo della sospensione è obbligatorio")
    private String reason;
}