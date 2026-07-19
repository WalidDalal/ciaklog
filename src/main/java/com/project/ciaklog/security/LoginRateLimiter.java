package com.project.ciaklog.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Fix (Logica Moderazione — trovato in revisione): il login non aveva nessun
// rate-limit, quindi era possibile tentare password a raffica senza limiti
// (brute-force). Implementazione semplice in memoria (niente Redis/infra
// esterna richiesta) — accettabile per la scala attuale dell'app; se in
// futuro il backend gira su più istanze, questo contatore andrebbe spostato
// su uno store condiviso (es. Redis), perché ogni istanza avrebbe il suo.
//
// Regola: max 5 tentativi falliti per email in una finestra di 15 minuti,
// poi blocco fino a fine finestra. Un login riuscito azzera il contatore.
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000;

    private record Attempts(AtomicInteger count, long windowStart) {}

    private final ConcurrentHashMap<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        Attempts a = attemptsByEmail.get(normalizedKey(email));
        if (a == null) return false;
        if (Instant.now().toEpochMilli() - a.windowStart() > WINDOW_MILLIS) {
            attemptsByEmail.remove(normalizedKey(email));
            return false;
        }
        return a.count().get() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String email) {
        attemptsByEmail.compute(normalizedKey(email), (k, existing) -> {
            long now = Instant.now().toEpochMilli();
            if (existing == null || now - existing.windowStart() > WINDOW_MILLIS) {
                return new Attempts(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(normalizedKey(email));
    }

    private String normalizedKey(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
