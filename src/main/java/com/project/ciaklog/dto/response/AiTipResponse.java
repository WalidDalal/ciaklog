package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiTipResponse {
    // Stringa vuota se l'AI non ha risposto — il frontend mostra il
    // messaggio statico di fallback in quel caso, mai un errore
    private String tip;
}
