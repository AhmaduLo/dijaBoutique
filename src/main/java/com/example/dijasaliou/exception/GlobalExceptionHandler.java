package com.example.dijasaliou.exception;

import com.example.dijasaliou.aspect.PlanRestrictionAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
     * Gestion des restrictions de plan d'abonnement (@RequiresPlan)
     * Retourne un 403 Forbidden avec un message explicite
     */
    @ExceptionHandler(PlanRestrictionAspect.PlanRestrictionException.class)
    public ResponseEntity<Map<String, Object>> handlePlanRestriction(PlanRestrictionAspect.PlanRestrictionException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.FORBIDDEN.value());
        errorResponse.put("error", "Fonctionnalité non disponible");
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

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
     * Gestion des erreurs de validation @Valid sur les @RequestBody
     * Retourne 400 avec la liste des champs invalides
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Données invalides");
        errorResponse.put("message", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Filet de sécurité : violation de contrainte DB (ex: email en double)
     * Retourne 400 avec message lisible au lieu de 500
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolationException: {}", ex.getMostSpecificCause().getMessage());
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Données invalides");
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.contains("Duplicate entry")) {
            errorResponse.put("message", "Cet email est déjà utilisé par un autre compte.");
        } else {
            errorResponse.put("message", "Violation de contrainte base de données.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Gestion des RuntimeException génériques
     *
     * SÉCURITÉ : ex.getMessage() n'est jamais envoyé tel quel au client.
     * - Si le message ressemble à un message utilisateur connu (court, sans stack trace)
     *   → retourné en 400 (erreur métier, ex: "Utilisateur non trouvé")
     * - Sinon → message générique 500 (ex: NullPointerException interne)
     * Dans tous les cas, la stack trace complète est loggée côté serveur uniquement.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException: {} - {}", ex.getClass().getName(), ex.getMessage(), ex);

        String message = ex.getMessage();
        boolean isUserFacingMessage = message != null
                && message.length() < 200
                && !message.contains("Exception")
                && !message.contains("at com.")
                && !message.contains("at java.")
                && !message.contains("at org.");

        if (isUserFacingMessage) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", LocalDateTime.now());
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Erreur de traitement");
            errorResponse.put("message", message);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Erreur interne");
        errorResponse.put("message", "Une erreur inattendue s'est produite.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Gestion de IllegalArgumentException
     * SÉCURITÉ : filtre le message comme RuntimeException — court et sans stack trace → 400, sinon générique
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        String message = ex.getMessage();
        boolean isUserFacing = message != null
                && message.length() < 200
                && !message.contains("Exception")
                && !message.contains("at com.")
                && !message.contains("at java.");
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Requête invalide");
        errorResponse.put("message", isUserFacing ? message : "Requête invalide.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Gestion des conflits métier (409 Conflict) — suppression bloquée par une dépendance.
     * Le message est retourné directement au frontend via l'intercepteur case 409.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflictException(ConflictException ex) {
        log.warn("ConflictException: {}", ex.getMessage());
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.CONFLICT.value());
        errorResponse.put("error", "Conflit");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Gestion de IllegalStateException (erreurs métier : contraintes, blocages)
     * Le message est retourné au client s'il est court et sans stack trace.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        String message = ex.getMessage();
        boolean isUserFacing = message != null
                && message.length() < 300
                && !message.contains("Exception")
                && !message.contains("at com.")
                && !message.contains("at java.");
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Opération non autorisée");
        errorResponse.put("message", isUserFacing ? message : "Opération non autorisée.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
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
        errorResponse.put("message", "Une erreur inattendue s'est produite. Veuillez réessayer ou contacter le support.");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
