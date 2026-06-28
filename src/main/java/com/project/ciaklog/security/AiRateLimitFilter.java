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

/**
 * Rate limiter per POST /api/ai/chat — max 20 richieste per utente per ora.
 * Implementazione in-memory con sliding window: tiene traccia dei timestamp
 * delle ultime richieste per ogni username e scarta quelle più vecchie di 1h.
 * Nessuna dipendenza esterna — adeguato per un singolo nodo.
 */
@Component
public class AiRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_HOUR = 20;
    private static final long WINDOW_MS = 60 * 60 * 1000L; // 1 ora in ms

    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Applica solo a POST /api/ai/chat
        return !("POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().equals("/api/ai/chat"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String username = extractUsername(request);

        if (username == null) {
            // Non autenticato — la security chain gestirà il 401
            filterChain.doFilter(request, response);
            return;
        }

        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW_MS;

        Deque<Long> timestamps = requestLog.computeIfAbsent(username, k -> new ConcurrentLinkedDeque<>());

        // Rimuove i timestamp fuori dalla finestra
        timestamps.removeIf(t -> t < windowStart);

        if (timestamps.size() >= MAX_REQUESTS_PER_HOUR) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Limite richieste AI raggiunto (max " + MAX_REQUESTS_PER_HOUR
                            + " richieste/ora). Riprova più tardi.\"}");
            return;
        }

        timestamps.addLast(now);
        filterChain.doFilter(request, response);
    }

    private String extractUsername(HttpServletRequest request) {
        // Lo username è già nel SecurityContext dopo JwtAuthenticationFilter
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return auth.getName();
    }
}