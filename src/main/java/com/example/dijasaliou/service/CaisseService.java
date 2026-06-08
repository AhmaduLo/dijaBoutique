package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ActiverCaisseRequest;
import com.example.dijasaliou.dto.CaisseSoldeDto;
import com.example.dijasaliou.dto.MouvementCaisseRequest;
import com.example.dijasaliou.dto.MouvementHistoriqueDto;
import com.example.dijasaliou.dto.MouvementHistoriqueDto.TypeHistorique;
import com.example.dijasaliou.dto.TransfertCaisseRequest;
import com.example.dijasaliou.entity.*;
import com.example.dijasaliou.entity.MouvementCaisseManuelEntity.TypeMouvement;
import com.example.dijasaliou.repository.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    private final PaiementCreditRepository            paiementCreditRepository;
    private final TenantService                       tenantService;
    private final UserRepository                      userRepository;

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
        config.setSoldeInitialVirement(request.getSoldeInitialVirement());
        config.setDateActivation(LocalDateTime.now());
        config.setActivePar(userUuid);

        caisseConfigRepository.save(config);
        log.info("Caisse activée pour tenant={} : Espèces={}, Wave={}, OM={}, Virement={}",
                tenant.getTenantUuid(),
                request.getSoldeInitialEspeces(),
                request.getSoldeInitialWave(),
                request.getSoldeInitialOm(),
                request.getSoldeInitialVirement());

        return calculerSolde(tenant, config, LocalDateTime.now());
    }

    // ── CALCUL DU SOLDE ──────────────────────────────────────────────────────

    /**
     * Calcule le solde actuel par compte + total. Si la caisse n'est pas activée,
     * retourne un DTO avec active = false.
     */
    @Transactional(readOnly = true)
    public CaisseSoldeDto getSoldeActuel() {
        return getSoldeAt(null);
    }

    /**
     * Calcule le solde à une date donnée (fin de journée). Si {@code asOfDate} est
     * null ou postérieure à maintenant, le calcul est en temps réel.
     *
     * Utilisé pour les "snapshots" virtuels quand l'utilisateur consulte un mois
     * passé sur le dashboard — les ventes/achats/dépenses/transferts/mouvements
     * sont bornés à la fin de la journée de {@code asOfDate}.
     */
    @Transactional(readOnly = true)
    public CaisseSoldeDto getSoldeAt(LocalDate asOfDate) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        LocalDateTime fin = toFinJournee(asOfDate);

        return caisseConfigRepository.findByTenant(tenant)
                .map(config -> calculerSolde(tenant, config, fin))
                .orElseGet(() -> CaisseSoldeDto.builder()
                        .active(false)
                        .build());
    }

    private CaisseSoldeDto calculerSolde(TenantEntity tenant, CaisseConfigEntity config, LocalDateTime fin) {
        LocalDateTime debut = config.getDateActivation();

        // Si la période se termine avant l'activation, la caisse n'existait pas
        // encore à cette date — on renvoie un solde à 0 (le frontend affiche
        // un message "Caisse pas encore activée à cette date").
        if (fin.isBefore(debut)) {
            return CaisseSoldeDto.builder()
                    .active(true)
                    .dateActivation(debut)
                    .soldeEspeces(BigDecimal.ZERO)
                    .soldeWave(BigDecimal.ZERO)
                    .soldeOm(BigDecimal.ZERO)
                    .soldeVirement(BigDecimal.ZERO)
                    .soldeTotal(BigDecimal.ZERO)
                    .soldeInitialEspeces(BigDecimal.ZERO)
                    .soldeInitialWave(BigDecimal.ZERO)
                    .soldeInitialOm(BigDecimal.ZERO)
                    .soldeInitialVirement(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal soldeEspeces  = calculerSoldeCompte(tenant, CompteCaisse.ESPECES, debut, fin,
                config.getSoldeInitialEspeces());
        BigDecimal soldeWave     = calculerSoldeCompte(tenant, CompteCaisse.WAVE, debut, fin,
                config.getSoldeInitialWave());
        BigDecimal soldeOm       = calculerSoldeCompte(tenant, CompteCaisse.ORANGE_MONEY, debut, fin,
                config.getSoldeInitialOm());
        BigDecimal soldeVirement = calculerSoldeCompte(tenant, CompteCaisse.VIREMENT, debut, fin,
                nz(config.getSoldeInitialVirement()));

        BigDecimal soldeTotal = soldeEspeces.add(soldeWave).add(soldeOm).add(soldeVirement);

        return CaisseSoldeDto.builder()
                .active(true)
                .dateActivation(debut)
                .soldeEspeces(soldeEspeces)
                .soldeWave(soldeWave)
                .soldeOm(soldeOm)
                .soldeVirement(soldeVirement)
                .soldeTotal(soldeTotal)
                .soldeInitialEspeces(config.getSoldeInitialEspeces())
                .soldeInitialWave(config.getSoldeInitialWave())
                .soldeInitialOm(config.getSoldeInitialOm())
                .soldeInitialVirement(nz(config.getSoldeInitialVirement()))
                .build();
    }

    /**
     * Calcule le solde d'un compte précis entre la date d'activation et {@code fin}.
     */
    private BigDecimal calculerSoldeCompte(TenantEntity tenant, CompteCaisse compte,
                                            LocalDateTime debut, LocalDateTime fin,
                                            BigDecimal soldeInitial) {
        ModePaiementCaisse modePaiement = compteToModePaiement(compte);
        VenteEntity.ModePaiementVente modeVente = compteToModeVente(compte);
        PaiementCreditEntity.ModePaiement modeCredit = compteToModeCredit(compte);

        BigDecimal entreesVentes      = venteRepository.sumByModePaiementBetween(tenant, modeVente, debut, fin);
        BigDecimal sortiesAchats      = achatRepository.sumByModePaiementBetween(tenant, modePaiement, debut, fin);
        BigDecimal sortiesDepenses    = depenseRepository.sumByModePaiementBetween(tenant, modePaiement, debut, fin);
        // paiements crédit filtrés sur datePaiement (date métier, pas createdDate technique)
        BigDecimal entreesCredits     = paiementCreditRepository.sumByModeBetween(
                tenant, modeCredit, debut.toLocalDate(), fin.toLocalDate());
        BigDecimal transfertsEntrants = transfertRepository.sumEntreesByCompteBetween(tenant, compte, debut, fin);
        BigDecimal transfertsSortants = transfertRepository.sumSortiesByCompteBetween(tenant, compte, debut, fin);
        BigDecimal entreesManuelles   = mouvementManuelRepository.sumByCompteAndTypeBetween(
                tenant, compte, TypeMouvement.ENTREE, debut, fin);
        BigDecimal sortiesManuelles   = mouvementManuelRepository.sumByCompteAndTypeBetween(
                tenant, compte, TypeMouvement.SORTIE, debut, fin);

        return nz(soldeInitial)
                .add(nz(entreesVentes))
                .add(nz(entreesCredits))
                .subtract(nz(sortiesAchats))
                .subtract(nz(sortiesDepenses))
                .add(nz(transfertsEntrants))
                .subtract(nz(transfertsSortants))
                .add(nz(entreesManuelles))
                .subtract(nz(sortiesManuelles));
    }

    /** Borne supérieure : fin de la journée d'asOfDate, ou maintenant si null. */
    private static LocalDateTime toFinJournee(LocalDate asOfDate) {
        if (asOfDate == null) {
            return LocalDateTime.now();
        }
        return asOfDate.atTime(23, 59, 59);
    }

    private static LocalDateTime maxDateTime(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    // ── TRANSFERTS ───────────────────────────────────────────────────────────

    @Transactional
    public CaisseSoldeDto creerTransfert(TransfertCaisseRequest request, String userUuid) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        verifierCaisseActive(tenant);

        if (request.getCompteSource().equals(request.getCompteDestination())) {
            throw new IllegalArgumentException("Le compte source et destination doivent être différents");
        }

        verifierSoldeSuffisant(tenant, request.getCompteSource(), request.getMontant());

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

        // Une sortie ne peut pas rendre le compte négatif
        if (request.getTypeMouvement() == TypeMouvement.SORTIE) {
            verifierSoldeSuffisant(tenant, request.getCompte(), request.getMontant());
        }

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

    // ── HISTORIQUE ────────────────────────────────────────────────────────────

    /**
     * Retourne l'historique des mouvements manuels et des transferts depuis
     * l'activation de la caisse, triés par date décroissante (plus récent en haut).
     *
     * N'inclut PAS les achats / ventes / dépenses — ils sont consultables sur
     * leurs pages respectives.
     */
    @Transactional(readOnly = true)
    public List<MouvementHistoriqueDto> getHistorique() {
        return getHistoriqueBetween(null, null);
    }

    /**
     * Historique borné à un intervalle [fromDate, toDate] (inclusif).
     * {@code fromDate} = null → on part de la date d'activation.
     * {@code toDate}   = null → on va jusqu'à maintenant.
     * Sinon, l'intervalle est restreint à la fenêtre demandée (mais jamais avant
     * l'activation, qui reste la borne inférieure absolue).
     */
    @Transactional(readOnly = true)
    public List<MouvementHistoriqueDto> getHistoriqueBetween(LocalDate fromDate, LocalDate toDate) {
        TenantEntity tenant = tenantService.getCurrentTenant();

        var configOpt = caisseConfigRepository.findByTenant(tenant);
        if (configOpt.isEmpty()) {
            return List.of();
        }
        LocalDateTime dateActivation = configOpt.get().getDateActivation();
        LocalDateTime debut = fromDate != null
                ? maxDateTime(dateActivation, fromDate.atStartOfDay())
                : dateActivation;
        LocalDateTime fin = toFinJournee(toDate);

        // Période entièrement avant l'activation → rien à afficher
        if (fin.isBefore(dateActivation)) {
            return List.of();
        }

        List<MouvementHistoriqueDto> historique = new ArrayList<>();

        mouvementManuelRepository.findByTenantBetween(tenant, debut, fin).forEach(m ->
            historique.add(MouvementHistoriqueDto.builder()
                    .id(m.getId())
                    .type(m.getTypeMouvement() == TypeMouvement.ENTREE
                            ? TypeHistorique.ENTREE
                            : TypeHistorique.SORTIE)
                    .compte(m.getCompte())
                    .montant(m.getMontant())
                    .motif(m.getMotif())
                    .date(m.getDateMouvement())
                    .faitPar(m.getFaitPar())
                    .build())
        );

        transfertRepository.findByTenantBetween(tenant, debut, fin).forEach(t ->
            historique.add(MouvementHistoriqueDto.builder()
                    .id(t.getId())
                    .type(TypeHistorique.TRANSFERT)
                    .compteSource(t.getCompteSource())
                    .compteDestination(t.getCompteDestination())
                    .montant(t.getMontant())
                    .motif(t.getMotif())
                    .date(t.getDateTransfert())
                    .faitPar(t.getFaitPar())
                    .build())
        );

        historique.sort(Comparator.comparing(MouvementHistoriqueDto::getDate).reversed());

        // Résolution des noms utilisateurs en un seul batch
        Map<String, String> nomsParId = resolveNomsUtilisateurs(historique);
        historique.forEach(h -> h.setFaitParNom(nomsParId.get(h.getFaitPar())));

        return historique;
    }

    /**
     * Récupère les noms (Prénom Nom) des utilisateurs qui ont fait les opérations
     * de l'historique, en un seul appel base.
     */
    private Map<String, String> resolveNomsUtilisateurs(List<MouvementHistoriqueDto> historique) {
        var ids = historique.stream()
                .map(MouvementHistoriqueDto::getFaitPar)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> {
                    try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) return new HashMap<>();

        Map<String, String> result = new HashMap<>();
        userRepository.findAllById(ids).forEach(u -> {
            String nom = ((u.getPrenom() != null ? u.getPrenom() : "") + " "
                       + (u.getNom() != null ? u.getNom() : "")).trim();
            if (nom.isEmpty()) nom = u.getEmail();
            result.put(String.valueOf(u.getId()), nom);
        });
        return result;
    }

    // ── UTILITAIRES ──────────────────────────────────────────────────────────

    private void verifierCaisseActive(TenantEntity tenant) {
        if (!caisseConfigRepository.existsByTenant(tenant)) {
            throw new IllegalStateException("La caisse n'est pas activée. Activez-la d'abord.");
        }
    }

    /**
     * Vérifie qu'un compte a un solde suffisant pour une opération sortante
     * (sortie manuelle ou transfert). Lève {@link IllegalArgumentException}
     * avec un message clair si le solde est insuffisant.
     */
    private void verifierSoldeSuffisant(TenantEntity tenant, CompteCaisse compte, BigDecimal montant) {
        BigDecimal soldeActuel = soldeCompte(tenant, compte);
        if (soldeActuel.compareTo(montant) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Solde insuffisant sur %s : disponible %s CFA, demandé %s CFA",
                    libelleCompte(compte),
                    formatMontant(soldeActuel),
                    formatMontant(montant)
            ));
        }
    }

    /** Formate un montant en CFA avec séparateur d'espaces (ex: 21 000). */
    private static String formatMontant(BigDecimal montant) {
        if (montant == null) return "0";
        var fmt = new java.text.DecimalFormat("#,##0");
        var sym = new java.text.DecimalFormatSymbols(java.util.Locale.FRENCH);
        sym.setGroupingSeparator(' ');
        fmt.setDecimalFormatSymbols(sym);
        return fmt.format(montant);
    }

    /** Solde courant d'un compte (calculé en temps réel). */
    private BigDecimal soldeCompte(TenantEntity tenant, CompteCaisse compte) {
        var configOpt = caisseConfigRepository.findByTenant(tenant);
        if (configOpt.isEmpty()) return BigDecimal.ZERO;
        CaisseConfigEntity config = configOpt.get();
        BigDecimal soldeInitial = switch (compte) {
            case ESPECES      -> config.getSoldeInitialEspeces();
            case WAVE         -> config.getSoldeInitialWave();
            case ORANGE_MONEY -> config.getSoldeInitialOm();
            case VIREMENT     -> nz(config.getSoldeInitialVirement());
        };
        return calculerSoldeCompte(tenant, compte, config.getDateActivation(), LocalDateTime.now(), soldeInitial);
    }

    private static String libelleCompte(CompteCaisse compte) {
        return switch (compte) {
            case ESPECES      -> "Espèces";
            case WAVE         -> "Wave";
            case ORANGE_MONEY -> "Orange Money";
            case VIREMENT     -> "Virement";
        };
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
            case VIREMENT     -> ModePaiementCaisse.VIREMENT;
        };
    }

    /** Convertit un CompteCaisse en ModePaiementVente correspondant (pour ventes). */
    private static VenteEntity.ModePaiementVente compteToModeVente(CompteCaisse compte) {
        return switch (compte) {
            case ESPECES      -> VenteEntity.ModePaiementVente.ESPECES;
            case WAVE         -> VenteEntity.ModePaiementVente.WAVE;
            case ORANGE_MONEY -> VenteEntity.ModePaiementVente.ORANGE_MONEY;
            case VIREMENT     -> VenteEntity.ModePaiementVente.VIREMENT;
        };
    }

    /** Convertit un CompteCaisse en PaiementCredit.ModePaiement (remboursements crédits). */
    private static PaiementCreditEntity.ModePaiement compteToModeCredit(CompteCaisse compte) {
        return switch (compte) {
            case ESPECES      -> PaiementCreditEntity.ModePaiement.ESPECES;
            case WAVE         -> PaiementCreditEntity.ModePaiement.WAVE;
            case ORANGE_MONEY -> PaiementCreditEntity.ModePaiement.ORANGE_MONEY;
            case VIREMENT     -> PaiementCreditEntity.ModePaiement.VIREMENT;
        };
    }
}
