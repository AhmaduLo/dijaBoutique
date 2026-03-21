package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.StockDto;
import com.example.dijasaliou.dto.VenteDto;
import com.example.dijasaliou.entity.ClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity.StatutCredit;
import com.example.dijasaliou.entity.PaiementCreditEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.repository.ClientRepository;
import com.example.dijasaliou.repository.CreditClientRepository;
import com.example.dijasaliou.repository.PaiementCreditRepository;
import com.example.dijasaliou.repository.VenteRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    public VenteService(VenteRepository venteRepository,
                        @Lazy StockService stockService,
                        TenantService tenantService,
                        StockAlertService stockAlertService,
                        @Lazy CreditClientService creditClientService,
                        ClientRepository clientRepository,
                        CreditClientRepository creditClientRepository,
                        PaiementCreditRepository paiementCreditRepository) {
        this.venteRepository = venteRepository;
        this.stockService = stockService;
        this.tenantService = tenantService;
        this.stockAlertService = stockAlertService;
        this.creditClientService = creditClientService;
        this.clientRepository = clientRepository;
        this.creditClientRepository = creditClientRepository;
        this.paiementCreditRepository = paiementCreditRepository;
    }

    /**
     * Récupérer toutes les ventes (utilisé pour rapports/export)
     */
    public List<VenteEntity> obtenirToutesLesVentes() {
        return venteRepository.findAll();
    }

    /**
     * Récupérer les ventes paginées avec recherche optionnelle et filtre de dates
     */
    public PagedResponse<VenteDto> obtenirVentesPaginees(int page, int size, String search, LocalDate dateDebut, LocalDate dateFin) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateVente", "id"));
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        Page<VenteEntity> ventesPage = venteRepository.findAllWithSearch(searchParam, dateDebut, dateFin, pageable);
        Page<VenteDto> dtoPage = ventesPage.map(VenteDto::fromEntity);
        return PagedResponse.from(dtoPage);
    }

    /**
     * Récupérer les ventes d'un utilisateur paginées avec recherche optionnelle et filtre de dates
     */
    public PagedResponse<VenteDto> obtenirVentesParUtilisateurPaginees(UserEntity utilisateur, int page, int size, String search, LocalDate dateDebut, LocalDate dateFin) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateVente", "id"));
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        Page<VenteEntity> ventesPage = venteRepository.findByUtilisateurWithSearch(utilisateur, searchParam, dateDebut, dateFin, pageable);
        Page<VenteDto> dtoPage = ventesPage.map(VenteDto::fromEntity);
        return PagedResponse.from(dtoPage);
    }

    /**
     * Récupérer une vente par ID
     */
    public VenteEntity obtenirVenteParId(Long id) {
        return venteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée avec l'ID : " + id));
    }

    /**
     * Créer une nouvelle vente
     */
    @org.springframework.transaction.annotation.Transactional
    public VenteEntity creerVente(VenteEntity vente, UserEntity utilisateur) {
        // Validation
        validerVente(vente);

        // Vérifier le stock disponible
        verifierStockAvantVente(vente.getNomProduit(), vente.getQuantite());

        // Associer l'utilisateur
        vente.setUtilisateur(utilisateur);

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
    public VenteEntity modifierVente(Long id, VenteEntity venteModifiee) {
        VenteEntity venteExistante = obtenirVenteParId(id);

        // SÉCURITÉ : Vérifier que la vente appartient au tenant actuel (double sécurité)
        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!venteExistante.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        validerVente(venteModifiee);

        // Vérifier si le nom du produit a changé
        boolean produitChange = !venteExistante.getNomProduit().equalsIgnoreCase(venteModifiee.getNomProduit());

        venteExistante.setQuantite(venteModifiee.getQuantite());
        venteExistante.setNomProduit(venteModifiee.getNomProduit());
        venteExistante.setPrixUnitaire(venteModifiee.getPrixUnitaire());
        venteExistante.setDateVente(venteModifiee.getDateVente());
        venteExistante.setClient(venteModifiee.getClient());
        venteExistante.setTelephoneClient(venteModifiee.getTelephoneClient());
        venteExistante.setAdresseClient(venteModifiee.getAdresseClient());
        venteExistante.setUtilisateur(venteModifiee.getUtilisateur());
        venteExistante.setUnite(venteModifiee.getUnite());

        // Si le produit a changé, récupérer automatiquement la photo du nouveau produit depuis le stock
        if (produitChange) {
            try {
                StockDto stock = stockService.obtenirStockParNomProduit(venteModifiee.getNomProduit());
                venteExistante.setPhotoUrl(stock.getPhotoUrl());
            } catch (RuntimeException e) {
                // Si le produit n'existe pas dans le stock, garder la photo fournie ou null
                venteExistante.setPhotoUrl(venteModifiee.getPhotoUrl());
            }
        } else {
            // Si le produit n'a pas changé, utiliser la photo fournie (permet de mettre à jour manuellement)
            venteExistante.setPhotoUrl(venteModifiee.getPhotoUrl());
        }

        venteExistante.calculerPrixTotal();

        return venteRepository.save(venteExistante);
    }

    /**
     * Supprimer une vente
     */
    @org.springframework.transaction.annotation.Transactional
    public void supprimerVente(Long id) {
        VenteEntity venteExistante = obtenirVenteParId(id);

        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!venteExistante.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        // Nettoyer les crédits liés avant suppression (évite crédits orphelins et dette corrompue)
        if (venteExistante.getModePaiement() == VenteEntity.ModePaiementVente.CREDIT) {
            creditClientService.supprimerCreditsDeLaVente(id);
        }

        venteRepository.deleteById(id);
    }

    /**
     * Récupérer les ventes d'un utilisateur
     */
    public List<VenteEntity> obtenirVentesParUtilisateur(UserEntity utilisateur) {
        return venteRepository.findByUtilisateur(utilisateur);
    }

    /**
     * Récupérer les ventes d'une période
     */
    public List<VenteEntity> obtenirVentesParPeriode(LocalDate debut, LocalDate fin) {
        return venteRepository.findByDateVenteBetween(debut, fin);
    }

    /**
     * Calculer le chiffre d'affaires d'une période
     */
    public BigDecimal calculerChiffreAffaires(LocalDate debut, LocalDate fin) {
        List<VenteEntity> ventes = obtenirVentesParPeriode(debut, fin);

        return ventes.stream()
                .map(VenteEntity::getPrixTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
    public Map<String, Object> calculerRapportModePaiement(LocalDate debut, LocalDate fin) {
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();

        Map<String, java.math.BigDecimal> totaux = new java.util.LinkedHashMap<>();
        Map<String, Long> nombres = new java.util.LinkedHashMap<>();
        for (String mode : List.of("ESPECES", "WAVE", "ORANGE_MONEY", "CREDIT")) {
            totaux.put(mode, java.math.BigDecimal.ZERO);
            nombres.put(mode, 0L);
        }

        // 1. Ventes directes non-CREDIT groupées par mode
        List<Object[]> directVentes = venteRepository.sumDirectVentesParModeEtPeriode(
                debut, fin, VenteEntity.ModePaiementVente.CREDIT, tenantUuid);
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
        Object[] creditRow = creditClientRepository.sumCreditsRestantParPeriode(
                debut, fin, StatutCredit.SOLDE, tenantUuid);
        if (creditRow != null && creditRow[1] != null) {
            java.math.BigDecimal creditTotal = creditRow[1] instanceof java.math.BigDecimal
                    ? (java.math.BigDecimal) creditRow[1]
                    : new java.math.BigDecimal(creditRow[1].toString());
            Long creditCount = creditRow[0] instanceof Number ? ((Number) creditRow[0]).longValue() : 0L;
            totaux.put("CREDIT", creditTotal);
            nombres.put("CREDIT", creditCount);
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
     * VALIDATION
     */
    private void validerVente(VenteEntity vente) {
        if (vente.getQuantite() == null || vente.getQuantite() <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à 0");
        }

        if (vente.getPrixUnitaire() == null ||
                vente.getPrixUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix unitaire doit être supérieur à 0");
        }

        if (vente.getNomProduit() == null || vente.getNomProduit().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire");
        }
    }

    /**
     * Vérifier le stock avant une vente
     * Lance une exception si le stock est insuffisant
     */
    private void verifierStockAvantVente(String nomProduit, Integer quantite) {
        try {
            StockDto stock = stockService.obtenirStockParNomProduit(nomProduit);

            if (!stock.estSuffisant(quantite)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Stock insuffisant pour '%s' ! Disponible : %d, Demandé : %d",
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
