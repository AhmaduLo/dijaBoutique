package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.WavePaymentRequest;
import com.example.dijasaliou.dto.WavePaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Service pour gérer les paiements via Wave (Mobile Money Sénégal)
 *
 * DOCUMENTATION API WAVE :
 * https://developers.wave.com/
 *
 * FLUX DE PAIEMENT :
 * 1. Client choisit un plan et entre son numéro Wave
 * 2. Backend appelle l'API Wave pour initier le paiement
 * 3. Wave envoie une notification push au téléphone du client
 * 4. Client confirme le paiement sur son téléphone
 * 5. Wave envoie un webhook au backend pour confirmer
 * 6. Backend active l'abonnement
 */
@Service
@Slf4j
public class WaveService {

    @Value("${wave.api.key:disabled}")
    private String waveApiKey;

    @Value("${wave.api.secret:disabled}")
    private String waveApiSecret;

    @Value("${wave.api.url:https://api.wave.com/v1}")
    private String waveApiUrl;

    @Value("${wave.webhook.url:disabled}")
    private String waveWebhookUrl;

    private final RestTemplate restTemplate;

    public WaveService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Initie un paiement Wave
     *
     * @param request Requête de paiement contenant le plan et le numéro de téléphone
     * @param tenantUuid UUID du tenant pour traçabilité
     * @return Réponse contenant l'ID de transaction et l'URL de paiement
     */
    public WavePaymentResponse initiatePayment(WavePaymentRequest request, String tenantUuid) {
        log.info("Initiation paiement Wave pour tenant {} - Plan: {}, Tel: {}",
                tenantUuid, request.getPlan(), maskPhoneNumber(request.getNumeroTelephone()));

        // Calculer le montant en XOF (Francs CFA)
        double montantXOF = request.getPlan().getPrixCFA();

        // Générer un ID de transaction unique
        String transactionId = "wave_" + UUID.randomUUID().toString();

        try {
            // Préparer la requête pour l'API Wave
            Map<String, Object> waveRequest = new HashMap<>();
            waveRequest.put("amount", (int) montantXOF); // Wave utilise des centimes
            waveRequest.put("currency", "XOF");
            waveRequest.put("customer_phone", request.getNumeroTelephone());
            waveRequest.put("description", "Abonnement " + request.getPlan().getLibelle() + " - Dija Saliou");
            waveRequest.put("client_reference", transactionId);
            waveRequest.put("webhook_url", waveWebhookUrl);
            waveRequest.put("metadata", Map.of(
                    "tenant_uuid", tenantUuid,
                    "plan", request.getPlan().name(),
                    "platform", "DijaSaliou"
            ));

            // Appeler l'API Wave
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + waveApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(waveRequest, headers);

            // IMPORTANT : En mode développement sans clés Wave, simuler la réponse
            if (waveApiKey == null || waveApiKey.isEmpty()) {
                log.warn("⚠️ MODE DÉVELOPPEMENT : Simulation de paiement Wave (pas de vraie API)");
                return simulateWavePayment(transactionId, montantXOF, request);
            }

            // Appel réel à l'API Wave
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                    waveApiUrl + "/checkout/sessions",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = response.getBody();

                return WavePaymentResponse.builder()
                        .waveTransactionId((String) responseBody.get("id"))
                        .waveUrl((String) responseBody.get("wave_launch_url"))
                        .montant(montantXOF)
                        .devise("XOF")
                        .plan(request.getPlan().name())
                        .numeroTelephone(maskPhoneNumber(request.getNumeroTelephone()))
                        .statut("PENDING")
                        .build();
            } else {
                throw new RuntimeException("Erreur lors de l'initiation du paiement Wave");
            }

        } catch (Exception e) {
            log.error("Erreur lors de l'initiation du paiement Wave: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible d'initier le paiement Wave. Veuillez réessayer.");
        }
    }

    /**
     * Vérifie le statut d'un paiement Wave
     *
     * @param waveTransactionId ID de la transaction Wave
     * @return true si le paiement est confirmé, false sinon
     */
    public boolean verifyPayment(String waveTransactionId) {
        log.info("Vérification du paiement Wave: {}", waveTransactionId);

        try {
            // IMPORTANT : En mode développement, simuler la vérification
            if (waveApiKey == null || waveApiKey.isEmpty()) {
                log.warn("⚠️ MODE DÉVELOPPEMENT : Simulation de vérification Wave");
                // En dev, on considère tous les paiements comme validés
                return true;
            }

            // Appel réel à l'API Wave pour vérifier le statut
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + waveApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                    waveApiUrl + "/checkout/sessions/" + waveTransactionId,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = response.getBody();
                String status = (String) responseBody.get("status");

                // Wave utilise "completed" pour les paiements réussis
                return "completed".equalsIgnoreCase(status);
            }

            return false;

        } catch (Exception e) {
            log.error("Erreur lors de la vérification du paiement Wave: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Simule un paiement Wave en mode développement
     * (utilisé quand les clés API ne sont pas configurées)
     */
    private WavePaymentResponse simulateWavePayment(String transactionId, double montant, WavePaymentRequest request) {
        log.info("🧪 Simulation Wave - Transaction: {}, Montant: {} XOF", transactionId, montant);

        return WavePaymentResponse.builder()
                .waveTransactionId(transactionId)
                .waveUrl("https://pay.wave.com/checkout/" + transactionId) // URL fictive
                .montant(montant)
                .devise("XOF")
                .plan(request.getPlan().name())
                .numeroTelephone(maskPhoneNumber(request.getNumeroTelephone()))
                .statut("PENDING")
                .build();
    }

    /**
     * Masque un numéro de téléphone pour les logs (sécurité)
     * Exemple: +221771234567 -> +221****4567
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "***";
        }

        String prefix = phoneNumber.substring(0, 4);
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + "****" + suffix;
    }

    /**
     * Vérifie la signature d'un webhook Wave
     *
     * SÉCURITÉ CRITIQUE : Vérification de la signature HMAC SHA-256
     * Cela garantit que le webhook provient bien de Wave et n'a pas été falsifié
     *
     * @param payload Le corps du webhook en JSON
     * @param signature La signature fournie par Wave dans l'en-tête X-Wave-Signature
     * @return true si la signature est valide
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        // En mode développement sans clés API, autoriser tous les webhooks
        if (waveApiSecret == null || waveApiSecret.isEmpty()) {
            log.warn("⚠️ MODE DÉVELOPPEMENT : Webhook Wave accepté sans vérification de signature");
            return true;
        }

        if (signature == null || signature.isEmpty()) {
            log.error("❌ SÉCURITÉ: Aucune signature fournie dans le webhook Wave");
            return false;
        }

        try {
            // Calculer le HMAC SHA-256 du payload avec le secret Wave
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    waveApiSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmac.init(secretKey);

            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Convertir en hexadécimal
            String calculatedSignature = HexFormat.of().formatHex(hash);

            // Comparer avec la signature fournie (comparaison sécurisée)
            boolean isValid = constantTimeEquals(calculatedSignature, signature);

            if (isValid) {
                log.info("✅ Signature webhook Wave valide");
            } else {
                log.error("❌ SÉCURITÉ: Signature webhook Wave invalide ! Possible tentative d'attaque.");
            }

            return isValid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("❌ Erreur lors de la vérification de signature Wave: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Comparaison de chaînes en temps constant (protection contre timing attacks)
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Traite un webhook Wave (notification de paiement)
     *
     * NOTE: La vérification de la signature doit être faite AVANT d'appeler cette méthode
     * (dans le controller)
     *
     * @param payload Données envoyées par Wave
     * @return true si le webhook est valide et traité
     */
    public boolean handleWebhook(Map<String, Object> payload) {
        log.info("Réception webhook Wave: {}", payload);

        try {
            String status = (String) payload.get("status");
            String transactionId = (String) payload.get("id");

            if ("completed".equalsIgnoreCase(status)) {
                log.info("✅ Paiement Wave confirmé: {}", transactionId);
                return true;
            } else if ("failed".equalsIgnoreCase(status)) {
                log.warn("❌ Paiement Wave échoué: {}", transactionId);
                return false;
            }

            return false;

        } catch (Exception e) {
            log.error("Erreur lors du traitement du webhook Wave: {}", e.getMessage(), e);
            return false;
        }
    }
}
