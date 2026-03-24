package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.UpdateTenantRequest;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — TenantService")
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TenantService tenantService;

    private TenantEntity tenantTest;
    private UserEntity adminUser;
    private UserEntity vendeurUser;

    @BeforeEach
    void setUp() {
        adminUser = UserEntity.builder()
                .id(1L)
                .nom("Diallo")
                .prenom("Mamadou")
                .email("mamadou@example.com")
                .role(UserEntity.Role.ADMIN)
                .nomEntreprise("Boutique Test")
                .build();

        vendeurUser = UserEntity.builder()
                .id(2L)
                .nom("Ndiaye")
                .prenom("Fatou")
                .email("fatou@example.com")
                .role(UserEntity.Role.USER)
                .nomEntreprise("Boutique Test")
                .build();

        tenantTest = TenantEntity.builder()
                .id(1L)
                .tenantUuid("uuid-tenant-test")
                .nomEntreprise("Boutique Test")
                .numeroTelephone("+221771234567")
                .adresse("Dakar, Sénégal")
                .nineaSiret("123456789")
                .plan(TenantEntity.Plan.STARTER)
                .utilisateurs(Arrays.asList(adminUser, vendeurUser))
                .build();
    }

    // =========================================================
    // getCurrentTenant
    // =========================================================

    @Test
    @DisplayName("getCurrentTenant() — lève IllegalStateException si TenantContext retourne null")
    void getCurrentTenant_leveException_siTenantContextNull() {
        try (MockedStatic<TenantContext> mockedStatic = mockStatic(TenantContext.class)) {
            mockedStatic.when(TenantContext::getCurrentTenant).thenReturn(null);

            assertThatThrownBy(() -> tenantService.getCurrentTenant())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Impossible d'accéder aux données sans tenant");
        }
    }

    @Test
    @DisplayName("getCurrentTenant() — lève IllegalStateException si le repo ne trouve pas le tenant")
    void getCurrentTenant_leveException_siTenantIntrouvableDansRepo() {
        try (MockedStatic<TenantContext> mockedStatic = mockStatic(TenantContext.class)) {
            mockedStatic.when(TenantContext::getCurrentTenant).thenReturn("uuid-inconnu");
            when(tenantRepository.findByTenantUuid("uuid-inconnu")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.getCurrentTenant())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Tenant introuvable pour l'UUID: uuid-inconnu");
        }
    }

    @Test
    @DisplayName("getCurrentTenant() — retourne le tenant si tout est valide")
    void getCurrentTenant_succes() {
        try (MockedStatic<TenantContext> mockedStatic = mockStatic(TenantContext.class)) {
            mockedStatic.when(TenantContext::getCurrentTenant).thenReturn("uuid-tenant-test");
            when(tenantRepository.findByTenantUuid("uuid-tenant-test")).thenReturn(Optional.of(tenantTest));

            TenantEntity resultat = tenantService.getCurrentTenant();

            assertThat(resultat).isNotNull();
            assertThat(resultat.getTenantUuid()).isEqualTo("uuid-tenant-test");
            assertThat(resultat.getNomEntreprise()).isEqualTo("Boutique Test");
        }
    }

    // =========================================================
    // isTenantDefined
    // =========================================================

    @Test
    @DisplayName("isTenantDefined() — retourne true si TenantContext.isCurrentTenantSet() = true")
    void isTenantDefined_retourneTrue() {
        try (MockedStatic<TenantContext> mockedStatic = mockStatic(TenantContext.class)) {
            mockedStatic.when(TenantContext::isCurrentTenantSet).thenReturn(true);

            boolean resultat = tenantService.isTenantDefined();

            assertThat(resultat).isTrue();
        }
    }

    @Test
    @DisplayName("isTenantDefined() — retourne false si TenantContext.isCurrentTenantSet() = false")
    void isTenantDefined_retourneFalse() {
        try (MockedStatic<TenantContext> mockedStatic = mockStatic(TenantContext.class)) {
            mockedStatic.when(TenantContext::isCurrentTenantSet).thenReturn(false);

            boolean resultat = tenantService.isTenantDefined();

            assertThat(resultat).isFalse();
        }
    }

    // =========================================================
    // getTenantByUuid
    // =========================================================

    @Test
    @DisplayName("getTenantByUuid() — retourne le tenant si l'UUID est trouvé")
    void getTenantByUuid_succes() {
        when(tenantRepository.findByTenantUuid("uuid-tenant-test")).thenReturn(Optional.of(tenantTest));

        TenantEntity resultat = tenantService.getTenantByUuid("uuid-tenant-test");

        assertThat(resultat).isNotNull();
        assertThat(resultat.getTenantUuid()).isEqualTo("uuid-tenant-test");
        verify(tenantRepository).findByTenantUuid("uuid-tenant-test");
    }

    @Test
    @DisplayName("getTenantByUuid() — lève IllegalArgumentException si l'UUID est inconnu")
    void getTenantByUuid_leveException_siUuidInconnu() {
        when(tenantRepository.findByTenantUuid("uuid-inexistant")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantByUuid("uuid-inexistant"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant introuvable pour l'UUID: uuid-inexistant");
    }

    // =========================================================
    // updateTenant
    // =========================================================

    @Test
    @DisplayName("updateTenant() — met à jour le nom de l'entreprise et le propage aux utilisateurs")
    void updateTenant_metsAJourNomEntreprise_etPropagehAuxUtilisateurs() {
        try (MockedStatic<TenantContext> mockedStatic = mockStatic(TenantContext.class)) {
            mockedStatic.when(TenantContext::getCurrentTenant).thenReturn("uuid-tenant-test");
            when(tenantRepository.findByTenantUuid("uuid-tenant-test")).thenReturn(Optional.of(tenantTest));
            when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenantTest));
            when(tenantRepository.saveAndFlush(any(TenantEntity.class))).thenReturn(tenantTest);
            when(userRepository.saveAllAndFlush(anyList())).thenReturn(Arrays.asList(adminUser, vendeurUser));

            UpdateTenantRequest request = new UpdateTenantRequest();
            request.setNomEntreprise("Nouveau Nom Boutique");
            request.setNumeroTelephone("+221779876543");

            TenantEntity resultat = tenantService.updateTenant(request);

            assertThat(resultat).isNotNull();
            verify(tenantRepository).saveAndFlush(any(TenantEntity.class));
            verify(userRepository).saveAllAndFlush(anyList());
            // Vérifier que le nom a été propagé aux utilisateurs
            assertThat(adminUser.getNomEntreprise()).isEqualTo("Nouveau Nom Boutique");
            assertThat(vendeurUser.getNomEntreprise()).isEqualTo("Nouveau Nom Boutique");
        }
    }

    // =========================================================
    // getAdminProprietaire
    // =========================================================

    @Test
    @DisplayName("getAdminProprietaire() — retourne l'utilisateur ADMIN du tenant")
    void getAdminProprietaire_retourneAdmin_siPresent() {
        UserEntity resultat = tenantService.getAdminProprietaire(tenantTest);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getRole()).isEqualTo(UserEntity.Role.ADMIN);
        assertThat(resultat.getEmail()).isEqualTo("mamadou@example.com");
    }

    @Test
    @DisplayName("getAdminProprietaire() — retourne null si aucun ADMIN dans le tenant")
    void getAdminProprietaire_retourneNull_siAucunAdmin() {
        TenantEntity tenantSansAdmin = TenantEntity.builder()
                .id(2L)
                .tenantUuid("uuid-sans-admin")
                .nomEntreprise("Boutique Sans Admin")
                .utilisateurs(Collections.singletonList(vendeurUser))
                .build();

        UserEntity resultat = tenantService.getAdminProprietaire(tenantSansAdmin);

        assertThat(resultat).isNull();
    }
}
