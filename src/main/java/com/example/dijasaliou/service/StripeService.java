package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.CreatePaymentIntentRequest;
import com.example.dijasaliou.dto.PaymentIntentResponse;
import com.example.dijasaliou.entity.TenantEntity;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service pour gérer les paiements avec Stripe
 *
 * IMPORTANT : Avant d'utiliser ce service, il faut :
 * 1. Créer un compte Stripe sur https://stripe.com
 * 2. Récupérer les clés API (Test mode) sur https://dashboard.stripe.com/apikeys
 * 3. Remplacer les clés dans application.properties
 *
 * FLUX DE PAIEMENT :
 * 1. Frontend appelle POST /api/payment/create-intent avec le plan
 * 2. Backend crée un PaymentIntent avec Stripe et retourne le clientSecret
 * 3. Frontend utilise Stripe.js pour afficher le formulaire de paiement
 * 4. Frontend confirme le paiement avec Stripe
 * 5. Frontend appelle POST /api/payment/success pour activer l'abonnement
 */
@Service
@Slf4j
public class StripeService {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    /**
     * Initialise Stripe avec la clé secrète
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe initialisé avec succès");
    }

    /**
     * Retourne la clé publique Stripe (safe pour le frontend)
     */
    public String getPublicKey() {
        return stripePublicKey;
    }

    /**
     * Crée un PaymentIntent Stripe pour un plan donné
     *
     * NOUVEAU FLUX : Le PaymentIntent est lié à un Stripe Customer créé avec le tenant_uuid
     * Cela permet de tracer tous les paiements d'un tenant et facilite la gestion des abonnements
     *
     * @param request Contient le plan et la devise
     * @param tenant Le tenant qui effectue le paiement
     * @return PaymentIntentResponse avec le clientSecret
     */
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request, TenantEntity tenant) {
        try {
            // 1. Calculer le montant selon le plan et la devise
            long montant = calculateAmount(request.getPlan(), request.getDevise());
            String devise = request.getDevise().equalsIgnoreCase("CFA") ? "xof" : "eur";

            // 2. Créer ou récupérer le Stripe Customer pour ce tenant
            Customer customer = createOrGetStripeCustomer(tenant);

            // 3. Créer les métadonnées pour tracer le paiement
            Map<String, String> metadata = new HashMap<>();
            metadata.put("plan", request.getPlan().name());
            metadata.put("devise_originale", request.getDevise());
            metadata.put("tenant_uuid", tenant.getTenantUuid()); // Traçabilité
            metadata.put("tenant_nom", tenant.getNomEntreprise());

            // 4. Créer le PaymentIntent avec Stripe lié au Customer
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(montant)
                    .setCurrency(devise)
                    .setCustomer(customer.getId()) // Lier au Customer Stripe
                    .setDescription("Abonnement " + request.getPlan().getLibelle() + " - " + tenant.getNomEntreprise())
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            log.info("PaymentIntent créé : {} - Tenant: {} - Customer: {} - Montant : {} {} - Plan : {}",
                    paymentIntent.getId(), tenant.getTenantUuid(), customer.getId(), montant, devise, request.getPlan());

            // 5. Retourner la réponse avec le clientSecret
            return PaymentIntentResponse.builder()
                    .clientSecret(paymentIntent.getClientSecret())
                    .montant(montant)
                    .devise(devise)
                    .plan(request.getPlan().name())
                    .message("PaymentIntent créé avec succès")
                    .build();

        } catch (StripeException e) {
            log.error("Erreur lors de la création du PaymentIntent : {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la création du paiement : " + e.getMessage());
        }
    }

    /**
     * Crée ou récupère un Stripe Customer pour un tenant
     *
     * Le Customer Stripe permet de :
     * - Tracer tous les paiements d'un tenant
     * - Faciliter les paiements futurs (carte enregistrée)
     * - Gérer les abonnements récurrents
     *
     * On utilise le tenant_uuid comme metadata pour retrouver le Customer
     *
     * @param tenant Le tenant pour lequel créer/récupérer le Customer
     * @return Le Customer Stripe
     */
    private Customer createOrGetStripeCustomer(TenantEntity tenant) throws StripeException {
        // 1. Chercher si un Customer existe déjà pour ce tenant
        // Note : On liste tous les customers et on filtre par metadata côté application
        // car l'API Stripe ne permet pas de filtrer directement par metadata
        CustomerListParams listParams = CustomerListParams.builder()
                .setLimit(100L) // Récupérer jusqu'à 100 customers
                .build();

        var customers = Customer.list(listParams);

        // 2. Filtrer manuellement par tenant_uuid dans les metadata
        for (Customer customer : customers.getData()) {
            Map<String, String> customerMetadata = customer.getMetadata();
            if (customerMetadata != null && tenant.getTenantUuid().equals(customerMetadata.get("tenant_uuid"))) {
                log.debug("Customer Stripe existant trouvé : {} pour tenant {}",
                        customer.getId(), tenant.getTenantUuid());
                return customer;
            }
        }

        // 3. Sinon, créer un nouveau Customer
        CustomerCreateParams createParams = CustomerCreateParams.builder()
                .setName(tenant.getNomEntreprise())
                .setPhone(tenant.getNumeroTelephone())
                .putMetadata("tenant_uuid", tenant.getTenantUuid())
                .putMetadata("tenant_nom", tenant.getNomEntreprise())
                .setDescription("Tenant: " + tenant.getNomEntreprise())
                .build();

        Customer newCustomer = Customer.create(createParams);

        log.info("Nouveau Customer Stripe créé : {} pour tenant {} ({})",
                newCustomer.getId(), tenant.getTenantUuid(), tenant.getNomEntreprise());

        return newCustomer;
    }

    /**
     * Vérifie qu'un PaymentIntent a bien été payé
     *
     * @param paymentIntentId ID du PaymentIntent Stripe
     * @return true si le paiement est réussi
     */
    public boolean verifyPaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            boolean isSucceeded = "succeeded".equals(paymentIntent.getStatus());

            if (isSucceeded) {
                log.info("PaymentIntent {} vérifié avec succès - Montant : {} {}",
                        paymentIntentId,
                        paymentIntent.getAmount(),
                        paymentIntent.getCurrency());
            } else {
                log.warn("PaymentIntent {} n'est pas succeeded : {}",
                        paymentIntentId,
                        paymentIntent.getStatus());
            }

            return isSucceeded;

        } catch (StripeException e) {
            log.error("Erreur lors de la vérification du PaymentIntent {} : {}",
                    paymentIntentId, e.getMessage());
            return false;
        }
    }

    /**
     * Calcule le montant à facturer selon le plan et la devise
     *
     * @param plan Plan sélectionné
     * @param devise EUR ou CFA
     * @return Montant en centimes
     */
    private long calculateAmount(TenantEntity.Plan plan, String devise) {
        if (devise.equalsIgnoreCase("CFA")) {
            // Pour le CFA, Stripe utilise des unités minimales (pas de centimes)
            return (long) plan.getPrixCFA();
        } else {
            // Pour l'EUR, convertir en centimes (9.99€ = 999 centimes)
            return (long) (plan.getPrixEuro() * 100);
        }
    }
}
