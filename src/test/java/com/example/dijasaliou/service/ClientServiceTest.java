package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ClientDto;
import com.example.dijasaliou.entity.ClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.ClientRepository;
import com.example.dijasaliou.repository.CreditClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — ClientService")
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CreditClientRepository creditClientRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private ClientService clientService;

    private TenantEntity tenantTest;
    private ClientEntity clientTest;

    @BeforeEach
    void setUp() {
        tenantTest = TenantEntity.builder()
                .tenantUuid("uuid-boutique-test")
                .nomEntreprise("Boutique Dija")
                .numeroTelephone("778899001")
                .plan(TenantEntity.Plan.STARTER)
                .build();

        clientTest = ClientEntity.builder()
                .nom("Ousmane Ndiaye")
                .telephone("771122334")
                .detteTotale(BigDecimal.ZERO)
                .tenant(tenantTest)
                .build();
    }

    // =========================================================
    // creerClient — validations
    // =========================================================

    @Test
    @DisplayName("creerClient() — lève IllegalArgumentException si nom null")
    void creerClient_leveExceptionSiNomNull() {
        assertThatThrownBy(() -> clientService.creerClient(null, "771234567"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatoire");
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerClient() — lève IllegalArgumentException si nom blank")
    void creerClient_leveExceptionSiNomBlank() {
        assertThatThrownBy(() -> clientService.creerClient("   ", "771234567"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatoire");
        verify(clientRepository, never()).save(any());
    }

    // =========================================================
    // creerClient — succès
    // =========================================================

    @Test
    @DisplayName("creerClient() — sauvegarde le client et retourne le DTO")
    void creerClient_succesRetourneDto() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(clientRepository.save(any())).thenReturn(clientTest);

        ClientDto dto = clientService.creerClient("Ousmane Ndiaye", "771122334");

        assertThat(dto).isNotNull();
        assertThat(dto.getNom()).isEqualTo("Ousmane Ndiaye");
        verify(clientRepository).save(any(ClientEntity.class));
    }

    @Test
    @DisplayName("creerClient() — nom trimmé avant sauvegarde")
    void creerClient_nomTrimmé() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);

        ArgumentCaptor<ClientEntity> captor = ArgumentCaptor.forClass(ClientEntity.class);
        when(clientRepository.save(captor.capture())).thenReturn(clientTest);

        clientService.creerClient("  Moussa Fall  ", "770000000");

        assertThat(captor.getValue().getNom()).isEqualTo("Moussa Fall");
    }

    // =========================================================
    // rechercherClients — paramètre search
    // =========================================================

    @Test
    @DisplayName("rechercherClients() — search null → searchParam null passé au repo")
    void rechercherClients_searchNullPasseNull() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(clientRepository.findAllWithSearch(isNull(), eq("uuid-boutique-test")))
                .thenReturn(List.of(clientTest));
        when(creditClientRepository.countCreditsActifsByClientIds(any(), any(), any())).thenReturn(List.of());

        List<ClientDto> result = clientService.rechercherClients(null);

        assertThat(result).hasSize(1);
        verify(clientRepository).findAllWithSearch(isNull(), eq("uuid-boutique-test"));
    }

    @Test
    @DisplayName("rechercherClients() — search non vide → passé tel quel au repo")
    void rechercherClients_searchNonVidePasséAuRepo() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(clientRepository.findAllWithSearch(eq("Ousmane"), eq("uuid-boutique-test")))
                .thenReturn(List.of(clientTest));
        when(creditClientRepository.countCreditsActifsByClientIds(any(), any(), any())).thenReturn(List.of());

        List<ClientDto> result = clientService.rechercherClients("Ousmane");

        assertThat(result).hasSize(1);
        verify(clientRepository).findAllWithSearch(eq("Ousmane"), eq("uuid-boutique-test"));
    }

    // =========================================================
    // obtenirClientParId
    // =========================================================

    @Test
    @DisplayName("obtenirClientParId() — retourne le DTO si client trouvé")
    void obtenirClientParId_retourneDtoSiTrouvé() {
        when(clientRepository.findById("test-id-1")).thenReturn(Optional.of(clientTest));
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(creditClientRepository.countCreditsActifsByClientId(any(), any(), any())).thenReturn(2L);

        ClientDto dto = clientService.obtenirClientParId("test-id-1");

        assertThat(dto).isNotNull();
        assertThat(dto.getNom()).isEqualTo("Ousmane Ndiaye");
        assertThat(dto.getNombreCreditsActifs()).isEqualTo(2L);
    }

    @Test
    @DisplayName("obtenirClientParId() — lève RuntimeException si client non trouvé")
    void obtenirClientParId_leveExceptionSiAbsent() {
        when(clientRepository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.obtenirClientParId("99"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }
}
