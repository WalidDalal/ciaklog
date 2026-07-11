package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private Role role;
}