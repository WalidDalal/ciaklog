package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.UpdateCredentialsRequest;
import com.project.ciaklog.dto.response.AuthResponse;
import com.project.ciaklog.dto.response.UserProfileResponse;

public interface UserService {
    UserProfileResponse getPublicProfile(String username, boolean canViewHidden);

    // Restituisce un nuovo AuthResponse (con token aggiornato) solo se è cambiato l'username,
    // null se è cambiata solo la password (il token rimane valido)
    AuthResponse updateCredentials(String username, UpdateCredentialsRequest dto);

    void deleteAccount(String username, String password);
}