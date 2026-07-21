package com.project.ciaklog.repository;

/*
 * DOVE METTERLO:
 *   src/test/java/com/project/ciaklog/repository/ReviewRepositoryTest.java
 *   (sostituisce la versione precedente — findByUser ora ha 5 parametri
 *   invece di 4, per via del nuovo parametro isAdmin)
 *
 * COSA COPRE ORA:
 *   1) Un visitatore anonimo NON vede REMOVED, HIDDEN, né le nascoste dall'autore
 *   2) Un altro utente loggato: stesso comportamento del visitatore anonimo
 *   3) L'autore stesso vede anche quella che ha nascosto lui (ma non la REMOVED)
 *   4) Un ADMIN vede anche quella HIDDEN (in attesa di decisione) e quella
 *      nascosta dall'autore — ma NON quella REMOVED (già risolta/rimossa,
 *      non deve comparire nella lista normale nemmeno per l'admin)
 */

import com.project.ciaklog.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    private User autore;
    private User altroUtente;

    @BeforeEach
    void setUp() {
        autore = userRepository.save(User.builder()
                .username("autore_test")
                .email("autore_test@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build());

        altroUtente = userRepository.save(User.builder()
                .username("altro_utente_test")
                .email("altro_test@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build());

        // 1 recensione normale, visibile application-test.properties tutti
        reviewRepository.save(Review.builder()
                .user(autore)
                .tmdbId(1L)
                .contentType(ContentType.MOVIE)
                .rating(4)
                .text("Recensione visibile normale")
                .status(ReviewStatus.VISIBLE)
                .hiddenByAuthor(false)
                .build());

        // 1 recensione rimossa per violazione (moderazione conclusa)
        reviewRepository.save(Review.builder()
                .user(autore)
                .tmdbId(2L)
                .contentType(ContentType.MOVIE)
                .rating(2)
                .text("Recensione rimossa per violazione")
                .status(ReviewStatus.REMOVED)
                .hiddenByAuthor(false)
                .build());

        // 1 recensione che l'autore ha nascosto volontariamente
        reviewRepository.save(Review.builder()
                .user(autore)
                .tmdbId(3L)
                .contentType(ContentType.TV)
                .rating(5)
                .text("Recensione nascosta dall'autore")
                .status(ReviewStatus.VISIBLE)
                .hiddenByAuthor(true)
                .build());

        // 1 recensione HIDDEN (2+ segnalazioni, in attesa di decisione admin)
        reviewRepository.save(Review.builder()
                .user(autore)
                .tmdbId(4L)
                .contentType(ContentType.MOVIE)
                .rating(1)
                .text("Recensione in attesa di moderazione")
                .status(ReviewStatus.HIDDEN)
                .hiddenByAuthor(false)
                .build());
    }

    @Test
    void findByUser_visitatoreAnonimo_vedeSoloLaVisibileNormale() {
        var risultato = reviewRepository.findByUser(
                autore, ReviewStatus.VISIBLE, /* viewerUsername */ null, /* isAdmin */ false, PageRequest.of(0, 10));

        List<Review> contenuto = risultato.getContent();

        assertThat(contenuto).hasSize(1);
        assertThat(contenuto.get(0).getText()).isEqualTo("Recensione visibile normale");
    }

    @Test
    void findByUser_altroUtenteLoggato_vedeSoloLaVisibileNormale() {
        var risultato = reviewRepository.findByUser(
                autore, ReviewStatus.VISIBLE, altroUtente.getUsername(), false, PageRequest.of(0, 10));

        List<Review> contenuto = risultato.getContent();

        assertThat(contenuto).hasSize(1);
        assertThat(contenuto.get(0).getText()).isEqualTo("Recensione visibile normale");
    }

    @Test
    void findByUser_autoreStesso_vedeAncheQuellaNascostaDaSe_maNonLaRemoved() {
        var risultato = reviewRepository.findByUser(
                autore, ReviewStatus.VISIBLE, autore.getUsername(), false, PageRequest.of(0, 10));

        List<Review> contenuto = risultato.getContent();

        assertThat(contenuto).hasSize(2);
        assertThat(contenuto)
                .extracting(Review::getText)
                .containsExactlyInAnyOrder("Recensione visibile normale", "Recensione nascosta dall'autore");
    }

    @Test
    void findByUser_admin_vedeAncheHiddenENascostaDallAutore_maNonLaRemoved() {
        var risultato = reviewRepository.findByUser(
                autore, ReviewStatus.VISIBLE, /* viewerUsername */ "un_admin_qualsiasi", /* isAdmin */ true, PageRequest.of(0, 10));

        List<Review> contenuto = risultato.getContent();

        // 3 = normale + nascosta dall'autore + HIDDEN. La REMOVED resta
        // fuori anche per l'admin: qui non è la vista di moderazione,
        // è la stessa lista che vede chiunque, solo con qualche riga in più.
        assertThat(contenuto).hasSize(3);
        assertThat(contenuto)
                .extracting(Review::getText)
                .containsExactlyInAnyOrder(
                        "Recensione visibile normale",
                        "Recensione nascosta dall'autore",
                        "Recensione in attesa di moderazione");
    }
}
