package com.project.ciaklog.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCredentialsRequest {

    @Size(min = 3, max = 30, message = "Username tra 3 e 30 caratteri")
    private String username; // null = non modificare

    @Size(max = 200, message = "Bio massimo 200 caratteri")
    private String bio; // null = non modificare, "" = azzera

    private String profileColor; // null = non modificare, "" = torna al colore automatico

    private String currentPassword;

    // Fix (disallineamento con la registrazione): questo campo non aveva
    // NESSUN controllo di robustezza — RegisterRequest.password richiede lo
    // stesso pattern (8+ caratteri, maiuscola, minuscola, numero), ma qui si
    // poteva impostare una nuova password di 1 carattere senza che nessuna
    // validazione lo impedisse (UserServiceImpl.updateCredentials la cifra e
    // basta, nessun controllo aggiuntivo lato service). Stesso @Pattern di
    // RegisterRequest — Jakarta Validation non applica @Pattern a valori
    // null, quindi resta valido lasciare il campo vuoto quando non si vuole
    // cambiare la password.
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "La password deve contenere almeno 8 caratteri, una maiuscola, una minuscola e un numero"
    )
    private String newPassword;
}