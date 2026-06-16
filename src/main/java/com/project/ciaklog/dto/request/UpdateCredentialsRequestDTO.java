// UpdateCredentialsRequestDTO.java
package com.project.ciaklog.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCredentialsRequestDTO {

    @Size(min = 3, max = 30, message = "Username tra 3 e 30 caratteri")
    private String username; // nullable — se null non si aggiorna

    @Size(min = 8, message = "Password minimo 8 caratteri")
    private String newPassword; // nullable — se null non si aggiorna

    private String currentPassword; // obbligatorio se newPassword non è null — validato nel Service
}