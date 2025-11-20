package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.*;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.service.EmailService;
import com.example.dijasaliou.service.StripeService;
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
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final WaveService waveService;

    public PaymentController(StripeService stripeService,
                             TenantService tenantService,
                             TenantRepository tenantRepository,
                             UserRepository userRepository,
                             EmailService emailService,
                             WaveService waveService) {
        this.stripeService = stripeService;
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.waveService = waveService;
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

        // Calculer les jours restants et la date d'expiration à afficher
        Long joursRestants = null;
        Boolean estExpire = false;
        LocalDateTime dateExpirationAffichee = null;
        Boolean essaiGratuit = tenant.essaiGratuitValide();

        if (essaiGratuit) {
            // Si l'essai gratuit est actif, calculer la date de fin de l'essai
            dateExpirationAffichee = tenant.getDateDebutEssai().plusDays(14);
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(now, dateExpirationAffichee);
            joursRestants = duration.toDays();
            estExpire = false;
        } else if (tenant.getDateExpiration() != null) {
            // Si abonnement payant, utiliser la date d'expiration de l'abonnement
            dateExpirationAffichee = tenant.getDateExpiration();
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(now, tenant.getDateExpiration());
            joursRestants = duration.toDays();
            estExpire = joursRestants < 0;
        } else {
            // Pas d'essai gratuit et pas d'abonnement payant
            estExpire = true;
        }

        // Message personnalisé
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

        // IMPORTANT : Marquer l'essai gratuit comme utilisé
        // Une fois qu'un utilisateur paie, il ne peut plus bénéficier de l'essai gratuit
        if (!tenant.getEssaiUtilise()) {
            tenant.setEssaiUtilise(true);
            log.info("Essai gratuit marqué comme utilisé pour le tenant {}", tenant.getTenantUuid());
        }

        tenantRepository.save(tenant);

        log.info("Abonnement {} activé pour le tenant {} jusqu'au {}",
                request.getPlan(), tenant.getTenantUuid(), dateExpiration);

        // 4. Récupérer les informations de l'utilisateur pour l'email
        UserEntity user = userRepository.findByEmailAndDeletedFalse(emailAdmin)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 5. Envoyer l'email de confirmation de paiement
        String userName = user.getPrenom() + " " + user.getNom();
        String nomEntreprise = tenant.getNomEntreprise();
        String planLibelle = request.getPlan().getLibelle();
        double montant = request.getPlan().getPrixEuro();
        String devise = "EUR"; // Vous pouvez récupérer la devise réelle depuis les metadata du PaymentIntent
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
            log.info("Email de confirmation de paiement envoyé à {}", user.getEmail());
        } catch (Exception e) {
            // On ne bloque pas le processus si l'email échoue
            log.error("Erreur lors de l'envoi de l'email de confirmation : {}", e.getMessage());
        }

        // 6. Retourner la confirmation
        Map<String, String> response = new HashMap<>();
        response.put("message", "Paiement confirmé ! Votre abonnement " + request.getPlan().getLibelle() + " est maintenant actif.");
        response.put("plan", request.getPlan().name());
        response.put("dateExpiration", dateExpiration.toString());

        return ResponseEntity.ok(response);
    }

    // ==================== ENDPOINTS WAVE (MOBILE MONEY SÉNÉGAL) ====================

    /**
     * POST /api/payment/wave/initiate
     * Initie un paiement via Wave (Mobile Money Sénégal)
     *
     * AUTHENTIFICATION REQUISE : L'utilisateur doit être connecté (ADMIN)
     *
     * Body :
     * {
     *   "plan": "BASIC",
     *   "numeroTelephone": "+221771234567"
     * }
     *
     * Retourne :
     * {
     *   "waveTransactionId": "wave_123456789",
     *   "waveUrl": "https://pay.wave.com/checkout/wave_123456789",
     *   "montant": 6555,
     *   "devise": "XOF",
     *   "plan": "BASIC",
     *   "numeroTelephone": "+221****4567",
     *   "statut": "PENDING"
     * }
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

        // Initier le paiement Wave
        WavePaymentResponse response = waveService.initiatePayment(request, tenant.getTenantUuid());

        log.info("Paiement Wave initié: Transaction ID = {}", response.getWaveTransactionId());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payment/wave/confirm
     * Confirme un paiement Wave après que l'utilisateur ait validé sur son téléphone
     *
     * AUTHENTIFICATION REQUISE : L'utilisateur doit être connecté (ADMIN)
     *
     * Body :
     * {
     *   "waveTransactionId": "wave_123456789",
     *   "plan": "BASIC"
     * }
     *
     * Retourne :
     * {
     *   "message": "Paiement confirmé ! Votre abonnement Plan Basic est maintenant actif.",
     *   "plan": "BASIC",
     *   "dateExpiration": "2025-12-19T..."
     * }
     */
    @PostMapping("/wave/confirm")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, String>> confirmWavePayment(
            @Valid @RequestBody WavePaymentConfirmRequest request,
            Authentication authentication) {

        String emailAdmin = authentication.getName();
        log.info("Admin {} confirme le paiement Wave {} pour le plan {}",
                emailAdmin, request.getWaveTransactionId(), request.getPlan());

        // 1. Vérifier que le paiement Wave a bien été effectué
        boolean isPaymentValid = waveService.verifyPayment(request.getWaveTransactionId());

        if (!isPaymentValid) {
            log.error("Paiement Wave {} non valide ou non confirmé", request.getWaveTransactionId());
            throw new RuntimeException("Le paiement n'a pas été confirmé par Wave. Veuillez réessayer ou contacter le support.");
        }

        // 2. Récupérer le tenant actuel
        TenantEntity tenant = tenantService.getCurrentTenant();

        // 3. Activer l'abonnement pour 30 jours
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateExpiration = now.plusDays(30); // 1 mois

        tenant.setPlan(request.getPlan());
        tenant.setDateExpiration(dateExpiration);
        tenant.setActif(true);

        // IMPORTANT : Marquer l'essai gratuit comme utilisé
        if (!tenant.getEssaiUtilise()) {
            tenant.setEssaiUtilise(true);
            log.info("Essai gratuit marqué comme utilisé pour le tenant {}", tenant.getTenantUuid());
        }

        tenantRepository.save(tenant);

        log.info("Abonnement {} activé via Wave pour le tenant {} jusqu'au {}",
                request.getPlan(), tenant.getTenantUuid(), dateExpiration);

        // 4. Récupérer les informations de l'utilisateur pour l'email
        UserEntity user = userRepository.findByEmailAndDeletedFalse(emailAdmin)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 5. Envoyer l'email de confirmation de paiement
        String userName = user.getPrenom() + " " + user.getNom();
        String nomEntreprise = tenant.getNomEntreprise();
        String planLibelle = request.getPlan().getLibelle();
        double montant = request.getPlan().getPrixCFA(); // Prix en XOF pour Wave
        String devise = "XOF"; // Francs CFA
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
            // On ne bloque pas le processus si l'email échoue
            log.error("Erreur lors de l'envoi de l'email de confirmation : {}", e.getMessage());
        }

        // 6. Retourner la confirmation
        Map<String, String> responseWave = new HashMap<>();
        responseWave.put("message", "Paiement Wave confirmé ! Votre abonnement " + request.getPlan().getLibelle() + " est maintenant actif.");
        responseWave.put("plan", request.getPlan().name());
        responseWave.put("dateExpiration", dateExpiration.toString());

        return ResponseEntity.ok(responseWave);
    }

    /**
     * POST /api/payment/wave/webhook
     * Webhook pour recevoir les notifications de paiement Wave
     *
     * Ce endpoint est PUBLIC (pas d'authentification) car appelé par Wave
     * La sécurité est assurée par la vérification de la signature Wave
     */
    @PostMapping("/wave/webhook")
    public ResponseEntity<String> handleWaveWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Réception webhook Wave: {}", payload);

        try {
            boolean success = waveService.handleWebhook(payload);

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
