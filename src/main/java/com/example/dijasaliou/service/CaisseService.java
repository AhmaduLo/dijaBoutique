package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ActiverCaisseRequest;
import com.example.dijasaliou.dto.CaisseSoldeDto;
import com.example.dijasaliou.dto.MouvementCaisseRequest;
import com.example.dijasaliou.dto.TransfertCaisseRequest;
import com.example.dijasaliou.entity.*;
import com.example.dijasaliou.entity.MouvementCaisseManuelEntity.TypeMouvement;
import com.example.dijasaliou.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service du module Caisse multi-comptes (BUSINESS).
 *
 * Responsable :
 *   - Activation de la caisse (1ère fois)
 *   - Calcul en temps réel des soldes par compte
 *   - Création des transferts entre comptes
 *   - Création des mouvements manuels (entrée/sortie)
 *
 * Le solde n'est JAMAIS stocké — toujours calculé à partir :
 *   solde_initial
 *   + ventes en mode = compte (depuis date_activation)
 *   - achats en mode = compte
 *   - dépenses en mode = compte
 *   + transferts entrants
 *   - transferts sortants
 *   + mouvements manuels ENTREE
 *   - mouvements manuels SORTIE
 *
 * Le mode VIREMENT (bancaire) n'impacte AUCUN compte de caisse.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaisseService {

    private final CaisseConfigRepository              caisseConfigRepository;
    private final TransfertCaisseRepository           transfertRepository;
    private final MouvementCaisseManuelRepository     mouvementManuelRepository;
    private final AchatRepository                     achatRepository;
    private final VenteRepository                     venteRepository;
    private final DepenseRepository                   depenseRepository;
    private final TenantService                       tenantService;

    // ── ACTIVATION ───────────────────────────────────────────────────────────

    /**
     * Active la caisse pour le tenant courant. Idempotent : si déjà active,
     * met à jour les soldes initiaux et la date d'activation (réinitialisation).
     */
    @Transactional
    public CaisseSoldeDto activerCaisse(ActiverCaisseRequest request, String userUuid) {
        TenantEntity tenant = tenantService.getCurrentTenant();

        CaisseConfigEntity config = caisseConfigRepository.findByTenant(tenant)
                .orElseGet(CaisseConfigEntity::new);

        config.setTenant(tenant);
        config.setSoldeInitialEspeces(request.getSoldeInitialEspeces());
        config.setSoldeInitialWave(request.getSoldeInitialWave());
        config.setSoldeInitialOm(request.getSoldeInitialOm());
        config.setDateActivation(LocalDateTime.now());
        config.setActivePar(userUuid);

        caisseConfigRepository.save(config);
        log.info("Caisse activée pour tenant={} : Espèces={}, Wave={}, OM={}",
                tenant.getTenantUuid(),
                request.getSoldeInitialEspeces(),
                request.getSoldeInitialWave(),
                request.getSoldeInitialOm());

        return calculerSolde(tenant, config);
    }

    // ── CALCUL DU SOLDE ──────────────────────────────────────────────────────

    /**
     * Calcule le solde actuel par compte + total. Si la caisse n'est pas activée,
     * retourne un DTO avec active = false.
     */
    @Transactional(readOnly = true)
    public CaisseSoldeDto getSoldeActuel() {
        TenantEntity tenant = tenantService.getCurrentTenant();

        return caisseConfigRepository.findByTenant(tenant)
                .map(config -> calculerSolde(tenant, config))
                .orElseGet(() -> CaisseSoldeDto.builder()
                        .active(false)
                        .build());
    }

    private CaisseSoldeDto calculerSolde(TenantEntity tenant, CaisseConfigEntity config) {
        LocalDateTime debut = config.getDateActivation();

        BigDecimal soldeEspeces = calculerSoldeCompte(tenant, CompteCaisse.ESPECES, debut,
                config.getSoldeInitialEspeces());
        BigDecimal soldeWave    = calculerSoldeCompte(tenant, CompteCaisse.WAVE, debut,
                config.getSoldeInitialWave());
        BigDecimal soldeOm      = calculerSoldeCompte(tenant, CompteCaisse.ORANGE_MONEY, debut,
                config.getSoldeInitialOm());

        BigDecimal soldeTotal = soldeEspeces.add(soldeWave).add(soldeOm);

        return CaisseSoldeDto.builder()
                .active(true)
                .dateActivation(debut)
                .soldeEspeces(soldeEspeces)
                .soldeWave(soldeWave)
                .soldeOm(soldeOm)
                .soldeTotal(soldeTotal)
                .soldeInitialEspeces(config.getSoldeInitialEspeces())
                .soldeInitialWave(config.getSoldeInitialWave())
                .soldeInitialOm(config.getSoldeInitialOm())
                .build();
    }

    /**
     * Calcule le solde d'un compte précis depuis la date d'activation.
     */
    private BigDecimal calculerSoldeCompte(TenantEntity tenant, CompteCaisse compte,
                                            LocalDateTime debut, BigDecimal soldeInitial) {
        ModePaiementCaisse modePaiement = compteToModePaiement(compte);
        VenteEntity.ModePaiementVente modeVente = compteToModeVente(compte);

        BigDecimal entreesVentes      = venteRepository.sumByModePaiementSince(tenant, modeVente, debut);
        BigDecimal sortiesAchats      = achatRepository.sumByModePaiementSince(tenant, modePaiement, debut);
        BigDecimal sortiesDepenses    = depenseRepository.sumByModePaiementSince(tenant, modePaiement, debut);
        BigDecimal transfertsEntrants = transfertRepository.sumEntreesByCompteSince(tenant, compte, debut);
        BigDecimal transfertsSortants = transfertRepository.sumSortiesByCompteSince(tenant, compte, debut);
        BigDecimal entreesManuelles   = mouvementManuelRepository.sumByCompteAndTypeSince(
                tenant, compte, TypeMouvement.ENTREE, debut);
        BigDecimal sortiesManuelles   = mouvementManuelRepository.sumByCompteAndTypeSince(
                tenant, compte, TypeMouvement.SORTIE, debut);

        return nz(soldeInitial)
                .add(nz(entreesVentes))
                .subtract(nz(sortiesAchats))
                .subtract(nz(sortiesDepenses))
                .add(nz(transfertsEntrants))
                .subtract(nz(transfertsSortants))
                .add(nz(entreesManuelles))
                .subtract(nz(sortiesManuelles));
    }

    // ── TRANSFERTS ───────────────────────────────────────────────────────────

    @Transactional
    public CaisseSoldeDto creerTransfert(TransfertCaisseRequest request, String userUuid) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        verifierCaisseActive(tenant);

        if (request.getCompteSource().equals(request.getCompteDestination())) {
            throw new IllegalArgumentException("Le compte source et destination doivent être différents");
        }

        TransfertCaisseEntity transfert = TransfertCaisseEntity.builder()
                .tenant(tenant)
                .compteSource(request.getCompteSource())
                .compteDestination(request.getCompteDestination())
                .montant(request.getMontant())
                .motif(request.getMotif())
                .dateTransfert(LocalDateTime.now())
                .faitPar(userUuid)
                .build();

        transfertRepository.save(transfert);
        log.info("Transfert créé : {} → {} pour {} (tenant={})",
                request.getCompteSource(), request.getCompteDestination(),
                request.getMontant(), tenant.getTenantUuid());

        return getSoldeActuel();
    }

    // ── MOUVEMENTS MANUELS ────────────────────────────────────────────────────

    @Transactional
    public CaisseSoldeDto creerMouvementManuel(MouvementCaisseRequest request, String userUuid) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        verifierCaisseActive(tenant);

        MouvementCaisseManuelEntity mouvement = MouvementCaisseManuelEntity.builder()
                .tenant(tenant)
                .typeMouvement(request.getTypeMouvement())
                .compte(request.getCompte())
                .montant(request.getMontant())
                .motif(request.getMotif())
                .dateMouvement(LocalDateTime.now())
                .faitPar(userUuid)
                .build();

        mouvementManuelRepository.save(mouvement);
        log.info("Mouvement manuel {} créé sur {} pour {} (tenant={})",
                request.getTypeMouvement(), request.getCompte(),
                request.getMontant(), tenant.getTenantUuid());

        return getSoldeActuel();
    }

    // ── UTILITAIRES ──────────────────────────────────────────────────────────

    private void verifierCaisseActive(TenantEntity tenant) {
        if (!caisseConfigRepository.existsByTenant(tenant)) {
            throw new IllegalStateException("La caisse n'est pas activée. Activez-la d'abord.");
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** Convertit un CompteCaisse en ModePaiementCaisse correspondant (pour achats/dépenses). */
    private static ModePaiementCaisse compteToModePaiement(CompteCaisse compte) {
        return switch (compte) {
            case ESPECES      -> ModePaiementCaisse.ESPECES;
            case WAVE         -> ModePaiementCaisse.WAVE;
            case ORANGE_MONEY -> ModePaiementCaisse.ORANGE_MONEY;
        };
    }

    /** Convertit un CompteCaisse en ModePaiementVente correspondant (pour ventes). */
    private static VenteEntity.ModePaiementVente compteToModeVente(CompteCaisse compte) {
        return switch (compte) {
            case ESPECES      -> VenteEntity.ModePaiementVente.ESPECES;
            case WAVE         -> VenteEntity.ModePaiementVente.WAVE;
            case ORANGE_MONEY -> VenteEntity.ModePaiementVente.ORANGE_MONEY;
        };
    }
}
