package com.project.ciaklog.security;

import com.project.ciaklog.entity.User;

public interface JwtService {
    String generateToken(User user);
    String extractUsername(String token);
    String extractRole(String token);
    boolean isTokenValid(String token);
}
