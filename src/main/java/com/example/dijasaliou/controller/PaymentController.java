package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.*;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.service.StripeService;
import com.example.dijasaliou.service.TenantService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller pour les paiements et abonnements
 *
 * ROUTES :
 * - GET /api/payment/config          → Récupère la clé publique Stripe (tous)
 * - GET /api/payment/subscription    → Statut de l'abonnement actuel (tous)
 * - POST /api/payment/create-intent  → Crée un PaymentIntent Stripe (ADMIN)
 * - POST /api/payment/success        → Confirme le paiement et active l'abonnement (ADMIN)
 */
@RestController
@RequestMapping("/payment")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class PaymentController {

    private final StripeService stripeService;
    private final TenantService tenantService;
    private final TenantRepository tenantRepository;

    public PaymentController(StripeService stripeService,
                             TenantService tenantService,
                             TenantRepository tenantRepository) {
        this.stripeService = stripeService;
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
    }

    /**
     * GET /api/payment/config
     * Retourne la clé publique Stripe pour le frontend
     *
     * Accessible publiquement (pas besoin d'authentification)
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getStripeConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("publicKey", stripeService.getPublicKey());
        return ResponseEntity.ok(config);
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

        // Calculer les jours restants
        Long joursRestants = null;
        Boolean estExpire = false;

        if (tenant.getDateExpiration() != null) {
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(now, tenant.getDateExpiration());
            joursRestants = duration.toDays();
            estExpire = joursRestants < 0;
        }

        // Vérifier si le plan est GRATUIT (pas encore payé)
        Boolean essaiGratuit = false; // Plus d'essai gratuit

        // Message personnalisé
        String message;
        if (tenant.getPlan() == TenantEntity.Plan.GRATUIT) {
            message = "Aucun abonnement actif - Veuillez souscrire à un plan pour accéder à l'application";
        } else if (estExpire) {
            message = "Abonnement expiré - Veuillez renouveler votre abonnement";
        } else {
            message = String.format("Abonnement %s actif", tenant.getPlan().getLibelle());
        }

        SubscriptionStatusResponse response = SubscriptionStatusResponse.builder()
                .plan(tenant.getPlan().name())
                .actif(tenant.getActif())
                .dateExpiration(tenant.getDateExpiration())
                .joursRestants(joursRestants)
                .essaiGratuit(essaiGratuit)
                .estExpire(estExpire || tenant.getPlan() == TenantEntity.Plan.GRATUIT)
                .message(message)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payment/create-intent
     * Crée un PaymentIntent Stripe pour initialiser le paiement
     *
     * AUTHENTIFICATION REQUISE : L'utilisateur doit être connecté (ADMIN)
     * Le PaymentIntent est lié au tenant_uuid de l'utilisateur pour traçabilité
     *
     * Body :
     * {
     *   "plan": "BASIC",
     *   "devise": "EUR"
     * }
     *
     * Retourne :
     * {
     *   "clientSecret": "pi_xxx_secret_xxx",
     *   "montant": 999,
     *   "devise": "eur",
     *   "plan": "BASIC"
     * }
     */
    @PostMapping("/create-intent")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request,
            Authentication authentication) {

        String emailUser = authentication.getName();
        TenantEntity tenant = tenantService.getCurrentTenant();

        log.info("Admin {} (Tenant: {}) crée un PaymentIntent pour le plan {}",
                emailUser, tenant.getTenantUuid(), request.getPlan());

        // Vérifier que ce n'est pas le plan GRATUIT
        if (request.getPlan() == TenantEntity.Plan.GRATUIT) {
            throw new RuntimeException("Impossible de créer un paiement pour le plan GRATUIT");
        }

        // Créer le PaymentIntent lié au tenant
        PaymentIntentResponse response = stripeService.createPaymentIntent(request, tenant);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payment/success
     * Confirme le paiement réussi et active l'abonnement
     *
     * Nécessite authentification ADMIN (utilisateur doit être connecté)
     *
     * Body :
     * {
     *   "paymentIntentId": "pi_xxx",
     *   "plan": "BASIC"
     * }
     *
     * IMPORTANT : Cette route doit être appelée APRÈS la confirmation du paiement côté frontend
     * et APRÈS que l'utilisateur se soit connecté
     */
    @PostMapping("/success")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, String>> confirmPayment(
            @Valid @RequestBody PaymentSuccessRequest request,
            Authentication authentication) {

        String emailAdmin = authentication.getName();
        log.info("Admin {} confirme le paiement {} pour le plan {}",
                emailAdmin, request.getPaymentIntentId(), request.getPlan());

        // 1. Vérifier que le PaymentIntent a bien été payé
        boolean isPaymentValid = stripeService.verifyPaymentIntent(request.getPaymentIntentId());

        if (!isPaymentValid) {
            log.error("PaymentIntent {} non valide ou non payé", request.getPaymentIntentId());
            throw new RuntimeException("Le paiement n'a pas été confirmé. Veuillez réessayer.");
        }

        // 2. Récupérer le tenant actuel
        TenantEntity tenant = tenantService.getCurrentTenant();

        // 3. Activer l'abonnement pour 30 jours
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateExpiration = now.plusDays(30); // 1 mois

        tenant.setPlan(request.getPlan());
        tenant.setDateExpiration(dateExpiration);
        tenant.setActif(true);

        tenantRepository.save(tenant);

        log.info("Abonnement {} activé pour le tenant {} jusqu'au {}",
                request.getPlan(), tenant.getTenantUuid(), dateExpiration);

        // 4. Retourner la confirmation
        Map<String, String> response = new HashMap<>();
        response.put("message", "Paiement confirmé ! Votre abonnement " + request.getPlan().getLibelle() + " est maintenant actif.");
        response.put("plan", request.getPlan().name());
        response.put("dateExpiration", dateExpiration.toString());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/payment/plans
     * Retourne la liste des plans disponibles avec leurs prix
     *
     * Accessible publiquement (pas besoin d'authentification)
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
