package com.example.dijasaliou.controller;

import com.example.dijasaliou.dto.UpdateTenantRequest;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.TenantService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TenantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du TenantController")
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantService tenantService;

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
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken("admin@boutique.com", null,
                    List.of(new SimpleGrantedAuthority("ADMIN")));

    @BeforeEach
    void setUp() {
        tenantTest = TenantEntity.builder()
                .tenantUuid("tenant-uuid-001")
                .nomEntreprise("Boutique DijaSaliou")
                .numeroTelephone("+221771234567")
                .adresse("Dakar, Sénégal")
                .nineaSiret("12345678")
                .actif(true)
                .plan(TenantEntity.Plan.BASIC)
                .build();

        adminTest = UserEntity.builder()
                .id(1L)
                .nom("Saliou")
                .prenom("Dija")
                .email("admin@boutique.com")
                .motDePasse("encoded")
                .nomEntreprise("Boutique DijaSaliou")
                .numeroTelephone("+221771234567")
                .build();
    }

    // ==================== GET /tenant/info ====================

    @Test
    @DisplayName("GET /tenant/info - Devrait retourner les infos du tenant")
    void getEntrepriseInfo_DevraitRetournerInfos() throws Exception {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(tenantService.getAdminProprietaire(tenantTest)).thenReturn(adminTest);

        mockMvc.perform(get("/tenant/info")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantUuid", is("tenant-uuid-001")))
                .andExpect(jsonPath("$.nomEntreprise", is("Boutique DijaSaliou")))
                .andExpect(jsonPath("$.numeroTelephone", is("+221771234567")))
                .andExpect(jsonPath("$.emailProprietaire", is("admin@boutique.com")))
                .andExpect(jsonPath("$.nomProprietaire", is("Saliou")))
                .andExpect(jsonPath("$.prenomProprietaire", is("Dija")));

        verify(tenantService, times(1)).getCurrentTenant();
        verify(tenantService, times(1)).getAdminProprietaire(tenantTest);
    }

    @Test
    @DisplayName("GET /tenant/info - Devrait gérer admin null (propriétaire non trouvé)")
    void getEntrepriseInfo_DevraitGererAdminNull() throws Exception {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(tenantService.getAdminProprietaire(tenantTest)).thenReturn(null);

        mockMvc.perform(get("/tenant/info")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantUuid", is("tenant-uuid-001")))
                .andExpect(jsonPath("$.nomProprietaire").doesNotExist());
    }

    // ==================== GET /admin/entreprise ====================

    @Test
    @DisplayName("GET /admin/entreprise - Devrait retourner les infos de l'entreprise")
    void getEntreprise_DevraitRetournerInfos() throws Exception {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(tenantService.getAdminProprietaire(tenantTest)).thenReturn(adminTest);

        mockMvc.perform(get("/admin/entreprise")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomEntreprise", is("Boutique DijaSaliou")))
                .andExpect(jsonPath("$.adresse", is("Dakar, Sénégal")))
                .andExpect(jsonPath("$.nineaSiret", is("12345678")));

        verify(tenantService, times(1)).getCurrentTenant();
    }

    // ==================== PUT /admin/entreprise ====================

    @Test
    @DisplayName("PUT /admin/entreprise - Devrait modifier les informations")
    void updateEntreprise_DevraitModifier() throws Exception {
        TenantEntity tenantModifie = TenantEntity.builder()
                .tenantUuid("tenant-uuid-001")
                .nomEntreprise("Nouvelle Boutique")
                .numeroTelephone("+221779999999")
                .adresse("Thiès, Sénégal")
                .actif(true)
                .plan(TenantEntity.Plan.BASIC)
                .build();

        when(tenantService.updateTenant(any(UpdateTenantRequest.class))).thenReturn(tenantModifie);
        when(tenantService.getAdminProprietaire(tenantModifie)).thenReturn(adminTest);

        UpdateTenantRequest request = new UpdateTenantRequest();
        request.setNomEntreprise("Nouvelle Boutique");
        request.setNumeroTelephone("+221779999999");

        mockMvc.perform(put("/admin/entreprise")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomEntreprise", is("Nouvelle Boutique")))
                .andExpect(jsonPath("$.numeroTelephone", is("+221779999999")));

        verify(tenantService, times(1)).updateTenant(any(UpdateTenantRequest.class));
    }

    @Test
    @DisplayName("PUT /admin/entreprise - Devrait retourner 400 si service échoue")
    void updateEntreprise_DevraitRetourner400SiErreur() throws Exception {
        when(tenantService.updateTenant(any()))
                .thenThrow(new RuntimeException("Nom entreprise invalide"));

        UpdateTenantRequest request = new UpdateTenantRequest();
        request.setNomEntreprise("");

        mockMvc.perform(put("/admin/entreprise")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
