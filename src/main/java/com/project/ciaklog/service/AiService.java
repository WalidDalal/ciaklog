package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.AiChatRequest;
import com.project.ciaklog.dto.request.StructureReviewRequest;
import com.project.ciaklog.dto.response.AiChatResponse;
import com.project.ciaklog.dto.response.DailyRecommendationResponse;
import com.project.ciaklog.dto.response.StructureReviewResponse;

public interface AiService {
    AiChatResponse chat(String username, AiChatRequest dto);
    AiChatResponse chatAdmin(String username, AiChatRequest dto);
    DailyRecommendationResponse getDaily(String username);

    // Fix (AI più centrale): "recensione a botta calda" — struttura appunti
    // sparsi dell'utente in una recensione ben scritta, mantenendo il suo tono
    StructureReviewResponse structureReview(String username, StructureReviewRequest dto);
}