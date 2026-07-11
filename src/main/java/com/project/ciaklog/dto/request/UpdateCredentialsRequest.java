package com.project.ciaklog.dto.request;

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

    private String currentPassword;

    private String newPassword;
}