package com.project.ciaklog.exception;

// 429 Too Many Requests — distinta da 401 (credenziali sbagliate) e 409
// (limite di business, es. max 3 titoli in WATCHING)
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
