package com.project.ciaklog.dto.request;

import com.project.ciaklog.entity.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReactionRequest {

    @NotNull(message = "type obbligatorio")
    private ReactionType type;
}
