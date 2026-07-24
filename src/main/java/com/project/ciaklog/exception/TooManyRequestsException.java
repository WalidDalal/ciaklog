package com.project.ciaklog.exception;

// Nessuna eccezione esistente
// mappava correttamente a 429 Too Many Requests, lo status HTTP corretto per
// "hai fatto troppi tentativi, riprova più tardi" (diverso da 401 credenziali
// sbagliate, o 409 limite di business come max 3 titoli in WATCHING)
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
