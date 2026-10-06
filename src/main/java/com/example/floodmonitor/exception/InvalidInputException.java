package com.example.floodmonitor.exception;

// Kommentar: Exception für ungültige Parameter wie from > to, limit <= 0 etc. (400)
public class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) {
        super(message);
    }
}