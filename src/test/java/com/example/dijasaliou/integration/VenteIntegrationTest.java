package com.example.dijasaliou.integration;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.jwt.JwtService;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.repository.VenteRepository;
import com.example.dijasaliou.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration — flux complet HTTP → Service → Base de données → Réponse JSON.
 *
 * Vérifie simultanément :
 * - Item 6 : VenteDto.fromEntity() applique bien le filtrage photo selon le plan
 * - Item 7 : La chaîne complète contrôleur → service → repository → DTO fonctionne
 * - Item 8 : Le filtre Hibernate (via JwtAuthenticationFilter + HibernateFilterInterceptor)
 *            isole correctement les données entre tenants
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-integration.properties")
@Transactional
@DisplayName("Tests d'intégration — Vente : filtrage photo + isolation multi-tenant")
class VenteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenteRepository venteRepository;

    // L'EmailService est moqué pour éviter les erreurs SMTP en test
    @MockitoBean
    private EmailService emailService;

    private TenantEntity tenantBasic;
    private TenantEntity tenantPremium;
    private String jwtBasic;
    private String jwtPremium;

    @BeforeEach
    void setUp() {
        // Tenant BASIC — abonnement valide, photos masquées (BASIC < PREMIUM)
        tenantBasic = tenantRepository.saveAndFlush(TenantEntity.builder()
                .tenantUuid("it-tenant-basic")
                .nomEntreprise("Boutique Basic")
                .numeroTelephone("+221771111111")
                .plan(TenantEntity.Plan.STARTER)
                .actif(true)
                .essaiUtilise(true)
                .dateExpiration(LocalDate.now().plusDays(30).atStartOfDay())
                .build());

        // Tenant PREMIUM — les photos doivent être visibles
        tenantPremium = tenantRepository.saveAndFlush(TenantEntity.builder()
                .tenantUuid("it-tenant-premium")
                .nomEntreprise("Boutique Premium")
                .numeroTelephone("+221772222222")
                .plan(TenantEntity.Plan.PRO)
                .actif(true)
                .essaiUtilise(true)
                .dateExpiration(LocalDate.now().plusDays(30).atStartOfDay())
                .build());

        // Utilisateurs
        UserEntity userBasic = userRepository.saveAndFlush(UserEntity.builder()
                .nom("Basic").prenom("User")
                .email("user@basic.com")
                .motDePasse("encoded")
                .nomEntreprise("Boutique Basic")
                .numeroTelephone("+221771111111")
                .role(UserEntity.Role.ADMIN)
                .tenant(tenantBasic)
                .build());

        UserEntity userPremium = userRepository.saveAndFlush(UserEntity.builder()
                .nom("Premium").prenom("User")
                .email("user@premium.com")
                .motDePasse("encoded")
                .nomEntreprise("Boutique Premium")
                .numeroTelephone("+221772222222")
                .role(UserEntity.Role.ADMIN)
                .tenant(tenantPremium)
                .build());

        // Vente avec photo pour le tenant BASIC
        venteRepository.saveAndFlush(VenteEntity.builder()
                .nomProduit("Collier Basic")
                .quantite(1.0)
                .prixUnitaire(new BigDecimal("5000"))
                .prixTotal(new BigDecimal("5000"))
                .dateVente(LocalDate.now())
                .photoUrl("/photos/collier-basic.jpg")
                .utilisateur(userBasic)
                .tenant(tenantBasic)
                .build());

        // Vente avec photo pour le tenant PREMIUM
        venteRepository.saveAndFlush(VenteEntity.builder()
                .nomProduit("Collier Premium")
                .quantite(1.0)
                .prixUnitaire(new BigDecimal("8000"))
                .prixTotal(new BigDecimal("8000"))
                .dateVente(LocalDate.now())
                .photoUrl("/photos/collier-premium.jpg")
                .utilisateur(userPremium)
                .tenant(tenantPremium)
                .build());

        // Génération des tokens JWT pour chaque tenant (utilise Bearer header)
        jwtBasic = jwtService.generateToken("user@basic.com", "it-tenant-basic", com.example.dijasaliou.entity.UserEntity.Role.USER);
        jwtPremium = jwtService.generateToken("user@premium.com", "it-tenant-premium", com.example.dijasaliou.entity.UserEntity.Role.USER);
    }

    // ==================== Item 6 : Filtrage photo selon le plan ====================

    @Test
    @DisplayName("[Plan BASIC] GET /ventes — photoUrl doit être null dans la réponse JSON")
    void getVentes_planBASIC_photoUrlNullDansReponse() throws Exception {
        mockMvc.perform(get("/ventes")
                        .header("Authorization", "Bearer " + jwtBasic)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nomProduit", is("Collier Basic")))
                // CRITIQUE : Plan BASIC → la photo est masquée (réservée aux plans PREMIUM/ENTREPRISE)
                .andExpect(jsonPath("$.content[0].photoUrl").doesNotExist());
    }

    @Test
    @DisplayName("[Plan PREMIUM] GET /ventes — photoUrl visible dans la réponse JSON")
    void getVentes_planPREMIUM_photoUrlVisibleDansReponse() throws Exception {
        mockMvc.perform(get("/ventes")
                        .header("Authorization", "Bearer " + jwtPremium)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nomProduit", is("Collier Premium")))
                // CRITIQUE : Plan PREMIUM → la photo est visible
                .andExpect(jsonPath("$.content[0].photoUrl", is("/photos/collier-premium.jpg")));
    }

    // ==================== Item 8 : Isolation multi-tenant ====================

    @Test
    @DisplayName("[Isolation] JWT tenant-BASIC → vente du tenant-PREMIUM invisible")
    void getVentes_jwtTenantBasic_retourneSeulementVentesBasic() throws Exception {
        mockMvc.perform(get("/ventes")
                        .header("Authorization", "Bearer " + jwtBasic)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nomProduit", is("Collier Basic")))
                .andExpect(jsonPath("$.content[?(@.nomProduit == 'Collier Premium')]").isEmpty());
    }

    @Test
    @DisplayName("[Isolation] JWT tenant-PREMIUM → vente du tenant-BASIC invisible")
    void getVentes_jwtTenantPremium_retourneSeulementVentesPremium() throws Exception {
        mockMvc.perform(get("/ventes")
                        .header("Authorization", "Bearer " + jwtPremium)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nomProduit", is("Collier Premium")))
                .andExpect(jsonPath("$.content[?(@.nomProduit == 'Collier Basic')]").isEmpty());
    }
}
