// AiChatResponseDTO.java
package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiChatResponse {
    private String reply;
    private List<TmdbSearchResultResponse> suggestions;
    private String sessionId;
}