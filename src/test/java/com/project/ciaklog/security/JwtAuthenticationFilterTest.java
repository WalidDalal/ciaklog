package com.project.ciaklog.security;

/*
 * DOVE METTERLO:
 *   src/test/java/com/project/ciaklog/security/JwtAuthenticationFilterTest.java
 *
 * IL BUG:
 *   Se il token è valido ma l'utente non è più risolvibile (username
 *   cambiato nel frattempo su un altro dispositivo, account eliminato),
 *   userDetailsService.loadUserByUsername(username) lancia
 *   UsernameNotFoundException. Oggi in JwtAuthenticationFilter questa
 *   eccezione non viene catturata: risale fuori dal filtro e il chiamante
 *   riceve un errore generico (whitelabel/500) invece di un 401 pulito in
 *   JSON — e il GlobalExceptionHandler non interviene perché gira solo a
 *   livello controller, non a livello filtro.
 *
 * IL FIX (in JwtAuthenticationFilter.doFilterInternal):
 *   Circondare la riga
 *       UserDetails userDetails = userDetailsService.loadUserByUsername(username);
 *   con un try/catch:
 *
 *   UserDetails userDetails;
 *   try {
 *       userDetails = userDetailsService.loadUserByUsername(username);
 *   } catch (UsernameNotFoundException ex) {
 *       log.warn("Token valido ma utente non più trovabile: {}", username);
 *       response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *       response.setContentType("application/json");
 *       response.getWriter().write("{\"error\": \"Sessione non più valida, effettua di nuovo il login\"}");
 *       return;
 *   }
 *
 *   (l'import di UsernameNotFoundException c'è già nel file, viene da
 *   org.springframework.security.core.userdetails)
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Token valido ma utente non più risolvibile → 401 pulito, non un errore non gestito")
    void tokenValidoUtenteNonRisolvibile_rispondeQuattrocentoUno() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reviews/user/mario");
        request.addHeader("Authorization", "Bearer un-token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.isTokenValid("un-token-valido")).thenReturn(true);
        when(jwtService.extractUsername("un-token-valido")).thenReturn("utente_cambiato_o_eliminato");
        when(userDetailsService.loadUserByUsername("utente_cambiato_o_eliminato"))
                .thenThrow(new UsernameNotFoundException("non trovato"));

        // Non deve lanciare l'eccezione fuori dal filtro
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Sessione non più valida");
        // La richiesta non deve proseguire verso il controller in questo caso
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Caso normale: token valido e utente trovabile continua come prima")
    void tokenValidoUtenteTrovabile_procedeNormalmente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reviews/user/mario");
        request.addHeader("Authorization", "Bearer un-token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        "mario", "hash", java.util.List.of());

        when(jwtService.isTokenValid("un-token-valido")).thenReturn(true);
        when(jwtService.extractUsername("un-token-valido")).thenReturn("mario");
        when(userDetailsService.loadUserByUsername("mario")).thenReturn(userDetails);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
