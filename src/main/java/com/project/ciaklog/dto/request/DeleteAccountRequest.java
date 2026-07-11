package com.project.ciaklog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Fix: l'eliminazione account era protetta solo dal modal "sei sicuro?" —
// azione irreversibile, ora richiede anche la password come secondo fattore
// per evitare cancellazioni da click accidentale.
@Getter
@Setter
public class DeleteAccountRequest {

    @NotBlank(message = "Password obbligatoria per confermare l'eliminazione")
    private String password;
}
