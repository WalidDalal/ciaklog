package com.project.ciaklog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username obbligatorio")
    @Size(min = 3, max = 30, message = "Username tra 3 e 30 caratteri")
    private String username;

    @NotBlank(message = "Email obbligatoria")
    @Email(message = "Email non valida")
    private String email;

    @NotBlank(message = "Password obbligatoria")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "La password deve contenere almeno 8 caratteri, una maiuscola, una minuscola e un numero"
    )
    private String password;
}