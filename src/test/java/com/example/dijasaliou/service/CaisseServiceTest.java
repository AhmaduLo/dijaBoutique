package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ActiverCaisseRequest;
import com.example.dijasaliou.dto.CaisseSoldeDto;
import com.example.dijasaliou.entity.CaisseConfigEntity;
import com.example.dijasaliou.entity.CompteCaisse;
import com.example.dijasaliou.entity.DeviseEntity;
import com.example.dijasaliou.entity.MouvementCaisseManuelEntity.TypeMouvement;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — CaisseService")
class CaisseServiceTest {

    @Mock private CaisseConfigRepository caisseConfigRepository;
    @Mock private TransfertCaisseRepository transfertRepository;
    @Mock private MouvementCaisseManuelRepository mouvementManuelRepository;
    @Mock private AchatRepository achatRepository;
    @Mock private VenteRepository venteRepository;
    @Mock private DepenseRepository depenseRepository;
    @Mock private PaiementCreditRepository paiementCreditRepository;
    @Mock private TenantService tenantService;
    @Mock private UserRepository userRepository;
    @Mock private UserPushNotificationService userPushService;
    @Mock private UserNotificationPreferenceService prefService;
    @Mock private DeviseService deviseService;

    @InjectMocks
    private CaisseService caisseService;

    private TenantEntity tenantTest;

    @BeforeEach
    void setUp() {
        tenantTest = new TenantEntity();
        tenantTest.setTenantUuid("uuid-tenant-test");
        when(tenantService.getCurrentTenant()).thenReturn(tenantTest);
    }

    @Test
    @DisplayName("1ère activation (pas de config existante) — dateActivation = now si non fournie")
    void activerCaisse_premiereActivation_dateActivationDefautNow() {
        when(caisseConfigRepository.findByTenant(tenantTest)).thenReturn(Optional.empty());
        when(caisseConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime now = LocalDateTime.of(2026, 8, 23, 10, 0);
        when(tenantService.nowInTenantTz()).thenReturn(now);

        ActiverCaisseRequest request = new ActiverCaisseRequest();
        request.setSoldeInitialEspeces(new BigDecimal("200000"));
        request.setSoldeInitialWave(BigDecimal.ZERO);
        request.setSoldeInitialOm(BigDecimal.ZERO);
        request.setSoldeInitialVirement(BigDecimal.ZERO);
        // dateActivation non fournie par le frontend

        caisseService.activerCaisse(request, "user-uuid");

        ArgumentCaptor<CaisseConfigEntity> captor = ArgumentCaptor.forClass(CaisseConfigEntity.class);
        verify(caisseConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getDateActivation()).isEqualTo(now);
    }

    @Test
    @DisplayName("Caisse déjà active, correction des soldes sans dateActivation fournie — date existante préservée")
    void activerCaisse_correctionSurCaisseActive_preserveDateActivation() {
        LocalDateTime dateActivationOriginale = LocalDateTime.of(2026, 8, 11, 9, 0);
        CaisseConfigEntity configExistante = CaisseConfigEntity.builder()
                .tenant(tenantTest)
                .soldeInitialEspeces(new BigDecimal("200000"))
                .soldeInitialWave(new BigDecimal("50000"))
                .soldeInitialOm(new BigDecimal("150000"))
                .soldeInitialVirement(BigDecimal.ZERO)
                .dateActivation(dateActivationOriginale)
                .build();
        when(caisseConfigRepository.findByTenant(tenantTest)).thenReturn(Optional.of(configExistante));
        when(caisseConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActiverCaisseRequest request = new ActiverCaisseRequest();
        request.setSoldeInitialEspeces(new BigDecimal("200000"));
        request.setSoldeInitialWave(new BigDecimal("50000"));
        request.setSoldeInitialOm(new BigDecimal("150000"));
        request.setSoldeInitialVirement(BigDecimal.ZERO);
        // dateActivation non fournie (comportement du frontend corrigé pour une caisse déjà active)

        CaisseSoldeDto resultat = caisseService.activerCaisse(request, "user-uuid");

        ArgumentCaptor<CaisseConfigEntity> captor = ArgumentCaptor.forClass(CaisseConfigEntity.class);
        verify(caisseConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getDateActivation()).isEqualTo(dateActivationOriginale);
        assertThat(resultat.getDateActivation()).isEqualTo(dateActivationOriginale);
        verify(tenantService, never()).nowInTenantTz();
    }

    @Test
    @DisplayName("Correction du solde vers une cible actuelle — recalcule le solde initial sans toucher a l'historique")
    void activerCaisse_correctionVersSoldeActuel_retroCalculeSoldeInitial() {
        LocalDateTime dateActivationOriginale = LocalDateTime.of(2026, 8, 11, 9, 0);
        CaisseConfigEntity configExistante = CaisseConfigEntity.builder()
                .tenant(tenantTest)
                .soldeInitialEspeces(new BigDecimal("200000"))
                .soldeInitialWave(BigDecimal.ZERO)
                .soldeInitialOm(BigDecimal.ZERO)
                .soldeInitialVirement(BigDecimal.ZERO)
                .dateActivation(dateActivationOriginale)
                .build();
        when(caisseConfigRepository.findByTenant(tenantTest)).thenReturn(Optional.of(configExistante));
        when(caisseConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Un mouvement manuel de +50 000 CFA a déjà été enregistré depuis l'activation :
        // solde actuel réel = 200 000 (initial) + 50 000 (mouvement) = 250 000 CFA.
        when(mouvementManuelRepository.sumByCompteAndTypeGrouped(eq(tenantTest), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{CompteCaisse.ESPECES, TypeMouvement.ENTREE, new BigDecimal("50000")}));

        // L'utilisateur corrige le formulaire (pré-rempli avec 250 000) vers 300 000 CFA
        // — ex: un comptage physique a trouvé 50 000 CFA de plus que prévu.
        ActiverCaisseRequest request = new ActiverCaisseRequest();
        request.setSoldeInitialEspeces(new BigDecimal("300000"));
        request.setSoldeInitialWave(BigDecimal.ZERO);
        request.setSoldeInitialOm(BigDecimal.ZERO);
        request.setSoldeInitialVirement(BigDecimal.ZERO);
        request.setDevise("XOF");

        CaisseSoldeDto resultat = caisseService.activerCaisse(request, "user-uuid");

        ArgumentCaptor<CaisseConfigEntity> captor = ArgumentCaptor.forClass(CaisseConfigEntity.class);
        verify(caisseConfigRepository).save(captor.capture());
        // dateActivation et le mouvement manuel déjà enregistré restent intacts...
        assertThat(captor.getValue().getDateActivation()).isEqualTo(dateActivationOriginale);
        // ...mais le solde initial est retro-calculé : 300 000 (cible) - 50 000 (mouvement) = 250 000
        assertThat(captor.getValue().getSoldeInitialEspeces()).isEqualByComparingTo(new BigDecimal("250000"));
        // Le total recalculé correspond exactement à la cible saisie par l'utilisateur.
        assertThat(resultat.getSoldeEspeces()).isEqualByComparingTo(new BigDecimal("300000.00"));
    }

    @Test
    @DisplayName("dateActivation fournie explicitement — toujours respectée (réinitialisation volontaire)")
    void activerCaisse_dateActivationExplicite_respectee() {
        LocalDateTime dateActivationOriginale = LocalDateTime.of(2026, 8, 11, 9, 0);
        CaisseConfigEntity configExistante = CaisseConfigEntity.builder()
                .tenant(tenantTest)
                .soldeInitialEspeces(new BigDecimal("200000"))
                .soldeInitialWave(BigDecimal.ZERO)
                .soldeInitialOm(BigDecimal.ZERO)
                .soldeInitialVirement(BigDecimal.ZERO)
                .dateActivation(dateActivationOriginale)
                .build();
        when(caisseConfigRepository.findByTenant(tenantTest)).thenReturn(Optional.of(configExistante));
        when(caisseConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime nouvelleDate = LocalDateTime.of(2026, 8, 23, 14, 30);
        ActiverCaisseRequest request = new ActiverCaisseRequest();
        request.setSoldeInitialEspeces(new BigDecimal("500000"));
        request.setSoldeInitialWave(BigDecimal.ZERO);
        request.setSoldeInitialOm(BigDecimal.ZERO);
        request.setSoldeInitialVirement(BigDecimal.ZERO);
        request.setDateActivation(nouvelleDate);

        caisseService.activerCaisse(request, "user-uuid");

        ArgumentCaptor<CaisseConfigEntity> captor = ArgumentCaptor.forClass(CaisseConfigEntity.class);
        verify(caisseConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getDateActivation()).isEqualTo(nouvelleDate);
    }

    @Test
    @DisplayName("getSoldeAt() — un mouvement manuel doit être réellement converti dans la devise du rapport, pas affiché brut")
    void getSoldeAt_mouvementManuel_convertiCorrectementEnDeviseCible() {
        tenantTest.setDevisePreferee("EUR");
        LocalDateTime dateActivation = LocalDateTime.of(2026, 8, 1, 0, 0);
        CaisseConfigEntity config = CaisseConfigEntity.builder()
                .tenant(tenantTest)
                .soldeInitialEspeces(new BigDecimal("200000"))
                .soldeInitialWave(BigDecimal.ZERO)
                .soldeInitialOm(BigDecimal.ZERO)
                .soldeInitialVirement(BigDecimal.ZERO)
                .dateActivation(dateActivation)
                .build();
        when(caisseConfigRepository.findByTenant(tenantTest)).thenReturn(Optional.of(config));

        DeviseEntity eur = DeviseEntity.builder().code("EUR").tauxChange(655.957).build();
        when(deviseService.obtenirDeviseParCode("EUR")).thenReturn(eur);

        // +50 000 CFA (référence fixe XOF, comme le solde initial — voir le modal "Mouvement manuel")
        when(mouvementManuelRepository.sumByCompteAndTypeGrouped(eq(tenantTest), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{CompteCaisse.ESPECES, TypeMouvement.ENTREE, new BigDecimal("50000")}));

        CaisseSoldeDto resultat = caisseService.getSoldeAt(null, "EUR");

        // (200 000 + 50 000) / 655.957 ≈ 381.12 € — pas 50 305 € (bug : mouvement affiché brut, non converti)
        assertThat(resultat.getSoldeEspeces()).isEqualByComparingTo(new BigDecimal("381.12"));
    }
}
