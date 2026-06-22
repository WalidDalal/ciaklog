package com.project.ciaklog.service.impl;

import com.project.ciaklog.dto.request.LoginRequest;
import com.project.ciaklog.dto.request.RegisterRequest;
import com.project.ciaklog.dto.response.AuthResponse;
import com.project.ciaklog.entity.Role;
import com.project.ciaklog.entity.User;
import com.project.ciaklog.entity.UserStatus;
import com.project.ciaklog.exception.DuplicateResourceException;
import com.project.ciaklog.exception.ForbiddenException;
import com.project.ciaklog.exception.UnauthorizedException;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.service.AuthService;
import com.project.ciaklog.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email già registrata");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("Username non disponibile");
        }

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Credenziali non valide"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenziali non valide");
        }

        if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.PERMANENTLY_SUSPENDED) {
            throw new ForbiddenException("Account sospeso, contatta l'amministratore");
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}
