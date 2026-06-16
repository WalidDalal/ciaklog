// AiChatRequestDTO.java
package com.project.ciaklog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiChatRequestDTO {

    @NotBlank(message = "message obbligatorio")
    private String message;

    private String sessionId; // nullable alla prima richiesta — il Service ne genera uno nuovo

    private List<MessageDTO> sessionHistory; // nullable alla prima richiesta

    @Getter
    @Setter
    public static class MessageDTO {
        private String role; // "user" o "assistant"
        private String content;
    }
}