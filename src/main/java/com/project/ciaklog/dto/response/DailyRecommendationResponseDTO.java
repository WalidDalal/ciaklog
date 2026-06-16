// DailyRecommendationResponseDTO.java
package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DailyRecommendationResponseDTO {
    private List<TmdbSearchResultDTO> suggestions;
    private LocalDateTime generatedAt;
}