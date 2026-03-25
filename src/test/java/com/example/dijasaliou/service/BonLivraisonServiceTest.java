package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.BonLivraisonDto;
import com.example.dijasaliou.dto.CreateBonLivraisonRequest;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.entity.BonLivraisonEntity;
import com.example.dijasaliou.entity.LigneBLEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.BonLivraisonRepository;
import com.example.dijasaliou.repository.UserRepository;
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
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — BonLivraisonService")
class BonLivraisonServiceTest {

    @Mock
    private BonLivraisonRepository bonLivraisonRepository;

    @Mock
    private TenantService tenantService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BonLivraisonService bonLivraisonService;

    private TenantEntity tenantTest;
    private BonLivraisonEntity blTest;

    @BeforeEach
    void setUp() {
        tenantTest = TenantEntity.builder()
                .tenantUuid("uuid-tenant-test")
                .nomEntreprise("Boutique Test")
                .numeroTelephone("771234567")
                .plan(TenantEntity.Plan.STARTER)
                .build();

        blTest = BonLivraisonEntity.builder()
                .numeroBL("BL-202603-0001")
                .tenant(tenantTest)
                .clientNom("Fatou Diallo")
                .adresseLivraison("Rue 10, Dakar")
                .telephoneClient("770000001")
                .statut(BonLivraisonEntity.Statut.EN_ATTENTE)
                .lignes(new ArrayList<>())
                .build();
    }

    // =========================================================
    // genererNumeroBL — format BL-YYYYMM-XXXX
    // =========================================================

    @Test
    @DisplayName("creer() — le numéro BL suit le format BL-YYYYMM-XXXX")
    void creer_numeroBLSuitFormatAttendu() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(bonLivraisonRepository.findMaxSequenceForPrefix(anyString())).thenReturn(0);
        when(bonLivraisonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findFirstByTenantAndRole(any(), any())).thenReturn(Optional.empty());

        CreateBonLivraisonRequest request = buildRequest("Amadou Sy", "Pikine", null);

        BonLivraisonDto dto = bonLivraisonService.creer(request);

        String expectedPrefix = "BL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        assertThat(dto.getNumeroBL())
                .startsWith(expectedPrefix)
                .matches("BL-\\d{6}-\\d{4}");
    }

    @Test
    @DisplayName("genererNumeroBL() — le compteur s'incrémente correctement (ex: 0001, 0005)")
    void creer_numeroBLIncrémentéCorrectement() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(bonLivraisonRepository.findMaxSequenceForPrefix(anyString())).thenReturn(4);
        when(bonLivraisonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findFirstByTenantAndRole(any(), any())).thenReturn(Optional.empty());

        CreateBonLivraisonRequest request = buildRequest("Client X", "Adresse Y", null);

        BonLivraisonDto dto = bonLivraisonService.creer(request);

        assertThat(dto.getNumeroBL()).endsWith("-0005");
    }

    // =========================================================
    // creer — statut EN_ATTENTE, lignes, unite par défaut "pièce"
    // =========================================================

    @Test
    @DisplayName("creer() — statut EN_ATTENTE et tenant associé")
    void creer_statutEnAttenteEtTenantAssocie() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(bonLivraisonRepository.findMaxSequenceForPrefix(anyString())).thenReturn(0);

        ArgumentCaptor<BonLivraisonEntity> captor = ArgumentCaptor.forClass(BonLivraisonEntity.class);
        when(bonLivraisonRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findFirstByTenantAndRole(any(), any())).thenReturn(Optional.empty());

        CreateBonLivraisonRequest request = buildRequest("Mariama Ba", "HLM Dakar", null);

        bonLivraisonService.creer(request);

        BonLivraisonEntity saved = captor.getValue();
        assertThat(saved.getStatut()).isEqualTo(BonLivraisonEntity.Statut.EN_ATTENTE);
        assertThat(saved.getTenant()).isEqualTo(tenantTest);
        assertThat(saved.getClientNom()).isEqualTo("Mariama Ba");
    }

    @Test
    @DisplayName("creer() — unite null dans la requête → 'pièce' par défaut")
    void creer_unitePieceParDefautSiNull() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(bonLivraisonRepository.findMaxSequenceForPrefix(anyString())).thenReturn(0);

        ArgumentCaptor<BonLivraisonEntity> captor = ArgumentCaptor.forClass(BonLivraisonEntity.class);
        when(bonLivraisonRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findFirstByTenantAndRole(any(), any())).thenReturn(Optional.empty());

        // ligne sans unite
        CreateBonLivraisonRequest.LigneBLRequest ligne = new CreateBonLivraisonRequest.LigneBLRequest();
        ligne.setNomProduit("Riz");
        ligne.setQuantite(5.0);
        ligne.setUnite(null);

        CreateBonLivraisonRequest request = new CreateBonLivraisonRequest();
        request.setClientNom("Ibou Fall");
        request.setAdresseLivraison("Medina, Dakar");
        request.setLignes(List.of(ligne));

        bonLivraisonService.creer(request);

        BonLivraisonEntity saved = captor.getValue();
        assertThat(saved.getLignes()).hasSize(1);
        assertThat(saved.getLignes().get(0).getUnite()).isEqualTo("pièce");
    }

    @Test
    @DisplayName("creer() — unite fournie dans la requête → conservée")
    void creer_uniteConservéeSiRenseignée() {
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
        when(bonLivraisonRepository.findMaxSequenceForPrefix(anyString())).thenReturn(0);

        ArgumentCaptor<BonLivraisonEntity> captor = ArgumentCaptor.forClass(BonLivraisonEntity.class);
        when(bonLivraisonRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findFirstByTenantAndRole(any(), any())).thenReturn(Optional.empty());

        CreateBonLivraisonRequest.LigneBLRequest ligne = new CreateBonLivraisonRequest.LigneBLRequest();
        ligne.setNomProduit("Sucre");
        ligne.setQuantite(2.0);
        ligne.setUnite("kg");

        CreateBonLivraisonRequest request = new CreateBonLivraisonRequest();
        request.setClientNom("Diallo");
        request.setAdresseLivraison("Grand-Yoff");
        request.setLignes(List.of(ligne));

        bonLivraisonService.creer(request);

        BonLivraisonEntity saved = captor.getValue();
        assertThat(saved.getLignes().get(0).getUnite()).isEqualTo("kg");
    }

    // =========================================================
    // marquerLivre
    // =========================================================

    @Test
    @DisplayName("marquerLivre() — statut LIVRE et dateLivraisonEffective renseignée")
    void marquerLivre_statutLivreEtDateEffective() {
        when(bonLivraisonRepository.findById("test-id-1")).thenReturn(Optional.of(blTest));
        when(bonLivraisonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findFirstByTenantAndRole(any(), any())).thenReturn(Optional.empty());

        BonLivraisonDto dto = bonLivraisonService.marquerLivre("test-id-1");

        assertThat(blTest.getStatut()).isEqualTo(BonLivraisonEntity.Statut.LIVRE);
        assertThat(blTest.getDateLivraisonEffective()).isNotNull();
        assertThat(dto.getStatut()).isEqualTo("LIVRE");
    }

    @Test
    @DisplayName("marquerLivre() — lève RuntimeException si BL non trouvé")
    void marquerLivre_leveExceptionSiAbsent() {
        when(bonLivraisonRepository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bonLivraisonService.marquerLivre("99"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // =========================================================
    // annuler
    // =========================================================

    @Test
    @DisplayName("annuler() — statut ANNULE")
    void annuler_statutAnnule() {
        when(bonLivraisonRepository.findById("test-id-1")).thenReturn(Optional.of(blTest));
        when(bonLivraisonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findFirstByTenantAndRole(any(), any())).thenReturn(Optional.empty());

        BonLivraisonDto dto = bonLivraisonService.annuler("test-id-1");

        assertThat(blTest.getStatut()).isEqualTo(BonLivraisonEntity.Statut.ANNULE);
        assertThat(dto.getStatut()).isEqualTo("ANNULE");
    }

    @Test
    @DisplayName("annuler() — lève RuntimeException si BL non trouvé")
    void annuler_leveExceptionSiAbsent() {
        when(bonLivraisonRepository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bonLivraisonService.annuler("99"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // =========================================================
    // supprimer
    // =========================================================

    @Test
    @DisplayName("supprimer() — appelle delete sur le repository")
    void supprimer_appelleDelete() {
        when(bonLivraisonRepository.findById("test-id-1")).thenReturn(Optional.of(blTest));

        bonLivraisonService.supprimer("test-id-1");

        verify(bonLivraisonRepository).delete(blTest);
    }

    @Test
    @DisplayName("supprimer() — lève RuntimeException si BL non trouvé")
    void supprimer_leveExceptionSiAbsent() {
        when(bonLivraisonRepository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bonLivraisonService.supprimer("99"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
        verify(bonLivraisonRepository, never()).delete(any());
    }

    // =========================================================
    // getParId
    // =========================================================

    @Test
    @DisplayName("getParId() — retourne le DTO si BL trouvé")
    void getParId_retourneDtoSiTrouvé() {
        when(bonLivraisonRepository.findById("test-id-1")).thenReturn(Optional.of(blTest));
        when(userRepository.findFirstByTenantAndRole(any(), any())).thenReturn(Optional.empty());

        BonLivraisonDto dto = bonLivraisonService.getParId("test-id-1");

        assertThat(dto).isNotNull();
        assertThat(dto.getClientNom()).isEqualTo("Fatou Diallo");
    }

    @Test
    @DisplayName("getParId() — lève RuntimeException si BL non trouvé")
    void getParId_leveExceptionSiAbsent() {
        when(bonLivraisonRepository.findById("55")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bonLivraisonService.getParId("55"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("55");
    }

    // =========================================================
    // getTousPagines — filtrage statut
    // =========================================================

    @Test
    @DisplayName("getTousPagines() — statut 'TOUS' → statutParam null passé au repo")
    void getTousPagines_statutTousPasseNull() {
        Page<BonLivraisonEntity> page = new PageImpl<>(Collections.emptyList());
        when(bonLivraisonRepository.findAllWithSearch(isNull(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<BonLivraisonDto> result = bonLivraisonService.getTousPagines(0, 10, null, "TOUS", null, null);

        assertThat(result).isNotNull();
        verify(bonLivraisonRepository).findAllWithSearch(isNull(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("getTousPagines() — statut invalide → statutParam null passé au repo")
    void getTousPagines_statutInvalidePasseNull() {
        Page<BonLivraisonEntity> page = new PageImpl<>(Collections.emptyList());
        when(bonLivraisonRepository.findAllWithSearch(isNull(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<BonLivraisonDto> result = bonLivraisonService.getTousPagines(0, 10, null, "INCONNU", null, null);

        assertThat(result).isNotNull();
        verify(bonLivraisonRepository).findAllWithSearch(isNull(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("getTousPagines() — statut valide 'EN_ATTENTE' → mappé et passé au repo")
    void getTousPagines_statutValidePasséAuRepo() {
        Page<BonLivraisonEntity> page = new PageImpl<>(List.of(blTest));
        when(bonLivraisonRepository.findAllWithSearch(
                eq(BonLivraisonEntity.Statut.EN_ATTENTE), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(userRepository.findFirstByTenantAndRole(any(), any())).thenReturn(Optional.empty());

        PagedResponse<BonLivraisonDto> result = bonLivraisonService.getTousPagines(0, 10, null, "EN_ATTENTE", null, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(bonLivraisonRepository).findAllWithSearch(
                eq(BonLivraisonEntity.Statut.EN_ATTENTE), any(), any(), any(), any(Pageable.class));
    }

    // =========================================================
    // Méthode utilitaire
    // =========================================================

    private CreateBonLivraisonRequest buildRequest(String clientNom, String adresse, String unite) {
        CreateBonLivraisonRequest.LigneBLRequest ligne = new CreateBonLivraisonRequest.LigneBLRequest();
        ligne.setNomProduit("Produit Test");
        ligne.setQuantite(3.0);
        ligne.setUnite(unite);

        CreateBonLivraisonRequest request = new CreateBonLivraisonRequest();
        request.setClientNom(clientNom);
        request.setAdresseLivraison(adresse);
        request.setLignes(List.of(ligne));
        return request;
    }
}
