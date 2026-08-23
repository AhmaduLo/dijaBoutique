package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ActiverCaisseRequest;
import com.example.dijasaliou.dto.CaisseSoldeDto;
import com.example.dijasaliou.entity.CaisseConfigEntity;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — CaisseService.activerCaisse")
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
}
