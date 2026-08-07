package com.project.ciaklog.exception;

// Fix (🟡 nessuna gestione dedicata per l'AI/LLM non disponibile — trovato
// nei test funzionali): prima ogni fallimento di Groq (giù, timeout, 5xx)
// veniva rilanciato come RuntimeException generica, che GlobalExceptionHandler
// mappava sempre a 500 — indistinguibile da un vero bug interno. Con questa
// eccezione dedicata, il frontend riceve un 503 con un messaggio chiaro
// ("il servizio AI non è disponibile ora, riprova"), invece di un errore
// generico che non comunica che il problema è temporaneo e esterno.
public class AiServiceUnavailableException extends RuntimeException {
    public AiServiceUnavailableException(String message) {
        super(message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
