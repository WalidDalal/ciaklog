package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.LoginRequest;
import com.project.ciaklog.dto.request.RegisterRequest;
import com.project.ciaklog.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest dto);
    AuthResponse login(LoginRequest dto);
}
