package com.example.dijasaliou.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour l'application
 * Permet de renvoyer des messages d'erreur structurés au frontend
 *
 * SÉCURITÉ : Les messages d'erreur génériques sont envoyés au client
 * Les détails complets sont loggés côté serveur uniquement
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Gestion de l'exception de limitation d'utilisateurs
     * Retourne un message clair au frontend avec le code 400 (Bad Request)
     */
    @ExceptionHandler(UserLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUserLimitExceeded(UserLimitExceededException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Limite d'utilisateurs atteinte");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("planName", ex.getPlanName());
        errorResponse.put("currentCount", ex.getCurrentCount());
        errorResponse.put("maxAllowed", ex.getMaxAllowed());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * Gestion des RuntimeException génériques
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Erreur");
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * Gestion de IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Argument invalide");
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * Gestion de IllegalStateException
     * Message générique au client, détails loggés serveur
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        // Logger l'erreur complète côté serveur (visible seulement dans les logs)
        log.error("IllegalStateException: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Erreur d'état du système");
        // Message générique pour le client (pas de détails sensibles)
        errorResponse.put("message", "Une erreur s'est produite lors du traitement de votre demande.");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * Gestion de toutes les autres exceptions
     * SÉCURITÉ : Message générique au client, détails complets loggés serveur uniquement
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // Logger l'exception complète côté serveur (visible seulement dans les logs)
        log.error("Exception non gérée: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Erreur interne du serveur");
        // Message générique pour le client (SÉCURITÉ : pas de fuite d'informations)
        errorResponse.put("message", "Une erreur s'est produite. Veuillez réessayer ultérieurement.");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
