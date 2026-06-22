package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.UpdateCredentialsRequest;
import com.project.ciaklog.dto.response.UserProfileResponse;

public interface UserService {
    UserProfileResponse getPublicProfile(String username);
    void updateCredentials(String username, UpdateCredentialsRequest dto);
}
