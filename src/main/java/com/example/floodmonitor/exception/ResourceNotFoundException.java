package com.example.floodmonitor.exception;

// Kommentar: Exception für nicht gefundene Ressourcen (404)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}