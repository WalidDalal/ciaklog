package com.project.ciaklog.service.impl;

/*
 * DOVE METTERLO:
 *   src/test/java/com/project/ciaklog/service/impl/ReportServiceImplTest.java
 *
 * COSA DEVI CAMBIARE PERCHÉ IL TEST PASSI (il fix):
 *   1) In ReportRepository, aggiungere un metodo che conta solo le
 *      segnalazioni ancora PENDING (invece di countByReview, che le conta
 *      tutte incluse le REJECTED del passato):
 *
 *      long countByReviewAndStatus(Review review, ReportStatus status);
 *
 *   2) In ReportServiceImpl.createReviewReport(...), sostituire:
 *          long totalReports = reportRepository.countByReview(review);
 *      con:
 *          long totalReports = reportRepository.countByReviewAndStatus(review, ReportStatus.PENDING);
 *
 *      (il report appena creato in questo metodo è già PENDING di default,
 *      quindi la sua stessa segnalazione appena salvata viene comunque
 *      conteggiata — non cambia nulla per il caso "prima volta")
 *
 * Finché non fai questo cambio, il test "nonDeveNascondere..." fallisce,
 * perché oggi countByReview(review) viene chiamato e basta — questo test
 * lo intercetta verificando che il metodo "giusto" (countByReviewAndStatus
 * con PENDING) venga effettivamente chiamato e usato per la decisione.
 */

import com.project.ciaklog.entity.*;
import com.project.ciaklog.dto.request.ReportRequest;
import com.project.ciaklog.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private ReportRepository reportRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewCommentRepository reviewCommentRepository;
    @Mock private ReviewReactionRepository reviewReactionRepository;
    @Mock private UserRepository userRepository;

    private ReportServiceImpl service;

    private User autoreRecensione;
    private User segnalante;
    private Review review;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(
                reportRepository, reviewRepository, reviewCommentRepository, reviewReactionRepository, userRepository);

        autoreRecensione = User.builder()
                .id(UUID.randomUUID())
                .username("autore")
                .email("autore@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        segnalante = User.builder()
                .id(UUID.randomUUID())
                .username("segnalante")
                .email("segnalante@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        review = Review.builder()
                .id(UUID.randomUUID())
                .user(autoreRecensione)
                .tmdbId(1L)
                .contentType(ContentType.MOVIE)
                .rating(3)
                .text("Una recensione")
                .status(ReviewStatus.VISIBLE)
                .build();

        when(userRepository.findByUsername("segnalante")).thenReturn(java.util.Optional.of(segnalante));
        when(reviewRepository.findById(review.getId())).thenReturn(java.util.Optional.of(review));
        when(reportRepository.existsByReporterAndReview(segnalante, review)).thenReturn(false);
        // save() ritorna semplicemente l'oggetto passato, come farebbe un vero repository
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("NON deve nascondere automaticamente una recensione se le uniche 2 segnalazioni 'totali' sono in realtà 1 vecchia già respinta + 1 nuova")
    void nonDeveNascondereContandoAncheSegnalazioniGiaRespinte() {
        ReportRequest dto = new ReportRequest();
        dto.setReviewId(review.getId());
        dto.setReasonCategory(ReportReasonCategory.SPAM);

        // Con il fix, il service deve chiedere al repository solo il conteggio
        // delle PENDING — che qui è 1 (solo quella nuova appena creata),
        // quindi sotto soglia (serve >= 2) e la recensione NON va nascosta.
        when(reportRepository.countByReviewAndStatus(review, ReportStatus.PENDING)).thenReturn(1L);

        service.createReport("segnalante", dto);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.VISIBLE);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve nascondere automaticamente una recensione quando ci sono davvero 2 segnalazioni ancora PENDING")
    void deveNascondereConDueSegnalazioniPendingVere() {
        ReportRequest dto = new ReportRequest();
        dto.setReviewId(review.getId());
        dto.setReasonCategory(ReportReasonCategory.SPAM);

        when(reportRepository.countByReviewAndStatus(review, ReportStatus.PENDING)).thenReturn(2L);

        service.createReport("segnalante", dto);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        verify(reviewRepository).save(review);
    }
}
