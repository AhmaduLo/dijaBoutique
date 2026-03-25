package com.example.dijasaliou.exception;

import com.example.dijasaliou.aspect.PlanRestrictionAspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests unitaires — GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // =========================================================
    // RuntimeException → 400 Bad Request
    // =========================================================

    @Test
    @DisplayName("handleRuntimeException() — retourne 400 avec le message de l'exception")
    void handleRuntimeException_retourne400AvecMessage() {
        RuntimeException ex = new RuntimeException("Ressource non trouvée");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("message", "Ressource non trouvée");
        assertThat(response.getBody()).containsEntry("error", "Erreur");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    // =========================================================
    // IllegalArgumentException → 400 Bad Request
    // =========================================================

    @Test
    @DisplayName("handleIllegalArgumentException() — retourne 400 avec le message")
    void handleIllegalArgumentException_retourne400() {
        IllegalArgumentException ex = new IllegalArgumentException("Le montant doit être positif");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("message", "Le montant doit être positif");
        assertThat(response.getBody()).containsEntry("error", "Argument invalide");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    // =========================================================
    // IllegalStateException → 400 Bad Request
    // =========================================================

    @Test
    @DisplayName("handleIllegalStateException() — retourne 400 avec le message")
    void handleIllegalStateException_retourne400() {
        IllegalStateException ex = new IllegalStateException("Crédit déjà soldé");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalStateException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("message", "Crédit déjà soldé");
        assertThat(response.getBody()).containsEntry("error", "Erreur de traitement");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    // =========================================================
    // PlanRestrictionException → 403 Forbidden
    // =========================================================

    @Test
    @DisplayName("handlePlanRestriction() — retourne 403 Forbidden avec le message")
    void handlePlanRestriction_retourne403() {
        PlanRestrictionAspect.PlanRestrictionException ex =
                new PlanRestrictionAspect.PlanRestrictionException(
                        "Fonctionnalité réservée aux plans ENTREPRISE");

        ResponseEntity<Map<String, Object>> response = handler.handlePlanRestriction(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("status", 403);
        assertThat(response.getBody()).containsEntry("message", "Fonctionnalité réservée aux plans ENTREPRISE");
        assertThat(response.getBody()).containsEntry("error", "Fonctionnalité non disponible");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    // =========================================================
    // UserLimitExceededException → 400 avec détails du plan
    // =========================================================

    @Test
    @DisplayName("handleUserLimitExceeded() — retourne 400 avec les détails du plan et des compteurs")
    void handleUserLimitExceeded_retourne400AvecDetails() {
        UserLimitExceededException ex = new UserLimitExceededException("GRATUIT", 3L, 3);

        ResponseEntity<Map<String, Object>> response = handler.handleUserLimitExceeded(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "Limite d'utilisateurs atteinte");
        assertThat(response.getBody()).containsEntry("planName", "GRATUIT");
        assertThat(response.getBody()).containsEntry("currentCount", 3L);
        assertThat(response.getBody()).containsEntry("maxAllowed", 3);
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleUserLimitExceeded() — le message contient le nom du plan et les limites")
    void handleUserLimitExceeded_messageContientInfosPlan() {
        UserLimitExceededException ex = new UserLimitExceededException("PRO", 5L, 5);

        ResponseEntity<Map<String, Object>> response = handler.handleUserLimitExceeded(ex);

        String message = response.getBody().get("message").toString();
        assertThat(message).contains("PRO").contains("5");
    }

    // =========================================================
    // Exception générale → 500 + message générique (sécurité)
    // =========================================================

    @Test
    @DisplayName("handleGeneralException() — retourne 500 Internal Server Error")
    void handleGeneralException_retourne500() {
        Exception ex = new Exception("erreur quelconque");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneralException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("error", "Erreur interne du serveur");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleGeneralException() — [SÉCURITÉ] le message client ne fuite pas les détails internes")
    void handleGeneralException_messageGeneriqueSanitise() {
        Exception ex = new Exception("Connection à db.interne:5432 refusée — NPE dans UserService:42");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneralException(ex);

        String messageClient = response.getBody().get("message").toString();

        // Le client ne doit jamais voir les détails techniques internes
        assertThat(messageClient)
                .doesNotContain("db.interne")
                .doesNotContain("5432")
                .doesNotContain("NPE")
                .doesNotContain("UserService");

        // Le message doit rester générique
        assertThat(messageClient).contains("erreur");
    }

    // =========================================================
    // Toutes les réponses contiennent un timestamp
    // =========================================================

    @Test
    @DisplayName("Toutes les réponses d'erreur contiennent un champ 'timestamp'")
    void toutesLesReponses_contiennentTimestamp() {
        assertThat(handler.handleRuntimeException(
                new RuntimeException("x")).getBody()).containsKey("timestamp");
        assertThat(handler.handleIllegalArgumentException(
                new IllegalArgumentException("x")).getBody()).containsKey("timestamp");
        assertThat(handler.handleIllegalStateException(
                new IllegalStateException("x")).getBody()).containsKey("timestamp");
        assertThat(handler.handleGeneralException(
                new Exception("x")).getBody()).containsKey("timestamp");
        assertThat(handler.handlePlanRestriction(
                new PlanRestrictionAspect.PlanRestrictionException("x")).getBody()).containsKey("timestamp");
    }
}
