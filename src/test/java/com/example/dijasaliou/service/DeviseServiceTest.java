package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.CreateDeviseDto;
import com.example.dijasaliou.dto.UpdateDeviseDto;
import com.example.dijasaliou.entity.DeviseEntity;
import com.example.dijasaliou.repository.DeviseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @InjectMocks
    private DeviseService deviseService;

    private DeviseEntity deviseXOF;
    private DeviseEntity deviseEUR;
    private CreateDeviseDto createDeviseDto;

    @BeforeEach
    void setUp() {
        deviseXOF = DeviseEntity.builder()
                .id(1L)
                .code("XOF")
                .nom("Franc CFA")
                .symbole("CFA")
                .pays("Sénégal")
                .tauxChange(1.0)
                .isDefault(true)
                .dateCreation(LocalDateTime.now())
                .build();

        deviseEUR = DeviseEntity.builder()
                .id(2L)
                .code("EUR")
                .nom("Euro")
                .symbole("€")
                .pays("Zone Euro")
                .tauxChange(655.957)
                .isDefault(false)
                .dateCreation(LocalDateTime.now())
                .build();

        createDeviseDto = CreateDeviseDto.builder()
                .code("USD")
                .nom("Dollar américain")
                .symbole("$")
                .pays("États-Unis")
                .tauxChange(600.0)
                .isDefault(false)
                .build();
    }

    @Test
    @DisplayName("obtenirToutesLesDevises() - Devrait retourner toutes les devises")
    void obtenirToutesLesDevises_DevraitRetourner() {
        when(deviseRepository.findAll()).thenReturn(Arrays.asList(deviseXOF, deviseEUR));

        List<DeviseEntity> resultat = deviseService.obtenirToutesLesDevises();

        assertThat(resultat).hasSize(2);
        verify(deviseRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("obtenirDeviseParId() - Devrait retourner une devise")
    void obtenirDeviseParId_DevraitRetourner() {
        when(deviseRepository.findById(1L)).thenReturn(Optional.of(deviseXOF));

        DeviseEntity resultat = deviseService.obtenirDeviseParId(1L);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getCode()).isEqualTo("XOF");
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
    @DisplayName("obtenirDeviseParCode() - Devrait retourner une devise par code")
    void obtenirDeviseParCode_DevraitRetourner() {
        when(deviseRepository.findByCode("EUR")).thenReturn(Optional.of(deviseEUR));

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
    @DisplayName("creerDevise() - Devrait créer une nouvelle devise")
    void creerDevise_DevraitCreer() {
        when(deviseRepository.existsByCode("USD")).thenReturn(false);
        when(deviseRepository.existsByIsDefaultTrue()).thenReturn(true);
        when(deviseRepository.save(any(DeviseEntity.class))).thenReturn(deviseEUR);

        DeviseEntity resultat = deviseService.creerDevise(createDeviseDto);

        assertThat(resultat).isNotNull();
        verify(deviseRepository, times(1)).save(any(DeviseEntity.class));
    }

    @Test
    @DisplayName("creerDevise() - Devrait lancer exception si code existe déjà")
    void creerDevise_DevraitLancerExceptionCodeExiste() {
        when(deviseRepository.existsByCode("USD")).thenReturn(true);

        assertThatThrownBy(() -> deviseService.creerDevise(createDeviseDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    @DisplayName("modifierDevise() - Devrait modifier une devise")
    void modifierDevise_DevraitModifier() {
        UpdateDeviseDto updateDto = UpdateDeviseDto.builder()
                .nom("Euro modifié")
                .tauxChange(660.0)
                .build();

        when(deviseRepository.findById(2L)).thenReturn(Optional.of(deviseEUR));
        when(deviseRepository.save(any(DeviseEntity.class))).thenReturn(deviseEUR);

        DeviseEntity resultat = deviseService.modifierDevise(2L, updateDto);

        assertThat(resultat).isNotNull();
        verify(deviseRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("supprimerDevise() - Devrait supprimer une devise")
    void supprimerDevise_DevraitSupprimer() {
        when(deviseRepository.findById(2L)).thenReturn(Optional.of(deviseEUR));

        deviseService.supprimerDevise(2L);

        verify(deviseRepository, times(1)).deleteById(2L);
    }

    @Test
    @DisplayName("supprimerDevise() - Ne devrait pas supprimer la devise par défaut")
    void supprimerDevise_NePasSupperimerDeviseParDefaut() {
        when(deviseRepository.findById(1L)).thenReturn(Optional.of(deviseXOF));

        assertThatThrownBy(() -> deviseService.supprimerDevise(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("devise par défaut");
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
        when(deviseRepository.findByCode("EUR")).thenReturn(Optional.of(deviseEUR));
        when(deviseRepository.findByCode("XOF")).thenReturn(Optional.of(deviseXOF));

        Double resultat = deviseService.convertir(100.0, "EUR", "XOF");

        assertThat(resultat).isNotNull();
        assertThat(resultat).isGreaterThan(0);
    }

    @Test
    @DisplayName("convertir() - Devrait retourner zéro si montant nul")
    void convertir_DevraitRetournerZeroSiMontantNul() {
        Double resultat = deviseService.convertir(0.0, "EUR", "XOF");

        assertThat(resultat).isEqualTo(0.0);
        verify(deviseRepository, never()).findByCode(any());
    }
}
