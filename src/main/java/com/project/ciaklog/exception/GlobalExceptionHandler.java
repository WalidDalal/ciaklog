package com.project.ciaklog.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 — risorsa non trovata
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 409 — risorsa duplicata
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateResourceException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 400 — violazione regole di business
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 503 — l'AI/LLM esterna (Groq) non risponde o è temporaneamente giù.
    // Fix (🟡 trovato nei test funzionali): prima finiva nel catch-all
    // generico e tornava 500, indistinguibile da un vero bug interno — un
    // problema esterno e temporaneo deve dirlo chiaramente al frontend.
    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleAiUnavailable(AiServiceUnavailableException ex) {
        log.warn("Servizio AI non disponibile: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Il servizio AI non è disponibile al momento, riprova tra poco.");
    }

    // 401 — non autenticato / credenziali errate
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // 403 — accesso negato (utente autenticato ma non autorizzato)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // 409 — limite superato (es. max 3 WATCHING)
    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleLimitExceeded(LimitExceededException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 429 — troppi tentativi in un breve periodo (es. login brute-force)
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestsException ex) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    // 403 — accesso negato da Spring Security (es. @PreAuthorize che fallisce).
    // Fix (🔴 dal FIX post-revisione, sezione Login): senza questo handler,
    // AuthorizationDeniedException cadeva nel catch-all generico sotto e
    // tornava 500 invece di 403. Riguardava tutti gli endpoint protetti da
    // @PreAuthorize (es. POST /api/ai/chat, GET /api/ai/daily, e
    // potenzialmente AdminController/ReportController protetti a livello di
    // classe con @PreAuthorize("hasRole('ADMIN')")).
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Accesso negato: non hai i permessi necessari.");
    }

    // 400 — validazione Bean Validation (@Valid sui DTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Dati non validi");
        body.put("details", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 400 — parametro/path variable con tipo o valore non valido
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Valore non valido per '%s': %s", ex.getName(), ex.getValue());
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    // 400 — body JSON malformato o valore enum non valido (es. status:
    // "DA_VEDERE" invece di "TO_WATCH" su POST /api/library). Fix (🟡 trovato
    // durante i test funzionali): mancava questo handler, quindi
    // HttpMessageNotReadableException cadeva nel catch-all generico qui sotto
    // e tornava un 500 — un input malformato dal client non deve mai
    // risultare in un errore "interno del server".
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Richiesta con body non leggibile: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Richiesta non valida: controlla i dati inviati.");
    }

    // 400 — @RequestParam obbligatorio mancante (es. `status` su
    // PUT /api/library/{id}?status=... non passato in query string). Fix (🟡
    // trovato durante i test funzionali, stesso identico problema del
    // gestore sopra ma per i parametri invece che per il body): mancava
    // anche questo handler, quindi MissingServletRequestParameterException
    // cadeva nello stesso catch-all generico e tornava 500.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Parametro obbligatorio mancante: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Richiesta non valida: manca il parametro '" + ex.getParameterName() + "'.");
    }

    // 500 — tutto il resto (deve stare per ultimo)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Errore non gestito: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Si è verificato un errore interno. Riprova più tardi.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}