package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.WavePaymentConfirmRequest;
import com.example.dijasaliou.dto.WavePaymentRequest;
import com.example.dijasaliou.dto.WavePaymentResponse;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.service.EmailService;
import com.example.dijasaliou.service.FactureService;
import com.example.dijasaliou.service.TenantService;
import com.example.dijasaliou.service.WaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du PaymentController")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private TenantRepository tenantRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private WaveService waveService;

    @MockitoBean
    private FactureService factureService;

    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    @MockitoBean
    private com.example.dijasaliou.config.HibernateFilterInterceptor hibernateFilterInterceptor;

    @MockitoBean
    private com.example.dijasaliou.filter.SubscriptionExpirationFilter subscriptionExpirationFilter;

    private TenantEntity tenantTest;
    private UserEntity adminTest;

    private final UsernamePasswordAuthenticationToken principalAdmin =
            new UsernamePasswordAuthenticationToken("admin@boutique.com", null,
                    List.of(new SimpleGrantedAuthority("ADMIN")));

    private final UsernamePasswordAuthenticationToken principalSuperAdmin =
            new UsernamePasswordAuthenticationToken("superadmin@platform.com", null,
                    List.of(new SimpleGrantedAuthority("SUPER_ADMIN")));

    @BeforeEach
    void setUp() {
        tenantTest = TenantEntity.builder()
                .tenantUuid("tenant-uuid-001")
                .nomEntreprise("Boutique DijaSaliou")
                .numeroTelephone("+221771234567")
                .actif(true)
                .plan(TenantEntity.Plan.STARTER)
                .essaiUtilise(true)
                .dateExpiration(LocalDateTime.now().plusDays(20))
                .build();

        adminTest = UserEntity.builder()
                .id(1L).nom("Saliou").prenom("Dija")
                .email("admin@boutique.com")
                .motDePasse("encoded")
                .nomEntreprise("Boutique DijaSaliou")
                .numeroTelephone("+221771234567")
                .build();
    }

    // ==================== GET /payment/plans ====================

    @Test
    @DisplayName("GET /payment/plans - Devrait retourner tous les plans disponibles")
    void getAvailablePlans_DevraitRetournerPlans() throws Exception {
        mockMvc.perform(get("/payment/plans")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.STARTER").exists())
                .andExpect(jsonPath("$.PRO").exists())
                .andExpect(jsonPath("$.BUSINESS").exists())
                // GRATUIT ne doit PAS être dans la liste (plan d'essai, pas à vendre)
                .andExpect(jsonPath("$.GRATUIT").doesNotExist())
                .andExpect(jsonPath("$.STARTER.libelle", notNullValue()))
                .andExpect(jsonPath("$.STARTER.prixCFA", notNullValue()));

        // Aucun service appelé — données statiques de l'enum
        verifyNoInteractions(tenantService, waveService);
    }

    // ==================== GET /payment/subscription ====================

    @Test
    @DisplayName("GET /payment/subscription - SUPER_ADMIN reçoit statut permanent")
    void getSubscriptionStatus_SuperAdminRecoit200Permanent() throws Exception {
        mockMvc.perform(get("/payment/subscription")
                        .principal(principalSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", is("SUPER_ADMIN")))
                .andExpect(jsonPath("$.actif", is(true)))
                .andExpect(jsonPath("$.estExpire", is(false)));

        // Tenant non consulté pour SUPER_ADMIN
        verify(tenantService, never()).getCurrentTenant();
    }

    @Test
    @DisplayName("GET /payment/subscription - Admin avec abonnement actif")
    void getSubscriptionStatus_AdminAvecAbonnementActif() throws Exception {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        mockMvc.perform(get("/payment/subscription")
                        .principal(principalAdmin)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", is("STARTER")))
                .andExpect(jsonPath("$.actif", is(true)));

        verify(tenantService, times(1)).getCurrentTenant();
    }

    @Test
    @DisplayName("GET /payment/subscription - Plan GRATUIT après fin d'essai")
    void getSubscriptionStatus_PlanGratuitApresEssai() throws Exception {
        TenantEntity tenantExpire = TenantEntity.builder()
                .tenantUuid("tenant-uuid-002")
                .nomEntreprise("Boutique Expirée")
                .numeroTelephone("+221700000000")
                .actif(true)
                .plan(TenantEntity.Plan.GRATUIT)
                .essaiUtilise(true)
                .build();
        when(tenantService.getCurrentTenant()).thenReturn(tenantExpire);

        mockMvc.perform(get("/payment/subscription")
                        .principal(principalAdmin)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", is("GRATUIT")));
    }

    // ==================== POST /payment/wave/initiate ====================

    @Test
    @DisplayName("POST /payment/wave/initiate - Devrait initier un paiement Wave")
    @WithMockUser(username = "admin@boutique.com", authorities = {"ADMIN"})
    void initiateWavePayment_DevraitInitierPaiement() throws Exception {
        // Le paiement en ligne Wave est désactivé côté serveur : la route répond
        // toujours 503, sans jamais appeler waveService (redirection vers le support).
        WavePaymentRequest request = WavePaymentRequest.builder()
                .plan(TenantEntity.Plan.STARTER)
                .numeroTelephone("+221771234567")
                .build();

        mockMvc.perform(post("/payment/wave/initiate")
                        .principal(principalAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", containsString("indisponible")));

        verify(waveService, never()).initiatePayment(any(), any());
    }

    @Test
    @DisplayName("POST /payment/wave/initiate - Devrait retourner 400 si champs manquants")
    @WithMockUser(username = "admin@boutique.com", authorities = {"ADMIN"})
    void initiateWavePayment_DevraitRetourner500SiValidationEchoue() throws Exception {
        // plan null et numeroTelephone vide → @Valid échoue avant même d'atteindre le controleur
        WavePaymentRequest requestInvalide = new WavePaymentRequest(null, "");

        mockMvc.perform(post("/payment/wave/initiate")
                        .principal(principalAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalide)))
                .andExpect(status().isBadRequest());

        verify(waveService, never()).initiatePayment(any(), any());
    }

    // ==================== POST /payment/wave/confirm ====================

    @Test
    @DisplayName("POST /payment/wave/confirm - Le paiement en ligne est desactive (503)")
    @WithMockUser(username = "admin@boutique.com", authorities = {"ADMIN"})
    void confirmWavePayment_DevraitConfirmerPaiement() throws Exception {
        // Comme pour l'initiation, la confirmation Wave est désactivée côté serveur.
        WavePaymentConfirmRequest request = WavePaymentConfirmRequest.builder()
                .waveTransactionId("wave_txn_12345")
                .plan(TenantEntity.Plan.STARTER)
                .build();

        mockMvc.perform(post("/payment/wave/confirm")
                        .principal(principalAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", containsString("indisponible")));

        verify(waveService, never()).verifyPayment(any());
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /payment/wave/confirm - Le paiement en ligne est desactive (503) meme avec un transactionId invalide")
    @WithMockUser(username = "admin@boutique.com", authorities = {"ADMIN"})
    void confirmWavePayment_DevraitRetourner400SiPaiementInvalide() throws Exception {
        WavePaymentConfirmRequest request = WavePaymentConfirmRequest.builder()
                .waveTransactionId("wave_txn_INVALID")
                .plan(TenantEntity.Plan.STARTER)
                .build();

        mockMvc.perform(post("/payment/wave/confirm")
                        .principal(principalAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());

        verify(tenantRepository, never()).save(any());
    }

    // ==================== POST /payment/wave/webhook ====================

    @Test
    @DisplayName("POST /payment/wave/webhook - Devrait traiter un webhook valide")
    void handleWaveWebhook_DevraitTraiterWebhookValide() throws Exception {
        long nowSeconds = java.time.Instant.now().getEpochSecond();
        String payload = "{\"event\":\"payment.completed\",\"timestamp\":" + nowSeconds + "}";

        when(waveService.verifyWebhookSignature(payload, "valid-signature")).thenReturn(true);
        when(waveService.handleWebhook(any())).thenReturn(true);

        mockMvc.perform(post("/payment/wave/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Wave-Signature", "valid-signature")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("traité")));

        verify(waveService, times(1)).handleWebhook(any());
    }

    @Test
    @DisplayName("POST /payment/wave/webhook - Devrait rejeter signature invalide")
    void handleWaveWebhook_DevraitRejeterSignatureInvalide() throws Exception {
        String payload = "{\"event\":\"payment.completed\"}";
        when(waveService.verifyWebhookSignature(payload, "bad-sig")).thenReturn(false);

        mockMvc.perform(post("/payment/wave/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Wave-Signature", "bad-sig")
                        .content(payload))
                .andExpect(status().isBadRequest());

        verify(waveService, never()).handleWebhook(any());
    }
}
