package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.FactureDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.TenantAdminDto;
import com.example.dijasaliou.entity.TenantEntity;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.example.dijasaliou.service.SuperAdminService;
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
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SuperAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du SuperAdminController")
@WithMockUser(username = "superadmin@platform.com", authorities = {"SUPER_ADMIN"})
class SuperAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SuperAdminService superAdminService;

    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    @MockitoBean
    private com.example.dijasaliou.config.HibernateFilterInterceptor hibernateFilterInterceptor;

    @MockitoBean
    private com.example.dijasaliou.filter.SubscriptionExpirationFilter subscriptionExpirationFilter;

    private TenantAdminDto tenantDto1;
    private TenantAdminDto tenantDto2;
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken("superadmin@platform.com", null,
                    List.of(new SimpleGrantedAuthority("SUPER_ADMIN")));

    @BeforeEach
    void setUp() {
        tenantDto1 = TenantAdminDto.builder()
                .id(1L).tenantUuid("tenant-001").nomEntreprise("Boutique Alpha")
                .adminEmail("admin@alpha.com").adminNom("Diop").adminPrenom("Amadou")
                .plan(TenantEntity.Plan.STARTER).actif(true).statut("ACTIF")
                .nbUtilisateurs(3L).dateCreation(LocalDateTime.of(2025, 1, 15, 10, 0))
                .dateExpiration(LocalDateTime.of(2026, 1, 15, 10, 0)).build();

        tenantDto2 = TenantAdminDto.builder()
                .id(2L).tenantUuid("tenant-002").nomEntreprise("Boutique Beta")
                .adminEmail("admin@beta.com").adminNom("Fall").adminPrenom("Fatou")
                .plan(TenantEntity.Plan.GRATUIT).actif(false).statut("EXPIRE")
                .nbUtilisateurs(1L).dateCreation(LocalDateTime.of(2025, 3, 1, 8, 0)).build();
    }

    // ==================== GET /superadmin/stats ====================

    @Test
    @DisplayName("GET /superadmin/stats - Devrait retourner les stats globales")
    void getStats_DevraitRetournerStatsGlobales() throws Exception {
        Map<String, Object> stats = Map.of(
                "nbTenants", 15L,
                "nbTenantsActifs", 12L,
                "nbUtilisateurs", 47L,
                "revenuMensuel", 85500.0
        );
        when(superAdminService.getGlobalStats()).thenReturn(stats);

        mockMvc.perform(get("/superadmin/stats")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbTenants", is(15)))
                .andExpect(jsonPath("$.nbTenantsActifs", is(12)));

        verify(superAdminService, times(1)).getGlobalStats();
    }

    // ==================== GET /superadmin/tenants ====================

    @Test
    @DisplayName("GET /superadmin/tenants - Devrait retourner tous les tenants paginés")
    void getAllTenants_DevraitRetournerTousLesTenants() throws Exception {
        PagedResponse<TenantAdminDto> pagedResponse = PagedResponse.from(
                new PageImpl<>(List.of(tenantDto1, tenantDto2), PageRequest.of(0, 20), 2));
        when(superAdminService.getAllTenants(0, 20, null)).thenReturn(pagedResponse);

        mockMvc.perform(get("/superadmin/tenants")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].tenantUuid", is("tenant-001")))
                .andExpect(jsonPath("$.content[0].nomEntreprise", is("Boutique Alpha")));

        verify(superAdminService, times(1)).getAllTenants(0, 20, null);
    }

    @Test
    @DisplayName("GET /superadmin/tenants - Devrait retourner liste vide")
    void getAllTenants_DevraitRetournerListeVide() throws Exception {
        PagedResponse<TenantAdminDto> pagedResponse = PagedResponse.from(
                new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(superAdminService.getAllTenants(0, 20, null)).thenReturn(pagedResponse);

        mockMvc.perform(get("/superadmin/tenants")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ==================== GET /superadmin/tenants/{id} ====================

    @Test
    @DisplayName("GET /superadmin/tenants/{id} - Devrait retourner le détail d'un tenant")
    void getTenant_DevraitRetournerTenant() throws Exception {
        when(superAdminService.getTenantById(1L)).thenReturn(tenantDto1);

        mockMvc.perform(get("/superadmin/tenants/1")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nomEntreprise", is("Boutique Alpha")))
                .andExpect(jsonPath("$.adminEmail", is("admin@alpha.com")))
                .andExpect(jsonPath("$.nbUtilisateurs", is(3)));

        verify(superAdminService, times(1)).getTenantById(1L);
    }

    @Test
    @DisplayName("GET /superadmin/tenants/{id} - Devrait retourner 400 si inexistant")
    void getTenant_DevraitRetourner400SiInexistant() throws Exception {
        when(superAdminService.getTenantById(999L))
                .thenThrow(new RuntimeException("Tenant non trouvé"));

        mockMvc.perform(get("/superadmin/tenants/999")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ==================== PUT /superadmin/tenants/{id}/activate ====================

    @Test
    @DisplayName("PUT /superadmin/tenants/{id}/activate - Devrait activer le tenant")
    void activateTenant_DevraitActiver() throws Exception {
        doNothing().when(superAdminService).activerTenant(1L);

        mockMvc.perform(put("/superadmin/tenants/1/activate")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("activé")));

        verify(superAdminService, times(1)).activerTenant(1L);
    }

    // ==================== PUT /superadmin/tenants/{id}/deactivate ====================

    @Test
    @DisplayName("PUT /superadmin/tenants/{id}/deactivate - Devrait désactiver le tenant")
    void deactivateTenant_DevraitDesactiver() throws Exception {
        doNothing().when(superAdminService).desactiverTenant(1L);

        mockMvc.perform(put("/superadmin/tenants/1/deactivate")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("désactivé")));

        verify(superAdminService, times(1)).desactiverTenant(1L);
    }

    // ==================== PUT /superadmin/tenants/{id}/plan ====================

    @Test
    @DisplayName("PUT /superadmin/tenants/{id}/plan - Devrait changer le plan")
    void changePlan_DevraitChangerPlan() throws Exception {
        TenantAdminDto tenantMisAJour = TenantAdminDto.builder()
                .id(1L).tenantUuid("tenant-001").nomEntreprise("Boutique Alpha")
                .plan(TenantEntity.Plan.BUSINESS).actif(true).statut("ACTIF")
                .nbUtilisateurs(3L).build();
        when(superAdminService.changerPlan(eq(1L), eq(TenantEntity.Plan.BUSINESS), eq(30)))
                .thenReturn(tenantMisAJour);

        Map<String, Object> body = Map.of("plan", "BUSINESS", "jours", 30);

        mockMvc.perform(put("/superadmin/tenants/1/plan")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", is("BUSINESS")));

        verify(superAdminService, times(1)).changerPlan(eq(1L), eq(TenantEntity.Plan.BUSINESS), eq(30));
    }

    // ==================== DELETE /superadmin/tenants/{id} ====================

    @Test
    @DisplayName("DELETE /superadmin/tenants/{id} - Devrait supprimer le tenant")
    void deleteTenant_DevraitSupprimer() throws Exception {
        doNothing().when(superAdminService).supprimerTenant(1L);

        mockMvc.perform(delete("/superadmin/tenants/1")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("supprimé")));

        verify(superAdminService, times(1)).supprimerTenant(1L);
    }

    // ==================== GET /superadmin/tenants/{id}/factures ====================

    @Test
    @DisplayName("GET /superadmin/tenants/{id}/factures - Devrait retourner les factures")
    void getFactures_DevraitRetournerFactures() throws Exception {
        FactureDto facture = FactureDto.builder()
                .id(1L).montantCFA(6500.0).statut("PAYEE")
                .build();
        when(superAdminService.getFacturesByTenant(1L)).thenReturn(List.of(facture));

        mockMvc.perform(get("/superadmin/tenants/1/factures")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].statut", is("PAYEE")));

        verify(superAdminService, times(1)).getFacturesByTenant(1L);
    }
}
