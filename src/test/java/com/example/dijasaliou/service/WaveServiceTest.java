package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.WavePaymentRequest;
import com.example.dijasaliou.dto.WavePaymentResponse;
import com.example.dijasaliou.entity.TenantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests unitaires — WaveService")
class WaveServiceTest {

    private WaveService waveService;

    @BeforeEach
    void setUp() {
        waveService = new WaveService();
        // Par défaut : mode développement (clés vides)
        ReflectionTestUtils.setField(waveService, "waveApiKey", "");
        ReflectionTestUtils.setField(waveService, "waveApiSecret", "");
        ReflectionTestUtils.setField(waveService, "waveApiUrl", "https://api.wave.com/v1");
        ReflectionTestUtils.setField(waveService, "waveWebhookUrl", "http://localhost:8080/api/payment/wave/webhook");
    }

    // =========================================================
    // initiatePayment — mode développement (pas de clé API)
    // =========================================================

    @Test
    @DisplayName("initiatePayment() dev mode — retourne une réponse PENDING avec devise XOF et plan BASIC")
    void initiatePayment_devMode_retourneReponseAvecStatutPendingEtDeviseXof() {
        WavePaymentRequest request = new WavePaymentRequest();
        request.setPlan(TenantEntity.Plan.BASIC);
        request.setNumeroTelephone("+221771234567");

        WavePaymentResponse response = waveService.initiatePayment(request, "uuid-tenant-test");

        assertThat(response).isNotNull();
        assertThat(response.getStatut()).isEqualTo("PENDING");
        assertThat(response.getDevise()).isEqualTo("XOF");
        assertThat(response.getPlan()).isEqualTo("BASIC");
        assertThat(response.getWaveTransactionId()).startsWith("wave_");
        assertThat(response.getWaveUrl()).contains(response.getWaveTransactionId());
    }

    @Test
    @DisplayName("initiatePayment() dev mode — le montant correspond au prixCFA du plan")
    void initiatePayment_devMode_montantEgalPrixCfaDuPlan() {
        WavePaymentRequest request = new WavePaymentRequest();
        request.setPlan(TenantEntity.Plan.BASIC);
        request.setNumeroTelephone("+221771234567");

        WavePaymentResponse response = waveService.initiatePayment(request, "uuid-tenant-test");

        assertThat(response.getMontant()).isEqualTo(TenantEntity.Plan.BASIC.getPrixCFA());
        // BASIC = 5000 CFA
        assertThat(response.getMontant()).isEqualTo(5000.0);
    }

    // =========================================================
    // verifyPayment — mode développement
    // =========================================================

    @Test
    @DisplayName("verifyPayment() dev mode — retourne true sans appel API")
    void verifyPayment_devMode_retourneTrue() {
        boolean resultat = waveService.verifyPayment("wave_transaction-fake-id");

        assertThat(resultat).isTrue();
    }

    // =========================================================
    // verifyWebhookSignature
    // =========================================================

    @Test
    @DisplayName("verifyWebhookSignature() — retourne true si waveApiSecret est vide (mode dev)")
    void verifyWebhookSignature_devMode_retourneTrue() {
        // waveApiSecret est "" par défaut dans setUp
        boolean resultat = waveService.verifyWebhookSignature("{\"status\":\"completed\"}", "n'importe-quelle-signature");

        assertThat(resultat).isTrue();
    }

    @Test
    @DisplayName("verifyWebhookSignature() — retourne false si la signature est null (secret configuré)")
    void verifyWebhookSignature_retourneFalse_siSignatureNull() {
        ReflectionTestUtils.setField(waveService, "waveApiSecret", "test-secret");

        boolean resultat = waveService.verifyWebhookSignature("{\"status\":\"completed\"}", null);

        assertThat(resultat).isFalse();
    }

    @Test
    @DisplayName("verifyWebhookSignature() — retourne true pour une signature HMAC SHA-256 valide")
    void verifyWebhookSignature_retourneTrue_pourSignatureHmacValide() throws Exception {
        ReflectionTestUtils.setField(waveService, "waveApiSecret", "test-secret");

        String payload = "payload";

        // Calculer la signature attendue
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec("test-secret".getBytes(), "HmacSHA256"));
        String signatureAttendue = HexFormat.of().formatHex(hmac.doFinal(payload.getBytes()));

        boolean resultat = waveService.verifyWebhookSignature(payload, signatureAttendue);

        assertThat(resultat).isTrue();
    }

    @Test
    @DisplayName("verifyWebhookSignature() — retourne false pour une signature HMAC invalide")
    void verifyWebhookSignature_retourneFalse_pourSignatureHmacInvalide() {
        ReflectionTestUtils.setField(waveService, "waveApiSecret", "test-secret");

        boolean resultat = waveService.verifyWebhookSignature("payload", "signature-totalement-fausse");

        assertThat(resultat).isFalse();
    }

    // =========================================================
    // handleWebhook
    // =========================================================

    @Test
    @DisplayName("handleWebhook() — retourne true pour status 'completed', false pour 'failed'")
    void handleWebhook_retourneTrue_pourCompleted_etFalse_pourFailed() {
        Map<String, Object> payloadCompleted = new HashMap<>();
        payloadCompleted.put("status", "completed");
        payloadCompleted.put("id", "wave_transaction-123");

        Map<String, Object> payloadFailed = new HashMap<>();
        payloadFailed.put("status", "failed");
        payloadFailed.put("id", "wave_transaction-456");

        boolean resultCompleted = waveService.handleWebhook(payloadCompleted);
        boolean resultFailed = waveService.handleWebhook(payloadFailed);

        assertThat(resultCompleted).isTrue();
        assertThat(resultFailed).isFalse();
    }
}
