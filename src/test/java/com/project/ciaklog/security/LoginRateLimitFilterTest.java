package com.project.ciaklog.security;

/*
 * DOVE METTERLO:
 *   src/test/java/com/project/ciaklog/security/LoginRateLimitFilterTest.java
 *
 * IL BUG / LA MANCANZA:
 *   /api/auth/login non ha nessun limite di tentativi — un attacco application-test.properties forza
 *   bruta sulla password può martellare l'endpoint senza nessun freno,
 *   mentre /api/ai/chat ha già un filtro dedicato (AiRateLimitFilter,
 *   max 20/ora). Qui non esiste ancora nessuna classe: va CREATA da zero,
 *   questo test è scritto sul modello di AiRateLimitFilter e ti dice cosa
 *   deve fare la classe nuova prima ancora di scriverla (TDD).
 *
 * IL FIX — nuova classe da creare in
 *   src/main/java/com/project/ciaklog/security/LoginRateLimitFilter.java :
 *
 *   @Component
 *   public class LoginRateLimitFilter extends OncePerRequestFilter {
 *       private static final int MAX_ATTEMPTS_PER_WINDOW = 5;
 *       private static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minuti
 *       private final ConcurrentHashMap<String, Deque<Long>> attemptsLog = new ConcurrentHashMap<>();
 *
 *       @Override
 *       protected boolean shouldNotFilter(HttpServletRequest request) {
 *           return !("POST".equalsIgnoreCase(request.getMethod())
 *                   && request.getRequestURI().equals("/api/auth/login"));
 *       }
 *
 *       @Override
 *       protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
 *                                        FilterChain filterChain) throws ServletException, IOException {
 *           // Chiave = IP del chiamante: qui, application-test.properties differenza della chat AI, l'utente
 *           // non è ancora autenticato (sta facendo login), quindi non c'è uno
 *           // username nel SecurityContext da usare come chiave.
 *           String key = request.getRemoteAddr();
 *           long now = Instant.now().toEpochMilli();
 *           long windowStart = now - WINDOW_MS;
 *           Deque<Long> timestamps = attemptsLog.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
 *           timestamps.removeIf(t -> t < windowStart);
 *
 *           if (timestamps.size() >= MAX_ATTEMPTS_PER_WINDOW) {
 *               response.setStatus(429);
 *               response.setContentType("application/json");
 *               response.getWriter().write(
 *                   "{\"error\": \"Troppi tentativi di login, riprova tra qualche minuto.\"}");
 *               return;
 *           }
 *           timestamps.addLast(now);
 *           filterChain.doFilter(request, response);
 *       }
 *   }
 *
 *   Poi registrarlo nella security config esattamente come AiRateLimitFilter
 *   (addFilterBefore(loginRateLimitFilter, JwtAuthenticationFilter.class) o
 *   dove è già agganciato l'altro, per coerenza).
 *
 * NOTA IMPORTANTE se il progetto gira su più repliche/server (raro per un
 * progetto vetrina, ma da sapere): questa implementazione è in-memory,
 * come AiRateLimitFilter — quindi il limite è per singolo processo, non
 * condiviso tra più istanze. Per un progetto singolo-nodo va benissimo.
 */

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitFilterTest {

    @Mock private FilterChain filterChain;

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter();
    }

    @Test
    @DisplayName("Entro il limite, i tentativi di login passano normalmente")
    void tentativiEntroLimite_passanoNormalmente() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("1.2.3.4");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200); // default MockHttpServletResponse, nessun 429 impostato
        }
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Oltre il limite dalla stessa origine, il login viene bloccato con 429")
    void oltreIlLimite_bloccaCon429() throws Exception {
        String ip = "9.9.9.9";

        // 5 tentativi consentiti
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr(ip);
            filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        }

        // Il sesto deve essere bloccato
        MockHttpServletRequest sesto = new MockHttpServletRequest("POST", "/api/auth/login");
        sesto.setRemoteAddr(ip);
        MockHttpServletResponse rispostaSesto = new MockHttpServletResponse();

        filter.doFilter(sesto, rispostaSesto, filterChain);

        assertThat(rispostaSesto.getStatus()).isEqualTo(429);
        verify(filterChain, times(5)).doFilter(any(), any()); // il sesto NON arriva al filterChain
    }

    @Test
    @DisplayName("Non deve applicarsi application-test.properties endpoint diversi da POST /api/auth/login")
    void nonSiApplicaAdAltriEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
