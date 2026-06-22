package com.project.ciaklog.service;

import com.project.ciaklog.dto.request.AiChatRequest;
import com.project.ciaklog.dto.response.AiChatResponse;
import com.project.ciaklog.dto.response.DailyRecommendationResponse;

public interface AiService {
    AiChatResponse chat(String username, AiChatRequest dto);
    DailyRecommendationResponse getDaily(String username);
}
