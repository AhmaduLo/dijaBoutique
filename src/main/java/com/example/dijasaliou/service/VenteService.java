package com.example.dijasaliou.service;

import com.example.dijasaliou.exception.ConflictException;
import com.example.dijasaliou.dto.BeneficeStatistiquesDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.dto.VenteDto;
import com.example.dijasaliou.entity.ClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity.StatutCredit;
import com.example.dijasaliou.entity.PaiementCreditEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.repository.ClientRepository;
import com.example.dijasaliou.repository.CreditClientRepository;
import com.example.dijasaliou.repository.PaiementCreditRepository;
import com.example.dijasaliou.repository.VenteLotConsommationRepository;
import com.example.dijasaliou.repository.VenteRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Service pour la logique métier des ventes
 */
@Service
@Slf4j
public class VenteService {

    private final VenteRepository venteRepository;
    private final StockService stockService;
    private final TenantService tenantService;
    private final StockAlertService stockAlertService;
    private final CreditClientService creditClientService;
    private final ClientRepository clientRepository;
    private final CreditClientRepository creditClientRepository;
    private final PaiementCreditRepository paiementCreditRepository;
    private final FifoCalculService fifoCalculService;
    private final VenteLotConsommationRepository consommationRepository;

    public VenteService(VenteRepository venteRepository,
                        @Lazy StockService stockService,
                        TenantService tenantService,
                        StockAlertService stockAlertService,
                        @Lazy CreditClientService creditClientService,
                        ClientRepository clientRepository,
                        CreditClientRepository creditClientRepository,
                        PaiementCreditRepository paiementCreditRepository,
                        FifoCalculService fifoCalculService,
                        VenteLotConsommationRepository consommationRepository) {
        this.venteRepository = venteRepository;
        this.stockService = stockService;
        this.tenantService = tenantService;
        this.stockAlertService = stockAlertService;
        this.creditClientService = creditClientService;
        this.clientRepository = clientRepository;
        this.creditClientRepository = creditClientRepository;
        this.paiementCreditRepository = paiementCreditRepository;
        this.fifoCalculService = fifoCalculService;
        this.consommationRepository = consommationRepository;
    }

    /**
     * Récupérer toutes les ventes du tenant courant (rapports/export)
     */
    @Transactional(readOnly = true)
    public List<VenteEntity> obtenirToutesLesVentes() {
        return venteRepository.findAllByTenant(tenantService.getCurrentTenant());
    }

    /**
     * Récupérer les ventes paginées avec recherche optionnelle et filtre de dates
     */
    @Transactional(readOnly = true)
    public PagedResponse<VenteDto> obtenirVentesPaginees(int page, int size, String search, LocalDate dateDebut, LocalDate dateFin) {
        PageRequest pageable = PageRequest.of(page, size);
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();
        LocalDateTime debutDt = dateDebut != null ? dateDebut.atStartOfDay() : null;
        LocalDateTime finDt = dateFin != null ? dateFin.atTime(LocalTime.MAX) : null;
        Page<VenteEntity> ventesPage = venteRepository.findAllWithSearch(tenantUuid, searchParam, debutDt, finDt, pageable);
        Page<VenteDto> dtoPage = ventesPage.map(VenteDto::fromEntity);
        enrichirCreditStatut(dtoPage.getContent(), tenantUuid);
        return PagedResponse.from(dtoPage);
    }

    /**
     * Récupérer les ventes d'un utilisateur paginées avec recherche optionnelle et filtre de dates
     */
    @Transactional(readOnly = true)
    public PagedResponse<VenteDto> obtenirVentesParUtilisateurPaginees(UserEntity utilisateur, int page, int size, String search, LocalDate dateDebut, LocalDate dateFin) {
        PageRequest pageable = PageRequest.of(page, size);
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();
        LocalDateTime debutDt2 = dateDebut != null ? dateDebut.atStartOfDay() : null;
        LocalDateTime finDt2 = dateFin != null ? dateFin.atTime(LocalTime.MAX) : null;
        Page<VenteEntity> ventesPage = venteRepository.findByUtilisateurWithSearch(utilisateur, tenantUuid, searchParam, debutDt2, finDt2, pageable);
        Page<VenteDto> dtoPage = ventesPage.map(VenteDto::fromEntity);
        enrichirCreditStatut(dtoPage.getContent(), tenantUuid);
        return PagedResponse.from(dtoPage);
    }

    /**
     * Enrichit une liste de VenteDto avec le statut de leur crédit associé (s'il existe).
     * Utilise une seule query bulk pour éviter le N+1 sur les ventes paginées.
     */
    private void enrichirCreditStatut(List<VenteDto> ventes, String tenantUuid) {
        if (ventes == null || ventes.isEmpty()) return;
        List<String> venteIds = ventes.stream()
                .filter(v -> "CREDIT".equals(v.getModePaiement()))
                .map(VenteDto::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (venteIds.isEmpty()) return;

        Map<String, String> statutParVenteId = new java.util.HashMap<>();
        for (Object[] row : creditClientRepository.findStatutByVenteIds(venteIds, tenantUuid)) {
            String venteId = (String) row[0];
            Object statut = row[1];
            statutParVenteId.put(venteId, statut != null ? statut.toString() : null);
        }
        for (VenteDto dto : ventes) {
            String s = statutParVenteId.get(dto.getId());
            if (s != null) dto.setCreditStatut(s);
        }
    }

    /**
     * Récupérer une vente par ID
     */
    @Transactional(readOnly = true)
    public VenteEntity obtenirVenteParId(String id) {
        return venteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée avec l'ID : " + id));
    }

    /**
     * Créer une nouvelle vente
     */
    @Transactional
    public VenteEntity creerVente(VenteEntity vente, UserEntity utilisateur) {
        // Validation
        validerVente(vente);

        // Vérifier le stock disponible
        verifierStockAvantVente(vente.getNomProduit(), vente.getQuantite());

        // Associer l'utilisateur
        vente.setUtilisateur(utilisateur);

        // Si le frontend fournit une dateVente (date locale de l'utilisateur), on la conserve.
        // Sinon on utilise l'heure serveur UTC. Cela évite le bug timezone : une vente faite à
        // 01h00 Paris (UTC+1) serait stockée la veille en UTC et invisible dans le filtre "Aujourd'hui".
        if (vente.getDateVente() == null) {
            vente.setDateVente(LocalDateTime.now());
        }

        // MULTI-TENANT : Assigner le tenant actuel (CRUCIAL!)
        vente.setTenant(tenantService.getCurrentTenant());

        // Récupérer automatiquement la photo du produit depuis le stock si non fournie
        if (vente.getPhotoUrl() == null || vente.getPhotoUrl().trim().isEmpty()) {
            try {
                StockDto stock = stockService.obtenirStockParNomProduit(vente.getNomProduit());
                vente.setPhotoUrl(stock.getPhotoUrl());
            } catch (RuntimeException e) {
                // Si le produit n'existe pas dans le stock, laisser photoUrl null
                // La vente peut quand même être créée sans photo
            }
        }

        // Calculer le prix total
        if (vente.getPrixTotal() == null) {
            vente.calculerPrixTotal();
        }

        // Sauvegarder la vente
        VenteEntity venteSauvegardee = venteRepository.save(vente);
        stockService.invalidateStockCache(venteSauvegardee.getTenant().getTenantUuid());

        // FIFO : consommer le stock dans les lots d'achat (du plus ancien au plus récent)
        // et calculer le bénéfice net de la vente.
        // Ne JAMAIS bloquer la vente si le FIFO échoue (logique non critique).
        try {
            fifoCalculService.consommerStockFifo(venteSauvegardee);
        } catch (Exception e) {
            log.error("FIFO : échec du calcul de bénéfice pour la vente {} : {}",
                    venteSauvegardee.getId(), e.getMessage(), e);
        }

        // ALERTE DE STOCK : Vérifier le stock après la vente et envoyer une alerte si nécessaire
        // (uniquement pour les plans PREMIUM et ENTREPRISE)
        try {
            stockAlertService.verifierEtEnvoyerAlerte(vente.getNomProduit());
        } catch (Exception e) {
            // Ne pas bloquer la vente si l'envoi d'alerte échoue
        }

        // CRÉDIT CLIENT : Si mode_paiement = CREDIT, créer automatiquement un crédit
        // (uniquement pour le plan ENTREPRISE)
        if (vente.getModePaiement() == VenteEntity.ModePaiementVente.CREDIT) {
            // Résoudre clientId → ClientEntity si clientRef pas déjà défini
            ClientEntity clientPourCredit = vente.getClientRef();
            if (clientPourCredit == null && vente.getClientId() != null) {
                clientPourCredit = clientRepository.findById(vente.getClientId())
                        .orElse(null);
            }
            if (clientPourCredit == null) {
                throw new IllegalArgumentException(
                        "Un client enregistré est obligatoire pour une vente à crédit");
            }
            venteSauvegardee.setEstSoldee(false);
            venteSauvegardee.setClientRef(clientPourCredit);
            venteRepository.save(venteSauvegardee);
            creditClientService.creerCreditDepuisVente(
                    venteSauvegardee,
                    clientPourCredit,
                    utilisateur,
                    vente.getDateEcheance());
        }

        return venteSauvegardee;
    }

    /**
     * Modifier une vente
     */
    @Transactional
    public VenteEntity modifierVente(String id, VenteEntity venteModifiee) {
        VenteEntity venteExistante = obtenirVenteParId(id);

        // SÉCURITÉ : Vérifier que la vente appartient au tenant actuel
        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!venteExistante.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        validerVente(venteModifiee);

        // Détecter la transition crédit AVANT de modifier les champs
        boolean devientCredit = venteModifiee.getModePaiement() == VenteEntity.ModePaiementVente.CREDIT;
        boolean aUnCreditActif = creditClientRepository.existsByVenteIdAndStatutIn(
                id, List.of(StatutCredit.EN_ATTENTE, StatutCredit.PARTIEL));

        // Validation stock avant modification
        boolean produitChange = !venteExistante.getNomProduit().equalsIgnoreCase(venteModifiee.getNomProduit());
        if (produitChange) {
            // Nouveau produit : vérifier que le stock du nouveau produit est suffisant
            verifierStockAvantVente(venteModifiee.getNomProduit(), venteModifiee.getQuantite());
        } else if (venteModifiee.getQuantite() > venteExistante.getQuantite()) {
            // Même produit, quantité augmentée : seul le delta supplémentaire est consommé
            Double delta = venteModifiee.getQuantite() - venteExistante.getQuantite();
            verifierStockAvantVente(venteModifiee.getNomProduit(), delta);
        }

        // Mettre à jour les champs de la vente
        venteExistante.setQuantite(venteModifiee.getQuantite());
        venteExistante.setNomProduit(venteModifiee.getNomProduit());
        venteExistante.setPrixUnitaire(venteModifiee.getPrixUnitaire());
        if (venteModifiee.getDateVente() != null) {
            venteExistante.setDateVente(venteModifiee.getDateVente());
        }
        venteExistante.setClient(venteModifiee.getClient());
        venteExistante.setTelephoneClient(venteModifiee.getTelephoneClient());
        venteExistante.setAdresseClient(venteModifiee.getAdresseClient());
        venteExistante.setUtilisateur(venteModifiee.getUtilisateur());
        venteExistante.setUnite(venteModifiee.getUnite());
        if (venteModifiee.getModePaiement() != null) {
            venteExistante.setModePaiement(venteModifiee.getModePaiement());
        }
        // Sortie hors vente (perte, vol, casse, don, crédit impayé) — toujours synchroniser,
        // y compris quand on REMET à null (ex: passer d'une sortie à une vraie vente).
        venteExistante.setTypeSortie(venteModifiee.getTypeSortie());
        venteExistante.setMotifSortie(venteModifiee.getMotifSortie());

        // Photo
        if (produitChange) {
            try {
                StockDto stock = stockService.obtenirStockParNomProduit(venteModifiee.getNomProduit());
                venteExistante.setPhotoUrl(stock.getPhotoUrl());
            } catch (RuntimeException e) {
                venteExistante.setPhotoUrl(venteModifiee.getPhotoUrl());
            }
        } else {
            venteExistante.setPhotoUrl(venteModifiee.getPhotoUrl());
        }

        // Recalculer le prix AVANT la logique crédit (le montant est nécessaire pour créer/màj le crédit)
        venteExistante.calculerPrixTotal();

        // === LOGIQUE CRÉDIT — 4 cas ===

        if (aUnCreditActif && !devientCredit) {
            // Cas 1 : CRÉDIT → ESPÈCES/WAVE/OM — solder le crédit
            creditClientService.solderCreditsDeLaVente(id);
            venteExistante.setEstSoldee(true);

        } else if (!aUnCreditActif && devientCredit) {
            // Cas 2 : ESPÈCES/WAVE/OM → CRÉDIT — créer un crédit
            ClientEntity client = resoudreClientPourCredit(venteModifiee, venteExistante);
            venteExistante.setEstSoldee(false);
            venteExistante.setClientRef(client);
            VenteEntity venteSauvegardee = venteRepository.save(venteExistante);
            UserEntity employe = venteModifiee.getUtilisateur() != null
                    ? venteModifiee.getUtilisateur()
                    : venteExistante.getUtilisateur();
            creditClientService.creerCreditDepuisVente(
                    venteSauvegardee, client, employe, venteModifiee.getDateEcheance());
            stockService.invalidateStockCache(venteSauvegardee.getTenant().getTenantUuid());

            // FIFO : recalculer après modification
            try {
                fifoCalculService.recalculerFifo(venteSauvegardee);
            } catch (Exception e) {
                log.error("FIFO : échec du recalcul de bénéfice pour la vente {} : {}",
                        venteSauvegardee.getId(), e.getMessage(), e);
            }

            return venteSauvegardee;

        } else if (aUnCreditActif) {
            // Cas 3 : CRÉDIT → CRÉDIT — mettre à jour le montant du crédit existant
            creditClientService.mettreAJourCreditDeLaVente(id, venteExistante.getPrixTotal());
            venteExistante.setEstSoldee(false);

        }
        // Cas 4 : ESPÈCES/WAVE/OM → ESPÈCES/WAVE/OM — rien à faire sur le crédit

        VenteEntity saved = venteRepository.save(venteExistante);
        stockService.invalidateStockCache(saved.getTenant().getTenantUuid());

        // FIFO : recalculer les lignes de consommation
        // (rend les unités aux lots puis refait le calcul avec les nouvelles valeurs)
        try {
            fifoCalculService.recalculerFifo(saved);
        } catch (Exception e) {
            log.error("FIFO : échec du recalcul de bénéfice pour la vente {} : {}",
                    saved.getId(), e.getMessage(), e);
        }

        return saved;
    }

    /**
     * Résout le ClientEntity pour une vente à crédit lors d'une modification.
     * Priorité : clientId du formulaire → clientRef existant.
     */
    private ClientEntity resoudreClientPourCredit(VenteEntity venteModifiee, VenteEntity venteExistante) {
        if (venteModifiee.getClientId() != null) {
            return clientRepository.findById(venteModifiee.getClientId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Client introuvable : " + venteModifiee.getClientId()));
        }
        if (venteExistante.getClientRef() != null) {
            return venteExistante.getClientRef();
        }
        throw new IllegalArgumentException("Un client enregistré est obligatoire pour une vente à crédit");
    }

    /**
     * Calcule l'aperçu des conséquences avant de supprimer une vente.
     * Sert à afficher une modale claire au commerçant.
     *
     * Gère les 3 types de ventes :
     *   - Vente à CRÉDIT  : agrège les paiements crédit (PaiementCreditEntity)
     *                       par mode et par mois.
     *   - Vente cash      : crée un "paiement virtuel" représentant l'argent
     *                       reçu = prix_total payé par mode_paiement à la date
     *                       de la vente.
     *   - Sortie hors vente (vol, casse, don) : pas d'impact CA ni caisse,
     *                       mais on calcule l'impact sur les pertes
     *                       (coût FIFO qui sera dé-compté).
     */
    @Transactional(readOnly = true)
    public com.example.dijasaliou.dto.ImpactSuppressionVenteDto calculerImpactSuppression(String id) {
        VenteEntity vente = obtenirVenteParId(id);
        TenantEntity tenant = tenantService.getCurrentTenant();
        if (!vente.getTenant().getTenantUuid().equals(tenant.getTenantUuid())) {
            throw new SecurityException("Accès refusé");
        }

        boolean estCredit = vente.getModePaiement() == VenteEntity.ModePaiementVente.CREDIT;
        boolean estSortie = vente.getTypeSortie() != null;
        String creditStatut = null;
        String typeSortie = vente.getTypeSortie() != null ? vente.getTypeSortie().name() : null;
        java.math.BigDecimal impactPertes = java.math.BigDecimal.ZERO;

        List<com.example.dijasaliou.dto.ImpactSuppressionVenteDto.PaiementAAnnuler> paiementsAAnnuler = new java.util.ArrayList<>();

        if (estCredit) {
            // CAS 1 : crédit — vrais paiements crédit en base
            List<CreditClientEntity> credits = creditClientRepository.findByVenteIdWithPaiements(id);
            for (CreditClientEntity credit : credits) {
                if (creditStatut == null && credit.getStatut() != null) {
                    creditStatut = credit.getStatut().name();
                }
                if (credit.getPaiements() != null) {
                    for (com.example.dijasaliou.entity.PaiementCreditEntity p : credit.getPaiements()) {
                        paiementsAAnnuler.add(com.example.dijasaliou.dto.ImpactSuppressionVenteDto.PaiementAAnnuler.builder()
                                .paiementId(p.getId())
                                .datePaiement(p.getDatePaiement())
                                .modePaiement(p.getModePaiement() != null ? p.getModePaiement().name() : null)
                                .montant(p.getMontantPaye())
                                .build());
                    }
                }
            }
        } else if (!estSortie && vente.getPrixTotal() != null
                && vente.getPrixTotal().compareTo(BigDecimal.ZERO) > 0) {
            // CAS 2 : vente cash (Espèces / Wave / OM / Virement) — paiement virtuel
            LocalDate dateVente = vente.getDateVente() != null ? vente.getDateVente().toLocalDate() : LocalDate.now();
            paiementsAAnnuler.add(com.example.dijasaliou.dto.ImpactSuppressionVenteDto.PaiementAAnnuler.builder()
                    .paiementId(vente.getId())
                    .datePaiement(dateVente)
                    .modePaiement(vente.getModePaiement().name())
                    .montant(vente.getPrixTotal())
                    .build());
        } else if (estSortie) {
            // CAS 3 : sortie hors vente — pas de paiement, mais impact pertes
            BigDecimal coutFifo = consommationRepository.sumCoutAchatByVenteId(id, tenant);
            if (coutFifo != null && coutFifo.compareTo(BigDecimal.ZERO) > 0) {
                impactPertes = coutFifo.setScale(2, java.math.RoundingMode.HALF_UP);
            }
        }

        // Agrégat par mode de paiement
        Map<String, BigDecimal> totauxParMode = new java.util.LinkedHashMap<>();
        for (var p : paiementsAAnnuler) {
            totauxParMode.merge(p.getModePaiement(), p.getMontant(), BigDecimal::add);
        }
        List<com.example.dijasaliou.dto.ImpactSuppressionVenteDto.MontantParMode> totalParMode = totauxParMode.entrySet().stream()
                .map(e -> com.example.dijasaliou.dto.ImpactSuppressionVenteDto.MontantParMode.builder()
                        .modePaiement(e.getKey()).montant(e.getValue()).build())
                .collect(Collectors.toList());

        // Impact CA par mois — cash basis : chaque paiement compte sur SON mois
        Map<String, BigDecimal> retraitParMoisKey = new java.util.LinkedHashMap<>();
        for (var p : paiementsAAnnuler) {
            if (p.getDatePaiement() == null) continue;
            String key = p.getDatePaiement().getYear() + "-" + String.format("%02d", p.getDatePaiement().getMonthValue());
            retraitParMoisKey.merge(key, p.getMontant(), BigDecimal::add);
        }
        List<com.example.dijasaliou.dto.ImpactSuppressionVenteDto.ImpactCaMois> impactCaParMois = new java.util.ArrayList<>();
        String[] moisLibelles = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                                  "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        for (var entry : retraitParMoisKey.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int annee = Integer.parseInt(parts[0]);
            int mois = Integer.parseInt(parts[1]);
            LocalDate debutMois = LocalDate.of(annee, mois, 1);
            LocalDate finMois = debutMois.withDayOfMonth(debutMois.lengthOfMonth());
            BigDecimal caActuel = calculerChiffreAffaires(debutMois, finMois);
            BigDecimal diminution = entry.getValue().setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal caApres = caActuel.subtract(diminution).setScale(2, java.math.RoundingMode.HALF_UP);
            impactCaParMois.add(com.example.dijasaliou.dto.ImpactSuppressionVenteDto.ImpactCaMois.builder()
                    .annee(annee).mois(mois)
                    .moisLibelle(moisLibelles[mois - 1] + " " + annee)
                    .caActuel(caActuel)
                    .caApres(caApres)
                    .diminution(diminution)
                    .build());
        }

        return com.example.dijasaliou.dto.ImpactSuppressionVenteDto.builder()
                .venteId(id)
                .nomProduit(vente.getNomProduit())
                .quantiteARestaurer(vente.getQuantite())
                .modePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : "ESPECES")
                .estVenteCredit(estCredit)
                .creditStatut(creditStatut)
                .estSortieHorsVente(estSortie)
                .typeSortie(typeSortie)
                .impactPertes(impactPertes)
                .paiementsAAnnuler(paiementsAAnnuler)
                .totalParMode(totalParMode)
                .impactCaParMois(impactCaParMois)
                .build();
    }

    /**
     * Supprime une vente EN CASCADE : annule tous les paiements crédit, supprime
     * les crédits associés, restaure le stock FIFO, supprime la vente.
     *
     * À utiliser uniquement après confirmation explicite par le commerçant
     * (modale qui montre l'impact via calculerImpactSuppression).
     *
     * Différence avec supprimerVente() :
     *   - supprimerVente : bloque si crédit non soldé a paiements, sinon détache
     *     (laisse crédit + paiements en BDD comme historique)
     *   - supprimerVenteEnCascade : supprime TOUT (paiements + crédit + vente)
     */
    @Transactional
    public void supprimerVenteEnCascade(String id) {
        VenteEntity vente = obtenirVenteParId(id);
        TenantEntity tenant = tenantService.getCurrentTenant();
        if (!vente.getTenant().getTenantUuid().equals(tenant.getTenantUuid())) {
            throw new SecurityException("Accès refusé");
        }

        // 1. Supprimer en bloc les crédits + paiements (cascade JPA grâce à @OneToMany orphanRemoval=true)
        List<CreditClientEntity> credits = creditClientRepository.findByVenteIdWithPaiements(id);
        for (CreditClientEntity credit : credits) {
            log.info("[CASCADE] Suppression du crédit {} (statut {}, {} paiements) lié à la vente {}",
                    credit.getId(), credit.getStatut(),
                    credit.getPaiements() != null ? credit.getPaiements().size() : 0, id);
            creditClientRepository.delete(credit); // cascade vers paiements via orphanRemoval
        }

        // 2. FIFO : restaurer le stock dans les lots d'achat
        try {
            fifoCalculService.annulerConsommationFifo(id);
        } catch (Exception e) {
            log.error("[CASCADE] Échec annulation FIFO pour vente {} : {}", id, e.getMessage(), e);
        }

        // 3. Supprimer la vente
        venteRepository.deleteById(id);
        stockService.invalidateStockCache(tenant.getTenantUuid());

        log.info("[CASCADE] Vente {} supprimée avec succès (cascade complète)", id);
    }

    /**
     * Supprimer une vente
     */
    @Transactional
    public void supprimerVente(String id) {
        VenteEntity venteExistante = obtenirVenteParId(id);

        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!venteExistante.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        // Bloquer la suppression uniquement si un crédit NON soldé a des paiements (incohérence comptable)
        // Un crédit déjà SOLDE avec paiements → suppression autorisée (historique préservé via vente=null)
        List<CreditClientEntity> credits = creditClientRepository.findByVenteIdWithPaiements(id); // JOIN FETCH évite N+1
        for (CreditClientEntity credit : credits) {
            if (credit.getStatut() != StatutCredit.SOLDE
                    && credit.getPaiements() != null && !credit.getPaiements().isEmpty()) {
                throw new ConflictException(
                        "Impossible de supprimer cette vente : le crédit associé a des paiements en cours. " +
                        "Soldez d'abord le crédit.");
            }
        }

        // Option A : solder les crédits actifs + détacher la FK vente → historique préservé
        creditClientService.solderEtDetacherCreditsDeLaVente(id);

        // FIFO : annuler la consommation (rendre les unités aux lots) AVANT la suppression
        // pour que les FK soient propres et que le stock soit cohérent.
        try {
            fifoCalculService.annulerConsommationFifo(id);
        } catch (Exception e) {
            log.error("FIFO : échec de l'annulation de bénéfice pour la vente {} : {}",
                    id, e.getMessage(), e);
        }

        venteRepository.deleteById(id);
        stockService.invalidateStockCache(tenantActuel.getTenantUuid());
    }

    /**
     * Récupérer les ventes d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<VenteEntity> obtenirVentesParUtilisateur(UserEntity utilisateur) {
        return venteRepository.findByUtilisateur(utilisateur);
    }

    /**
     * Récupérer les ventes d'une période
     */
    @Transactional(readOnly = true)
    public List<VenteEntity> obtenirVentesParPeriode(LocalDate debut, LocalDate fin) {
        return venteRepository.findByDateVenteBetween(
                debut.atStartOfDay(), fin.atTime(LocalTime.MAX));
    }

    /**
     * Calculer le chiffre d'affaires d'une période — comptabilité de caisse.
     *
     * LOGIQUE CASH BASIS :
     *   CA = (somme des ventes payées immédiatement, mode != CREDIT)
     *      + (somme des paiements crédit reçus sur la période)
     *
     * Une vente crédit n'entre PAS dans le CA tant que le client ne paye pas.
     * À chaque paiement reçu, le montant payé s'ajoute au CA de la période du paiement.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculerChiffreAffaires(LocalDate debut, LocalDate fin) {
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();

        BigDecimal caNonCredit = venteRepository.sumChiffreAffairesNonCreditPeriode(
                debut.atStartOfDay(), fin.atTime(LocalTime.MAX), tenantUuid);
        if (caNonCredit == null) caNonCredit = BigDecimal.ZERO;

        BigDecimal caPaiementsCredit = paiementCreditRepository.sumMontantPayeBetweenAndTenant(
                debut, fin, tenantUuid);
        if (caPaiementsCredit == null) caPaiementsCredit = BigDecimal.ZERO;

        return caNonCredit.add(caPaiementsCredit);
    }

    /**
     * Ancienne méthode : CA en comptabilité d'engagement (toutes ventes confondues, crédit inclus).
     * Conservée pour usage interne uniquement (le rapport modes de paiement par exemple).
     */
    @Transactional(readOnly = true)
    public BigDecimal calculerChiffreAffairesAccrual(LocalDate debut, LocalDate fin) {
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();
        return venteRepository.sumChiffreAffairesPeriode(
                debut.atStartOfDay(), fin.atTime(LocalTime.MAX), tenantUuid);
    }

    /**
     * Liste détaillée des sorties hors vente (perte, vol, casse, don) ET des crédits
     * passés en perte sur une période. Utilisée par la modale "Pertes" du frontend.
     *
     * Les crédits perdus sont convertis en VenteDto virtuels (typeSortie=CREDIT_IMPAYE)
     * pour partager la même structure que les vraies sorties — le frontend les affiche
     * de manière unifiée dans le tableau.
     */
    @Transactional(readOnly = true)
    public List<com.example.dijasaliou.dto.VenteDto> obtenirSortiesPeriode(LocalDate debut, LocalDate fin) {
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();
        LocalDateTime debutDt = debut.atStartOfDay();
        LocalDateTime finDt = fin.atTime(LocalTime.MAX);

        List<com.example.dijasaliou.dto.VenteDto> resultat = new java.util.ArrayList<>(
                venteRepository.findSortiesBetween(debutDt, finDt, tenantUuid).stream()
                        .map(com.example.dijasaliou.dto.VenteDto::fromEntity)
                        .toList()
        );

        // Crédits passés en perte sur la période — virtual sorties
        for (com.example.dijasaliou.entity.CreditClientEntity credit :
                creditClientRepository.findCreditsPassesEnPerteBetween(debut, fin, tenantUuid)) {
            resultat.add(com.example.dijasaliou.dto.VenteDto.fromCreditPerdu(credit));
        }

        // Tri global par date desc
        resultat.sort((a, b) -> {
            if (a.getDateVente() == null) return 1;
            if (b.getDateVente() == null) return -1;
            return b.getDateVente().compareTo(a.getDateVente());
        });

        return resultat;
    }

    /**
     * Calculer les statistiques de bénéfice net (FIFO) sur une période — comptabilité de caisse.
     *
     * LOGIQUE CASH BASIS :
     *   CA          = ventes non-crédit de la période + paiements crédit reçus dans la période
     *   coût FIFO   = coût des ventes non-crédit + coût prorata des ventes crédit pour la part payée
     *   bénéfice    = CA − coût FIFO
     *
     * EXEMPLE :
     *   Vente crédit 100k (coût FIFO 60k). Mr Diop paye 50k en juin → on attribue :
     *     • CA juin     += 50k
     *     • Coût juin   += 60k × (50k / 100k) = 30k
     *     • Bénéfice    += 20k
     *
     * Les ventes sans ligne de consommation (produit jamais acheté ⇒ coût FIFO inconnu)
     * sont comptées dans nbVentesSansBenefice.
     */
    @Transactional(readOnly = true)
    public BeneficeStatistiquesDto calculerStatistiquesBenefice(LocalDate debut, LocalDate fin) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        LocalDateTime debutDt = debut.atStartOfDay();
        LocalDateTime finDt   = fin.atTime(LocalTime.MAX);
        String tenantUuid = tenant.getTenantUuid();

        // 1. Partie NON-CRÉDIT : ventes payées immédiatement → on prend tout (CA + coût + bénéfice complets)
        BigDecimal caNonCredit = venteRepository.sumChiffreAffairesNonCreditPeriode(debutDt, finDt, tenantUuid);
        if (caNonCredit == null) caNonCredit = BigDecimal.ZERO;

        BigDecimal coutAchat = consommationRepository.sumCoutAchatNonCreditBetween(tenant, debutDt, finDt);
        if (coutAchat == null) coutAchat = BigDecimal.ZERO;

        BigDecimal benefice  = consommationRepository.sumBeneficeNonCreditBetween(tenant, debutDt, finDt);
        if (benefice == null) benefice = BigDecimal.ZERO;

        // 2. Partie CRÉDIT : pour chaque paiement reçu dans la période, attribution prorata du coût/bénéfice
        List<PaiementCreditEntity> paiements = paiementCreditRepository.findPaiementsAvecVenteBetween(
                debut, fin, tenantUuid);

        BigDecimal caPaiementsCredit  = BigDecimal.ZERO;
        BigDecimal coutPaiementsCredit = BigDecimal.ZERO;
        BigDecimal beneficePaiementsCredit = BigDecimal.ZERO;

        for (PaiementCreditEntity p : paiements) {
            BigDecimal montantPaye = p.getMontantPaye();
            if (montantPaye == null || montantPaye.compareTo(BigDecimal.ZERO) <= 0) continue;

            VenteEntity vente = (p.getCredit() != null) ? p.getCredit().getVente() : null;
            if (vente == null || vente.getPrixTotal() == null
                    || vente.getPrixTotal().compareTo(BigDecimal.ZERO) <= 0) {
                // Paiement sans vente ou vente à prix 0 : on ajoute juste le CA, pas de coût attribuable
                caPaiementsCredit = caPaiementsCredit.add(montantPaye);
                continue;
            }

            // Prorata = montant payé / prix total de la vente
            BigDecimal prorata = montantPaye.divide(vente.getPrixTotal(), 6, java.math.RoundingMode.HALF_UP);

            BigDecimal coutVente     = consommationRepository.sumCoutAchatByVenteId(vente.getId(), tenant);
            if (coutVente == null) coutVente = BigDecimal.ZERO;
            BigDecimal beneficeVente = consommationRepository.sumBeneficeByVenteId(vente.getId(), tenant);
            if (beneficeVente == null) beneficeVente = BigDecimal.ZERO;

            caPaiementsCredit       = caPaiementsCredit.add(montantPaye);
            coutPaiementsCredit     = coutPaiementsCredit.add(coutVente.multiply(prorata));
            beneficePaiementsCredit = beneficePaiementsCredit.add(beneficeVente.multiply(prorata));
        }

        // 3. Agrégation finale
        BigDecimal ca         = caNonCredit.add(caPaiementsCredit);
        BigDecimal coutTotal  = coutAchat.add(coutPaiementsCredit).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal beneficeTotal = benefice.add(beneficePaiementsCredit).setScale(2, java.math.RoundingMode.HALF_UP);

        // 4. Comptages
        long nbVentesNonCredit  = venteRepository.countVentesNonCreditPeriode(debutDt, finDt, tenantUuid);
        long nbPaiementsCredit  = paiementCreditRepository.countByPeriodeAndTenant(debut, fin, tenantUuid);
        long nbVentes           = nbVentesNonCredit + nbPaiementsCredit;
        long nbVentesAvecBenefice = consommationRepository.countVentesAvecBeneficeBetween(tenant, debutDt, finDt);
        long nbVentesSansBenefice = Math.max(0L, nbVentes - nbVentesAvecBenefice);

        // 5. Sorties hors vente (pertes, vols, dons) — comptabilisées séparément
        java.util.Map<String, BigDecimal> pertesParType = new java.util.LinkedHashMap<>();
        BigDecimal totalPertes = BigDecimal.ZERO;
        long nbSorties = 0L;
        for (Object[] row : consommationRepository.sumPertesFifoParTypeBetween(tenant, debutDt, finDt)) {
            String type = row[0] != null ? row[0].toString() : "AUTRE";
            BigDecimal cout = row[1] instanceof BigDecimal ? (BigDecimal) row[1]
                    : (row[1] instanceof Number ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO);
            pertesParType.put(type, cout.setScale(2, java.math.RoundingMode.HALF_UP));
            totalPertes = totalPertes.add(cout);
        }
        // Compte les sorties via la query sumSortiesParTypeEtPeriode (compte les ventes, pas les lignes FIFO)
        for (Object[] row : venteRepository.sumSortiesParTypeEtPeriode(debutDt, finDt, tenantUuid)) {
            nbSorties += row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
        }

        // 5bis. Crédits impayés passés en perte sur la période
        // Pour chaque crédit en perte : perte FIFO = coût total vente × (montant restant / prix total vente)
        BigDecimal pertesCreditImpaye = BigDecimal.ZERO;
        long nbCreditsEnPerte = 0L;
        for (CreditClientEntity credit : creditClientRepository.findCreditsPassesEnPerteBetween(debut, fin, tenantUuid)) {
            BigDecimal montantRestant = credit.getMontantRestant();
            if (montantRestant == null || montantRestant.compareTo(BigDecimal.ZERO) <= 0) continue;

            VenteEntity vente = credit.getVente();
            if (vente == null || vente.getPrixTotal() == null
                    || vente.getPrixTotal().compareTo(BigDecimal.ZERO) <= 0) {
                // Sans vente associée, on prend la perte = montant restant (pas de coût FIFO calculable)
                pertesCreditImpaye = pertesCreditImpaye.add(montantRestant);
                nbCreditsEnPerte++;
                continue;
            }

            // Prorata FIFO : part non payée × coût total de la vente
            BigDecimal proratNonPaye = montantRestant.divide(vente.getPrixTotal(), 6, java.math.RoundingMode.HALF_UP);
            BigDecimal coutVente = consommationRepository.sumCoutAchatByVenteId(vente.getId(), tenant);
            if (coutVente == null) coutVente = BigDecimal.ZERO;
            pertesCreditImpaye = pertesCreditImpaye.add(coutVente.multiply(proratNonPaye));
            nbCreditsEnPerte++;
        }
        pertesCreditImpaye = pertesCreditImpaye.setScale(2, java.math.RoundingMode.HALF_UP);
        if (pertesCreditImpaye.compareTo(BigDecimal.ZERO) > 0) {
            pertesParType.merge("CREDIT_IMPAYE", pertesCreditImpaye, BigDecimal::add);
            totalPertes = totalPertes.add(pertesCreditImpaye);
            nbSorties += nbCreditsEnPerte;
        }

        BigDecimal marge = BigDecimal.ZERO;
        if (ca.compareTo(BigDecimal.ZERO) > 0) {
            marge = beneficeTotal
                    .multiply(new BigDecimal("100"))
                    .divide(ca, 2, java.math.RoundingMode.HALF_UP);
        }

        return BeneficeStatistiquesDto.builder()
                .dateDebut(debut)
                .dateFin(fin)
                .chiffreAffaires(ca)
                .totalCoutAchat(coutTotal)
                .beneficeNet(beneficeTotal)
                .totalPertes(totalPertes.setScale(2, java.math.RoundingMode.HALF_UP))
                .pertesParType(pertesParType)
                .nbSorties(nbSorties)
                .margePourcentage(marge)
                .nbVentes(nbVentes)
                .nbVentesAvecBenefice(nbVentesAvecBenefice)
                .nbVentesSansBenefice(nbVentesSansBenefice)
                .build();
    }

    /**
     * Calcule la répartition du CA par mode de paiement pour une période.
     *
     * Logique :
     * - ESPECES / WAVE / ORANGE_MONEY = ventes directes + remboursements de crédits du même mode
     * - CREDIT = somme des montants restants dus sur les crédits non soldés (créés dans la période)
     *
     * Retourne une map : mode → {total, nombre}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> calculerRapportModePaiement(LocalDate debut, LocalDate fin) {
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();

        Map<String, java.math.BigDecimal> totaux = new java.util.LinkedHashMap<>();
        Map<String, Long> nombres = new java.util.LinkedHashMap<>();
        for (String mode : List.of("ESPECES", "WAVE", "ORANGE_MONEY", "VIREMENT", "CREDIT")) {
            totaux.put(mode, java.math.BigDecimal.ZERO);
            nombres.put(mode, 0L);
        }

        // 1. Ventes directes non-CREDIT groupées par mode
        List<Object[]> directVentes = venteRepository.sumDirectVentesParModeEtPeriode(
                debut.atStartOfDay(), fin.atTime(LocalTime.MAX), VenteEntity.ModePaiementVente.CREDIT, tenantUuid);
        for (Object[] row : directVentes) {
            String mode = ((VenteEntity.ModePaiementVente) row[0]).name();
            Long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            java.math.BigDecimal total = row[2] instanceof java.math.BigDecimal
                    ? (java.math.BigDecimal) row[2]
                    : (row[2] instanceof Number ? new java.math.BigDecimal(row[2].toString()) : java.math.BigDecimal.ZERO);
            totaux.put(mode, total);
            nombres.put(mode, count);
        }

        // 2. Montant restant dû sur les crédits non soldés créés dans la période
        List<Object[]> creditRows = creditClientRepository.sumCreditsRestantParPeriode(
                debut.atStartOfDay(), fin.atTime(LocalTime.MAX), StatutCredit.SOLDE, tenantUuid);
        if (creditRows != null && !creditRows.isEmpty()) {
            Object[] creditRow = creditRows.get(0);
            if (creditRow != null && creditRow.length >= 2 && creditRow[1] != null) {
                java.math.BigDecimal creditTotal = creditRow[1] instanceof java.math.BigDecimal
                        ? (java.math.BigDecimal) creditRow[1]
                        : new java.math.BigDecimal(creditRow[1].toString());
                Long creditCount = creditRow[0] instanceof Number ? ((Number) creditRow[0]).longValue() : 0L;
                totaux.put("CREDIT", creditTotal);
                nombres.put("CREDIT", creditCount);
            }
        }

        // 3. Remboursements de crédits par mode, additionnés aux totaux correspondants
        List<Object[]> paiementsCredit = paiementCreditRepository.sumParModeEtPeriode(
                debut, fin, tenantUuid);
        for (Object[] row : paiementsCredit) {
            String mode = ((PaiementCreditEntity.ModePaiement) row[0]).name();
            Long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            java.math.BigDecimal total = row[2] instanceof java.math.BigDecimal
                    ? (java.math.BigDecimal) row[2]
                    : (row[2] instanceof Number ? new java.math.BigDecimal(row[2].toString()) : null);
            if (total != null) {
                totaux.merge(mode, total, java.math.BigDecimal::add);
            }
            if (count > 0) {
                nombres.merge(mode, count, Long::sum);
            }
        }

        // Construire la réponse : {ESPECES: {total, nombre}, WAVE: {...}, ...}
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (String mode : totaux.keySet()) {
            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("total", totaux.get(mode));
            entry.put("nombre", nombres.get(mode));
            result.put(mode, entry);
        }
        return result;
    }

    /**
     * Retourne le prochain numéro de ticket/facture pour le tenant courant.
     * Format : FAC-001, FAC-002, ...
     * Basé sur le nombre de ventes existantes en base → jamais de doublon entre appareils.
     */
    @Transactional(readOnly = true)
    public String getProchainNumeroFacture() {
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();
        long count = venteRepository.countByTenantUuid(tenantUuid);
        return String.format("FAC-%03d", count + 1);
    }

    /**
     * VALIDATION
     */
    private void validerVente(VenteEntity vente) {
        if (vente.getQuantite() == null || vente.getQuantite() <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à 0");
        }

        if (vente.getNomProduit() == null || vente.getNomProduit().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire");
        }

        // Cas sortie hors vente (perte, vol, casse, don, crédit impayé) :
        //   - prixUnitaire peut être 0 (la marchandise sort sans contrepartie financière)
        //   - typeSortie obligatoire pour expliquer pourquoi
        //   - le mode paiement CREDIT n'a pas de sens dans ce cas
        if (vente.getTypeSortie() != null) {
            if (vente.getPrixUnitaire() == null
                    || vente.getPrixUnitaire().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Le prix unitaire ne peut être négatif");
            }
            if (vente.getModePaiement() == VenteEntity.ModePaiementVente.CREDIT) {
                throw new IllegalArgumentException(
                        "Une sortie hors vente (perte, vol, don, casse) ne peut pas être à crédit");
            }
            return;
        }

        // Vente commerciale classique : prix obligatoire > 0
        if (vente.getPrixUnitaire() == null
                || vente.getPrixUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Le prix unitaire doit être supérieur à 0 (ou indiquer un motif de sortie : perte, vol, don…)");
        }
    }

    /**
     * Vérifier le stock avant une vente
     * Lance une exception si le stock est insuffisant
     */
    private void verifierStockAvantVente(String nomProduit, Double quantite) {
        try {
            StockDto stock = stockService.obtenirStockParNomProduit(nomProduit);

            if (!stock.estSuffisant(quantite)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Stock insuffisant pour '%s' ! Disponible : %.2f, Demandé : %.2f",
                                nomProduit,
                                stock.getStockDisponible(),
                                quantite
                        )
                );
            }
        } catch (RuntimeException e) {
            // Si le produit n'existe pas dans les achats, on peut quand même vendre
            // (cas d'un produit jamais acheté mais qu'on souhaite vendre)
            // Commentez cette partie si vous voulez forcer l'achat avant la vente
            if (e.getMessage().contains("Produit non trouvé")) {
                // Permettre la vente même sans achat préalable
                return;
            }
            throw e;
        }
    }
}
