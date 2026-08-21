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

    // Stesso @Pattern di RegisterRequest — non applicato a valori null,
    // quindi resta valido lasciare il campo vuoto quando non si vuole
    // cambiare la password.
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "La password deve contenere almeno 8 caratteri, una maiuscola, una minuscola e un numero"
    )
    private String newPassword;
}