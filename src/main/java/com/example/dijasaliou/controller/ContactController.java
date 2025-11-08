package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.ContactRequest;
import com.example.dijasaliou.service.EmailService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur REST pour le formulaire de contact
 *
 * Permet aux administrateurs d'envoyer des messages de contact
 * à l'équipe de support
 *
 * Endpoints disponibles :
 * - POST /api/contact - Envoyer un message de contact
 */
@RestController
@RequestMapping("/contact")
@Slf4j
public class ContactController {

    private final EmailService emailService;

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Envoyer un message de contact
     *
     * POST /api/contact
     *
     * Body :
     * {
     *   "nom": "Dija Saliou",
     *   "email": "admin@boutique.com",
     *   "entreprise": "Boutique DijaSaliou",
     *   "sujet": "Demande d'assistance technique",
     *   "message": "Bonjour, j'ai besoin d'aide pour..."
     * }
     *
     * @param request Les informations du formulaire de contact
     * @param authentication Informations de l'utilisateur authentifié (optionnel)
     * @return Confirmation de l'envoi
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> sendContactMessage(
            @Valid @RequestBody ContactRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication != null ? authentication.getName() : "Non authentifié";
        log.info("Réception d'un message de contact de {} ({})", request.getNom(), userEmail);

        try {
            // Envoyer l'email de contact
            emailService.sendContactEmail(request);

            log.info("Message de contact envoyé avec succès pour {}", request.getEmail());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Votre message a été envoyé avec succès. Nous vous répondrons dans les plus brefs délais.");
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message de contact pour {}: {}", request.getEmail(), e.getMessage());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Une erreur est survenue lors de l'envoi de votre message. Veuillez réessayer plus tard.");
            response.put("status", "error");

            return ResponseEntity.status(500).body(response);
        }
    }
}
