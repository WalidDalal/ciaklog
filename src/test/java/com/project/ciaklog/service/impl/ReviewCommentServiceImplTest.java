package com.project.ciaklog.service.impl;

/*
 * DOVE METTERLO:
 *   src/test/java/com/project/ciaklog/service/impl/ReviewCommentServiceImplTest.java
 *
 * IL BUG:
 *   updateComment(...) controlla solo che l'autore sia il proprietario, ma
 *   non controlla lo stato del commento né quello della recensione madre.
 *   Risultato: si può modificare il testo di una risposta già rimossa
 *   (status REMOVED), o di una risposta la cui recensione madre non è più
 *   visibile — createComment invece blocca correttamente questo secondo
 *   caso (vedi "review.getStatus() != VISIBLE" dentro createComment).
 *
 * IL FIX in ReviewCommentServiceImpl.updateComment, subito dopo il
 * controllo di autorizzazione esistente:
 *
 *   if (comment.getStatus() != ReviewStatus.VISIBLE) {
 *       throw new BusinessRuleException("Non è possibile modificare una risposta non più visibile");
 *   }
 *   if (comment.getReview().getStatus() != ReviewStatus.VISIBLE) {
 *       throw new BusinessRuleException("Non è possibile modificare una risposta la cui recensione non è più visibile");
 *   }
 */

import com.project.ciaklog.dto.request.ReviewCommentRequest;
import com.project.ciaklog.entity.*;
import com.project.ciaklog.exception.BusinessRuleException;
import com.project.ciaklog.exception.ForbiddenException;
import com.project.ciaklog.repository.ReviewCommentRepository;
import com.project.ciaklog.repository.ReviewRepository;
import com.project.ciaklog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewCommentServiceImplTest {

    @Mock private ReviewCommentRepository reviewCommentRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private UserRepository userRepository;

    private ReviewCommentServiceImpl service;

    private User autore;
    private Review review;

    @BeforeEach
    void setUp() {
        service = new ReviewCommentServiceImpl(reviewCommentRepository, reviewRepository, userRepository);

        autore = User.builder()
                .id(UUID.randomUUID())
                .username("luca")
                .email("luca@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        review = Review.builder()
                .id(UUID.randomUUID())
                .user(autore)
                .tmdbId(1L)
                .contentType(ContentType.MOVIE)
                .rating(4)
                .text("Recensione")
                .status(ReviewStatus.VISIBLE)
                .build();

        when(userRepository.findByUsername("luca")).thenReturn(Optional.of(autore));
    }

    @Test
    @DisplayName("NON deve permettere di modificare una risposta già rimossa")
    void nonDeveModificareRispostaGiaRimossa() {
        ReviewComment comment = ReviewComment.builder()
                .id(UUID.randomUUID())
                .review(review)
                .author(autore)
                .text("testo originale")
                .status(ReviewStatus.REMOVED)
                .build();
        when(reviewCommentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        ReviewCommentRequest dto = new ReviewCommentRequest();
        dto.setText("provo a modificarla comunque");

        assertThatThrownBy(() -> service.updateComment("luca", comment.getId(), dto))
                .isInstanceOf(BusinessRuleException.class);

        verify(reviewCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("NON deve permettere di modificare una risposta la cui recensione madre non è più visibile")
    void nonDeveModificareSeRecensioneMadreNonVisibile() {
        review.setStatus(ReviewStatus.REMOVED);

        ReviewComment comment = ReviewComment.builder()
                .id(UUID.randomUUID())
                .review(review)
                .author(autore)
                .text("testo originale")
                .status(ReviewStatus.VISIBLE)
                .build();
        when(reviewCommentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        ReviewCommentRequest dto = new ReviewCommentRequest();
        dto.setText("provo a modificarla comunque");

        assertThatThrownBy(() -> service.updateComment("luca", comment.getId(), dto))
                .isInstanceOf(BusinessRuleException.class);

        verify(reviewCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve continuare a permettere la modifica normale di una risposta visibile su una recensione visibile")
    void deveModificareNormalmenteSeTuttoVisibile() {
        ReviewComment comment = ReviewComment.builder()
                .id(UUID.randomUUID())
                .review(review)
                .author(autore)
                .text("testo originale")
                .status(ReviewStatus.VISIBLE)
                .build();
        when(reviewCommentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(reviewCommentRepository.save(comment)).thenReturn(comment);

        ReviewCommentRequest dto = new ReviewCommentRequest();
        dto.setText("testo modificato");

        service.updateComment("luca", comment.getId(), dto);

        assertThat(comment.getText()).isEqualTo("testo modificato");
        verify(reviewCommentRepository, times(1)).save(comment);
    }

    @Test
    @DisplayName("NON deve permettere di modificare la risposta di un altro utente")
    void nonDeveModificareRispostaDiAltroUtente() {
        User altroAutore = User.builder()
                .id(UUID.randomUUID())
                .username("altro")
                .email("altro@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        ReviewComment comment = ReviewComment.builder()
                .id(UUID.randomUUID())
                .review(review)
                .author(altroAutore)
                .text("testo originale")
                .status(ReviewStatus.VISIBLE)
                .build();
        when(reviewCommentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        ReviewCommentRequest dto = new ReviewCommentRequest();
        dto.setText("provo a modificarla comunque");

        assertThatThrownBy(() -> service.updateComment("luca", comment.getId(), dto))
                .isInstanceOf(ForbiddenException.class);
    }
}
