// DailyRecommendationResponseDTO.java
package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DailyRecommendationResponse {
    private List<TmdbSearchResultResponse> suggestions;
    private LocalDateTime generatedAt;
}