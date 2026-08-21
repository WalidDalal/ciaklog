package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.AiChatRequest;
import com.project.ciaklog.dto.request.MovieQuestionRequest;
import com.project.ciaklog.dto.request.StructureCommentRequest;
import com.project.ciaklog.dto.request.StructureReviewRequest;
import com.project.ciaklog.dto.response.AiChatResponse;
import com.project.ciaklog.dto.response.DailyRecommendationResponse;
import com.project.ciaklog.dto.response.MovieQuestionResponse;
import com.project.ciaklog.dto.response.ReviewOpinionResponse;
import com.project.ciaklog.dto.response.StructureReviewResponse;

import java.util.UUID;

public interface AiService {
    AiChatResponse chat(String username, AiChatRequest dto);
    AiChatResponse chatAdmin(String username, AiChatRequest dto);
    DailyRecommendationResponse getDaily(String username);

    // "recensione a botta calda" — struttura appunti
    // sparsi dell'utente in una recensione ben scritta, mantenendo il suo tono
    StructureReviewResponse structureReview(String username, StructureReviewRequest dto);

    // Stesso principio della "botta calda"
    // per le recensioni, applicato alle risposte — testo più breve, tono da
    // commento, con il contesto di cosa si sta rispondendo
    StructureReviewResponse structureComment(String username, StructureCommentRequest dto);

    // "Chiedi su questo film" — risponde su un titolo
    // specifico (trama, cast, ecc.), stateless, separata dalla chat generale
    MovieQuestionResponse answerMovieQuestion(String username, MovieQuestionRequest dto);

    // Metodo generico per generare testo libero dall'AI dato un prompt già
    // pronto — evita di duplicare la logica di chiamata a Groq (retry,
    // gestione errori) in ogni nuovo service. Ritorna stringa vuota (non
    // lancia eccezione) se l'AI non risponde.
    String generateNarrative(String prompt);

    // Solo su richiesta esplicita dell'utente sulla propria recensione con
    // voto basso — mai automatica
    ReviewOpinionResponse getAiOpinionOnReview(String username, UUID reviewId);

    // Frase breve al posto del solito messaggio piatto quando non c'è nulla
    // da mostrare (libreria vuota, ricerca senza risultati) — riusa
    // generateNarrative()
    String getEmptyStateTip(String context);
}