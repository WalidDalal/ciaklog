package com.project.ciaklog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiChatRequest {

    @NotBlank(message = "message obbligatorio")
    private String message;

    private String sessionId; // nullable alla prima richiesta — il Service ne genera uno nuovo

    private List<MessageDTO> sessionHistory; // nullable alla prima richiesta

    public enum MessageRole {
        USER, ASSISTANT
    }

    @Getter
    @Setter
    public static class MessageDTO {
        @NotNull(message = "role obbligatorio")
        private MessageRole role;

        @NotBlank(message = "content obbligatorio")
        private String content;
    }
}
