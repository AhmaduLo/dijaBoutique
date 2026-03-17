package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.*;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.service.EmailService;
import com.example.dijasaliou.service.TenantService;
import com.example.dijasaliou.service.WaveService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller pour les paiements et abonnements (Wave Mobile Money)
 *
 * ROUTES :
 * - GET /api/payment/subscription    → Statut de l'abonnement actuel
 * - POST /api/payment/wave/initiate  → Initie un paiement Wave (ADMIN)
 * - POST /api/payment/wave/confirm   → Confirme un paiement Wave (ADMIN)
 * - POST /api/payment/wave/webhook   → Webhook Wave
 * - GET /api/payment/plans           → Liste des plans disponibles
 */
@RestController
@RequestMapping("/payment")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class PaymentController {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final WaveService waveService;

    public PaymentController(TenantService tenantService,
                             TenantRepository tenantRepository,
                             UserRepository userRepository,
                             EmailService emailService,
                             WaveService waveService) {
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.waveService = waveService;
    }

    /**
     * GET /api/payment/subscription
     * Retourne le statut de l'abonnement actuel
     *
     * Accessible à tous les utilisateurs authentifiés
     */
    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionStatusResponse> getSubscriptionStatus(Authentication authentication) {
        String email = authentication.getName();
        log.info("Utilisateur {} consulte son abonnement", email);

        TenantEntity tenant = tenantService.getCurrentTenant();

        Long joursRestants = null;
        Boolean estExpire = false;
        LocalDateTime dateExpirationAffichee = null;
        Boolean essaiGratuit = tenant.essaiGratuitValide();

        if (essaiGratuit) {
            dateExpirationAffichee = tenant.getDateDebutEssai().plusDays(14);
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(now, dateExpirationAffichee);
            joursRestants = duration.toDays();
            estExpire = false;
        } else if (tenant.getDateExpiration() != null) {
            dateExpirationAffichee = tenant.getDateExpiration();
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(now, tenant.getDateExpiration());
            joursRestants = duration.toDays();
            estExpire = joursRestants < 0;
        } else {
            estExpire = true;
        }

        String message;
        if (essaiGratuit) {
            message = String.format("Essai gratuit actif - %d jours restants", joursRestants);
        } else if (tenant.getPlan() == TenantEntity.Plan.GRATUIT) {
            message = "Période d'essai terminée - Veuillez souscrire à un plan pour accéder à l'application";
        } else if (estExpire) {
            message = "Abonnement expiré - Veuillez renouveler votre abonnement";
        } else {
            message = String.format("Abonnement %s actif - %d jours restants", tenant.getPlan().getLibelle(), joursRestants);
        }

        SubscriptionStatusResponse response = SubscriptionStatusResponse.builder()
                .plan(tenant.getPlan().name())
                .actif(tenant.getActif())
                .dateExpiration(dateExpirationAffichee)
                .joursRestants(joursRestants)
                .essaiGratuit(essaiGratuit)
                .estExpire(estExpire)
                .message(message)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==================== ENDPOINTS WAVE (MOBILE MONEY SÉNÉGAL) ====================

    /**
     * POST /api/payment/wave/initiate
     * Initie un paiement via Wave (Mobile Money Sénégal)
     */
    @PostMapping("/wave/initiate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<WavePaymentResponse> initiateWavePayment(
            @Valid @RequestBody WavePaymentRequest request,
            Authentication authentication) {

        String emailUser = authentication.getName();
        TenantEntity tenant = tenantService.getCurrentTenant();

        log.info("Admin {} (Tenant: {}) initie un paiement Wave pour le plan {}",
                emailUser, tenant.getTenantUuid(), request.getPlan());

        WavePaymentResponse response = waveService.initiatePayment(request, tenant.getTenantUuid());

        log.info("Paiement Wave initié: Transaction ID = {}", response.getWaveTransactionId());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payment/wave/confirm
     * Confirme un paiement Wave après que l'utilisateur ait validé sur son téléphone
     */
    @PostMapping("/wave/confirm")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, String>> confirmWavePayment(
            @Valid @RequestBody WavePaymentConfirmRequest request,
            Authentication authentication) {

        String emailAdmin = authentication.getName();
        log.info("Admin {} confirme le paiement Wave {} pour le plan {}",
                emailAdmin, request.getWaveTransactionId(), request.getPlan());

        boolean isPaymentValid = waveService.verifyPayment(request.getWaveTransactionId());

        if (!isPaymentValid) {
            log.error("Paiement Wave {} non valide ou non confirmé", request.getWaveTransactionId());
            throw new RuntimeException("Le paiement n'a pas été confirmé par Wave. Veuillez réessayer ou contacter le support.");
        }

        TenantEntity tenant = tenantService.getCurrentTenant();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateExpiration = now.plusDays(30);

        tenant.setPlan(request.getPlan());
        tenant.setDateExpiration(dateExpiration);
        tenant.setActif(true);

        if (!tenant.getEssaiUtilise()) {
            tenant.setEssaiUtilise(true);
            log.info("Essai gratuit marqué comme utilisé pour le tenant {}", tenant.getTenantUuid());
        }

        tenantRepository.save(tenant);

        log.info("Abonnement {} activé via Wave pour le tenant {} jusqu'au {}",
                request.getPlan(), tenant.getTenantUuid(), dateExpiration);

        UserEntity user = userRepository.findByEmailAndDeletedFalse(emailAdmin)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String userName = user.getPrenom() + " " + user.getNom();
        String nomEntreprise = tenant.getNomEntreprise();
        String planLibelle = request.getPlan().getLibelle();
        double montant = request.getPlan().getPrixCFA();
        String devise = "XOF";
        String dateExpirationFormatee = dateExpiration.toLocalDate().toString();

        try {
            emailService.sendPaymentConfirmationEmail(
                    user.getEmail(),
                    userName,
                    nomEntreprise,
                    planLibelle,
                    montant,
                    devise,
                    dateExpirationFormatee
            );
            log.info("Email de confirmation de paiement Wave envoyé à {}", user.getEmail());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de confirmation : {}", e.getMessage());
        }

        Map<String, String> responseWave = new HashMap<>();
        responseWave.put("message", "Paiement Wave confirmé ! Votre abonnement " + request.getPlan().getLibelle() + " est maintenant actif.");
        responseWave.put("plan", request.getPlan().name());
        responseWave.put("dateExpiration", dateExpiration.toString());

        return ResponseEntity.ok(responseWave);
    }

    /**
     * POST /api/payment/wave/webhook
     * Webhook pour recevoir les notifications de paiement Wave
     */
    @PostMapping("/wave/webhook")
    public ResponseEntity<String> handleWaveWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Wave-Signature", required = false) String signature) {

        log.info("Réception webhook Wave");

        try {
            boolean isSignatureValid = waveService.verifyWebhookSignature(payload, signature);

            if (!isSignatureValid) {
                log.error("❌ SÉCURITÉ: Signature webhook Wave invalide ! Possible tentative d'attaque.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature invalide");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(payload, Map.class);

            boolean success = waveService.handleWebhook(payloadMap);

            if (success) {
                log.info("✅ Webhook Wave traité avec succès");
                return ResponseEntity.ok("Webhook traité");
            } else {
                log.warn("⚠️ Webhook Wave non traité (statut non 'completed')");
                return ResponseEntity.ok("Webhook reçu mais non traité");
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du webhook Wave: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur");
        }
    }

    /**
     * GET /api/payment/plans
     * Retourne la liste des plans disponibles avec leurs prix
     */
    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> getAvailablePlans() {
        Map<String, Object> plans = new HashMap<>();

        for (TenantEntity.Plan plan : TenantEntity.Plan.values()) {
            if (plan != TenantEntity.Plan.GRATUIT) {
                Map<String, Object> planInfo = new HashMap<>();
                planInfo.put("libelle", plan.getLibelle());
                planInfo.put("description", plan.getDescription());
                planInfo.put("prixEuro", plan.getPrixEuro());
                planInfo.put("prixCFA", plan.getPrixCFA());
                planInfo.put("maxUtilisateurs", plan.getMaxUtilisateurs());
                plans.put(plan.name(), planInfo);
            }
        }

        return ResponseEntity.ok(plans);
    }
}
