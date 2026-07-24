package com.project.ciaklog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

// A differenza di /api/ai/chat, che ha
// già AiRateLimitFilter dedicato, /api/auth/login non aveva nessun limite
// di tentativi — utile contro un attacco a forza bruta sulla password.
// Chiave = IP del chiamante: qui, a differenza della chat AI, l'utente non
// è ancora autenticato (sta facendo login), quindi non c'è uno username nel
// SecurityContext da usare come chiave.
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS_PER_WINDOW = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minuti

    private final ConcurrentHashMap<String, Deque<Long>> attemptsLog = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().equals("/api/auth/login"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getRemoteAddr();
        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW_MS;

        Deque<Long> timestamps = attemptsLog.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        timestamps.removeIf(t -> t < windowStart);

        if (timestamps.size() >= MAX_ATTEMPTS_PER_WINDOW) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Troppi tentativi di login, riprova tra qualche minuto.\"}");
            return;
        }

        timestamps.addLast(now);
        filterChain.doFilter(request, response);
    }
}