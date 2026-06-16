// AiChatResponseDTO.java
package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiChatResponseDTO {
    private List<TmdbSearchResultDTO> suggestions;
    private String sessionId;
}