package com.example.dijasaliou.exception;

/**
 * Exception métier pour les conflits de suppression/modification.
 * Mappe sur HTTP 409 Conflict — intercepté proprement par le frontend.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
