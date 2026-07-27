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

    // Fix (🟡 soglia identica a quella per email): questo filtro (per IP) aveva
    // la STESSA soglia stretta di LoginRateLimiter (per email, dentro
    // AuthServiceImpl) — 5 tentativi/15min. In locale, dove l'IP è sempre lo
    // stesso, i due limiti scattano insieme e bloccano TUTTO da quell'indirizzo,
    // inclusi login corretti su account diversi. Le due soglie hanno scopi
    // diversi: quella per email deve restare stretta (blocca solo l'account
    // preso di mira); questa per IP serve a beccare scanning/credential-stuffing
    // su MOLTI account dallo stesso indirizzo, quindi va larga — alzata a
    // 40 tentativi/ora (non 5/15min), pensata per non scattare durante un uso
    // normale (anche condiviso, es. NAT/ufficio) ma comunque intercettare un
    // attacco vero.
    private static final int MAX_ATTEMPTS_PER_WINDOW = 40;
    private static final long WINDOW_MS = 60 * 60 * 1000L; // 1 ora

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

        filterChain.doFilter(request, response);

        // Fix: prima si registrava un "tentativo" per OGNI richiesta, anche i login
        // riusciti — bastava fare login/logout ripetuti con credenziali corrette per
        // finire bloccati. Ora conta solo le risposte 401 (credenziali sbagliate),
        // coerente con LoginRateLimiter che già registra solo i fallimenti.
        if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            timestamps.addLast(now);
        }
    }
}