package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.CreateDeviseDto;
import com.example.dijasaliou.dto.UpdateDeviseDto;
import com.example.dijasaliou.entity.DeviseEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.DeviseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du DeviseService")
class DeviseServiceTest {

    @Mock
    private DeviseRepository deviseRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private DeviseService deviseService;

    private TenantEntity tenantA;
    private TenantEntity tenantB;
    private DeviseEntity deviseXOF;      // système
    private DeviseEntity deviseEUR;      // système
    private DeviseEntity devisePersoA;   // personnalisée, appartient a tenantA
    private DeviseEntity devisePersoB;   // personnalisée, appartient a tenantB
    private CreateDeviseDto createDeviseDto;

    @BeforeEach
    void setUp() {
        tenantA = TenantEntity.builder().id(10L).build();
        tenantB = TenantEntity.builder().id(20L).build();

        deviseXOF = DeviseEntity.builder()
                .id(1L).code("XOF").nom("Franc CFA").symbole("CFA").pays("Sénégal")
                .tauxChange(1.0).isDefault(true).dateCreation(LocalDateTime.now())
                .build();

        deviseEUR = DeviseEntity.builder()
                .id(2L).code("EUR").nom("Euro").symbole("€").pays("Zone Euro")
                .tauxChange(655.957).isDefault(false).dateCreation(LocalDateTime.now())
                .build();

        devisePersoA = DeviseEntity.builder()
                .id(3L).code("CAD").nom("Dollar canadien").symbole("$").pays("Canada")
                .tauxChange(480.0).isDefault(false).dateCreation(LocalDateTime.now())
                .tenant(tenantA)
                .build();

        devisePersoB = DeviseEntity.builder()
                .id(4L).code("GBP").nom("Livre sterling").symbole("£").pays("Royaume-Uni")
                .tauxChange(800.0).isDefault(false).dateCreation(LocalDateTime.now())
                .tenant(tenantB)
                .build();

        createDeviseDto = CreateDeviseDto.builder()
                .code("USD").nom("Dollar américain").symbole("$").pays("États-Unis")
                .tauxChange(600.0).isDefault(false)
                .build();
    }

    @Test
    @DisplayName("obtenirToutesLesDevises() - Devrait retourner système + devises du tenant courant")
    void obtenirToutesLesDevises_DevraitRetourner() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);
        when(deviseRepository.findVisiblesPourTenant(tenantA))
                .thenReturn(Arrays.asList(deviseXOF, deviseEUR, devisePersoA));

        List<DeviseEntity> resultat = deviseService.obtenirToutesLesDevises();

        assertThat(resultat).hasSize(3).contains(devisePersoA);
        verify(deviseRepository, times(1)).findVisiblesPourTenant(tenantA);
    }

    @Test
    @DisplayName("obtenirDeviseParId() - Une devise système est visible sans vérif de tenant")
    void obtenirDeviseParId_DevraitRetournerDeviseSysteme() {
        when(deviseRepository.findById(1L)).thenReturn(Optional.of(deviseXOF));

        DeviseEntity resultat = deviseService.obtenirDeviseParId(1L);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getCode()).isEqualTo("XOF");
        verify(tenantService, never()).getCurrentTenant();
    }

    @Test
    @DisplayName("obtenirDeviseParId() - Devrait lancer exception si non trouvée")
    void obtenirDeviseParId_DevraitLancerException() {
        when(deviseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviseService.obtenirDeviseParId(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Devise non trouvée");
    }

    @Test
    @DisplayName("obtenirDeviseParId() - Devrait masquer une devise personnalisée d'une autre boutique")
    void obtenirDeviseParId_DevraitMasquerDeviseAutreTenant() {
        when(deviseRepository.findById(4L)).thenReturn(Optional.of(devisePersoB));
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);

        assertThatThrownBy(() -> deviseService.obtenirDeviseParId(4L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Devise non trouvée");
    }

    @Test
    @DisplayName("obtenirDeviseParCode() - Devrait retourner une devise visible pour le tenant")
    void obtenirDeviseParCode_DevraitRetourner() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);
        when(deviseRepository.findByCodeVisiblePourTenant("EUR", tenantA))
                .thenReturn(List.of(deviseEUR));

        DeviseEntity resultat = deviseService.obtenirDeviseParCode("eur");

        assertThat(resultat).isNotNull();
        assertThat(resultat.getCode()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("obtenirDeviseParDefaut() - Devrait retourner la devise par défaut")
    void obtenirDeviseParDefaut_DevraitRetourner() {
        when(deviseRepository.findByIsDefaultTrue()).thenReturn(Optional.of(deviseXOF));

        DeviseEntity resultat = deviseService.obtenirDeviseParDefaut();

        assertThat(resultat).isNotNull();
        assertThat(resultat.getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("creerDevise() - Devrait créer une devise personnalisée pour le tenant courant")
    void creerDevise_DevraitCreer() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);
        when(deviseRepository.existsByCodeAndTenantIsNull("USD")).thenReturn(false);
        when(deviseRepository.existsByCodeAndTenant("USD", tenantA)).thenReturn(false);
        when(deviseRepository.save(any(DeviseEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviseEntity resultat = deviseService.creerDevise(createDeviseDto);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getTenant()).isEqualTo(tenantA);

        ArgumentCaptor<DeviseEntity> captor = ArgumentCaptor.forClass(DeviseEntity.class);
        verify(deviseRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getTenant()).isEqualTo(tenantA);
    }

    @Test
    @DisplayName("creerDevise() - Devrait refuser un code déjà pris par une devise système")
    void creerDevise_DevraitRefuserCodeSysteme() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);
        when(deviseRepository.existsByCodeAndTenantIsNull("USD")).thenReturn(true);

        assertThatThrownBy(() -> deviseService.creerDevise(createDeviseDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("devise système");
    }

    @Test
    @DisplayName("creerDevise() - Devrait refuser un code déjà pris par ce même tenant")
    void creerDevise_DevraitRefuserCodeDejaPourCeTenant() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);
        when(deviseRepository.existsByCodeAndTenantIsNull("USD")).thenReturn(false);
        when(deviseRepository.existsByCodeAndTenant("USD", tenantA)).thenReturn(true);

        assertThatThrownBy(() -> deviseService.creerDevise(createDeviseDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Vous avez déjà");
    }

    @Test
    @DisplayName("modifierDevise() - Devrait modifier sa propre devise personnalisée")
    void modifierDevise_DevraitModifier() {
        UpdateDeviseDto updateDto = UpdateDeviseDto.builder()
                .nom("Dollar canadien modifié")
                .tauxChange(490.0)
                .build();

        when(deviseRepository.findById(3L)).thenReturn(Optional.of(devisePersoA));
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);
        when(deviseRepository.save(any(DeviseEntity.class))).thenReturn(devisePersoA);

        DeviseEntity resultat = deviseService.modifierDevise(3L, updateDto);

        assertThat(resultat).isNotNull();
        verify(deviseRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("modifierDevise() - Devrait refuser de modifier une devise système")
    void modifierDevise_DevraitRefuserDeviseSysteme() {
        when(deviseRepository.findById(1L)).thenReturn(Optional.of(deviseXOF));
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);

        assertThatThrownBy(() -> deviseService.modifierDevise(1L, UpdateDeviseDto.builder().build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("système");
    }

    @Test
    @DisplayName("modifierDevise() - Devrait refuser de modifier la devise personnalisée d'une autre boutique")
    void modifierDevise_DevraitRefuserAutreTenant() {
        when(deviseRepository.findById(4L)).thenReturn(Optional.of(devisePersoB));
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);

        assertThatThrownBy(() -> deviseService.modifierDevise(4L, UpdateDeviseDto.builder().build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Devise non trouvée");
    }

    @Test
    @DisplayName("supprimerDevise() - Devrait supprimer sa propre devise personnalisée")
    void supprimerDevise_DevraitSupprimer() {
        when(deviseRepository.findById(3L)).thenReturn(Optional.of(devisePersoA));
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);

        deviseService.supprimerDevise(3L);

        verify(deviseRepository, times(1)).deleteById(3L);
    }

    @Test
    @DisplayName("supprimerDevise() - Ne devrait pas supprimer une devise système")
    void supprimerDevise_NePasSupperimerDeviseSysteme() {
        when(deviseRepository.findById(1L)).thenReturn(Optional.of(deviseXOF));
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);

        assertThatThrownBy(() -> deviseService.supprimerDevise(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("système");
    }

    @Test
    @DisplayName("definirDeviseParDefaut() - Devrait définir une devise par défaut")
    void definirDeviseParDefaut_DevraitDefinir() {
        when(deviseRepository.findById(2L)).thenReturn(Optional.of(deviseEUR));
        when(deviseRepository.findByIsDefaultTrue()).thenReturn(Optional.of(deviseXOF));
        when(deviseRepository.save(any())).thenReturn(deviseEUR);

        DeviseEntity resultat = deviseService.definirDeviseParDefaut(2L);

        assertThat(resultat).isNotNull();
        verify(deviseRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("convertir() - Devrait convertir entre devises")
    void convertir_DevraitConvertir() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantA);
        when(deviseRepository.findByCodeVisiblePourTenant("EUR", tenantA)).thenReturn(List.of(deviseEUR));
        when(deviseRepository.findByCodeVisiblePourTenant("XOF", tenantA)).thenReturn(List.of(deviseXOF));

        Double resultat = deviseService.convertir(100.0, "EUR", "XOF");

        assertThat(resultat).isNotNull();
        assertThat(resultat).isGreaterThan(0);
    }

    @Test
    @DisplayName("convertir() - Devrait retourner zéro si montant nul")
    void convertir_DevraitRetournerZeroSiMontantNul() {
        Double resultat = deviseService.convertir(0.0, "EUR", "XOF");

        assertThat(resultat).isEqualTo(0.0);
        verify(deviseRepository, never()).findByCodeVisiblePourTenant(any(), any());
    }
}
