package com.project.ciaklog.service.impl;

/*
 * DOVE METTERLO:
 *   src/test/java/com/project/ciaklog/service/impl/WatchEntryServiceImplTest.java
 *
 * TIPO DI TEST: unit test con Mockito (nessun database vero) — qui il bug è
 * nella LOGICA del service (manca un controllo), non in una query, quindi
 * mockare i repository va benissimo: possiamo decidere noi cosa "risponde"
 * il database finto e verificare solo il comportamento del metodo.
 *
 * COSA DEVI CAMBIARE PERCHÉ IL TEST "nonDeveRimuovere..." PASSI (il fix):
 *   In WatchEntryServiceImpl.removeFromLibrary(...), prima di
 *   "watchEntryRepository.delete(entry)", aggiungere lo stesso controllo già
 *   presente in updateStatus(...):
 *
 *   boolean hasActiveReview = reviewRepository.existsByUserAndTmdbIdAndContentTypeAndStatusNot(
 *           user, entry.getTmdbId(), entry.getContentType(), ReviewStatus.REMOVED);
 *   if (hasActiveReview) {
 *       throw new BusinessRuleException(
 *               "Hai già recensito questo contenuto — elimina la recensione prima di rimuoverlo dalla libreria");
 *   }
 *
 * Il secondo test ("deveRimuovere...") verifica il caso normale (nessuna
 * recensione attiva) e deve continuare a funzionare esattamente come oggi
 * — serve a dimostrare che il fix non rompe il caso normale.
 */

import com.project.ciaklog.entity.*;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import com.project.ciaklog.repository.WatchEntryRepository;
import com.project.ciaklog.service.TmdbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchEntryServiceImplTest {

    @Mock private WatchEntryRepository watchEntryRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private TmdbService tmdbService;

    private WatchEntryServiceImpl service;

    private User utente;
    private WatchEntry voceLibreria;

    @BeforeEach
    void setUp() {
        service = new WatchEntryServiceImpl(watchEntryRepository, userRepository, reviewRepository, tmdbService);

        utente = User.builder()
                .id(UUID.randomUUID())
                .username("mario")
                .email("mario@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        voceLibreria = WatchEntry.builder()
                .id(UUID.randomUUID())
                .user(utente)
                .tmdbId(42L)
                .contentType(ContentType.MOVIE)
                .title("Un film qualsiasi")
                .status(WatchStatus.WATCHED)
                .lastStatusUpdate(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("mario")).thenReturn(Optional.of(utente));
        when(watchEntryRepository.findById(voceLibreria.getId())).thenReturn(Optional.of(voceLibreria));
    }

    @Test
    @DisplayName("NON deve permettere di rimuovere dalla libreria un titolo con una recensione ancora attiva")
    void nonDeveRimuovereSeEsisteRecensioneAttiva() {
        when(reviewRepository.existsByUserAndTmdbIdAndContentTypeAndStatusNot(
                utente, voceLibreria.getTmdbId(), voceLibreria.getContentType(), ReviewStatus.REMOVED))
                .thenReturn(true);

        assertThatThrownBy(() -> service.removeFromLibrary("mario", voceLibreria.getId()))
                .isInstanceOf(BusinessRuleException.class);

        // La verifica più importante: il delete non deve MAI essere chiamato
        // se c'è una recensione attiva collegata, altrimenti resta orfana.
        verify(watchEntryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve continuare a rimuovere normalmente un titolo senza nessuna recensione attiva")
    void deveRimuovereSeNessunaRecensioneAttiva() {
        when(reviewRepository.existsByUserAndTmdbIdAndContentTypeAndStatusNot(
                utente, voceLibreria.getTmdbId(), voceLibreria.getContentType(), ReviewStatus.REMOVED))
                .thenReturn(false);

        service.removeFromLibrary("mario", voceLibreria.getId());

        verify(watchEntryRepository, times(1)).delete(voceLibreria);
    }
}
