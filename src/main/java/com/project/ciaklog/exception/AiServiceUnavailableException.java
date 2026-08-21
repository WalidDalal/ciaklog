package com.project.ciaklog.exception;

// Eccezione dedicata per Groq/LLM non disponibile — mappata a 503 in
// GlobalExceptionHandler, distinta da un 500 generico
public class AiServiceUnavailableException extends RuntimeException {
    public AiServiceUnavailableException(String message) {
        super(message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
