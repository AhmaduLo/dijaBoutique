package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.CreditClientDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.PaiementCreditDto;
import com.example.dijasaliou.entity.*;
import com.example.dijasaliou.entity.CreditClientEntity.StatutCredit;
import com.example.dijasaliou.repository.ClientRepository;
import com.example.dijasaliou.repository.CreditClientRepository;
import com.example.dijasaliou.repository.PaiementCreditRepository;
import com.example.dijasaliou.repository.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CreditClientService {

    private final CreditClientRepository creditClientRepository;
    private final PaiementCreditRepository paiementCreditRepository;
    private final ClientRepository clientRepository;
    private final VenteRepository venteRepository;
    private final TenantService tenantService;
    // @Lazy : VenteService injecte déjà CreditClientService en @Lazy → on évite la cycle
    private final VenteService venteService;

    public CreditClientService(CreditClientRepository creditClientRepository,
                                PaiementCreditRepository paiementCreditRepository,
                                ClientRepository clientRepository,
                                VenteRepository venteRepository,
                                TenantService tenantService,
                                @org.springframework.context.annotation.Lazy VenteService venteService) {
        this.creditClientRepository = creditClientRepository;
        this.paiementCreditRepository = paiementCreditRepository;
        this.clientRepository = clientRepository;
        this.venteRepository = venteRepository;
        this.tenantService = tenantService;
        this.venteService = venteService;
    }

    /**
     * Appelé par VenteService quand mode_paiement = CREDIT
     */
    @Transactional
    public CreditClientEntity creerCreditDepuisVente(VenteEntity vente, ClientEntity client,
                                                      UserEntity employe, LocalDate dateEcheance) {
        // Garde anti-doublon : retourner le crédit actif existant si présent
        if (vente.getId() != null && creditClientRepository.existsByVenteIdAndStatutIn(
                vente.getId(), List.of(StatutCredit.EN_ATTENTE, StatutCredit.PARTIEL))) {
            return creditClientRepository.findByVenteId(vente.getId()).stream()
                    .filter(c -> c.getStatut() != StatutCredit.SOLDE)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Crédit actif introuvable malgré le garde"));
        }

        CreditClientEntity credit = CreditClientEntity.builder()
                .client(client)
                .vente(vente)
                .montantInitial(vente.getPrixTotal())
                .montantRestant(vente.getPrixTotal())
                .statut(StatutCredit.EN_ATTENTE)
                .dateEcheance(dateEcheance)
                .employe(employe)
                .employeNom(employe.getPrenom() + " " + employe.getNom())
                .tenant(vente.getTenant())
                .build();

        CreditClientEntity saved = creditClientRepository.save(credit);

        // Mettre à jour la dette totale du client
        client.setDetteTotale(client.getDetteTotale().add(vente.getPrixTotal()));
        clientRepository.save(client);

        log.info("Crédit créé : {} CFA pour {} (vente #{}, tenant: {})",
                vente.getPrixTotal(), client.getNom(), vente.getId(),
                vente.getTenant().getTenantUuid());

        return saved;
    }

    /**
     * Crée un crédit manuellement à partir d'une venteId et clientId.
     * Utilisé quand le frontend crée le crédit séparément (ex: modification de vente).
     */
    @Transactional
    public CreditClientEntity creerCredit(String venteId, String clientId, LocalDate dateEcheance, UserEntity employe) {
        VenteEntity vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new RuntimeException("Vente introuvable : " + venteId));

        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable : " + clientId));

        TenantEntity currentTenant = tenantService.getCurrentTenant();
        if (!vente.getTenant().getTenantUuid().equals(currentTenant.getTenantUuid())) {
            throw new RuntimeException("Accès non autorisé");
        }

        // Empêcher la création d'un doublon si un crédit actif existe déjà pour cette vente
        if (creditClientRepository.existsByVenteIdAndStatutIn(
                venteId, List.of(StatutCredit.EN_ATTENTE, StatutCredit.PARTIEL))) {
            throw new IllegalStateException("Un crédit actif existe déjà pour cette vente");
        }

        // Marquer la vente comme non soldée et en mode CREDIT
        vente.setEstSoldee(false);
        vente.setModePaiement(VenteEntity.ModePaiementVente.CREDIT);
        vente.setClientRef(client);
        venteRepository.save(vente);

        return creerCreditDepuisVente(vente, client, employe, dateEcheance);
    }

    /**
     * Enregistre un paiement (partiel ou total) sur un crédit
     */
    @Transactional
    public CreditClientDto enregistrerPaiement(String creditId, BigDecimal montant,
                                                PaiementCreditEntity.ModePaiement modePaiement,
                                                String note, UserEntity employe) {
        return enregistrerPaiement(creditId, montant, modePaiement, note, null, employe);
    }

    /** Variante avec date de paiement explicite (permet l'antédatage). */
    @Transactional
    public CreditClientDto enregistrerPaiement(String creditId, BigDecimal montant,
                                                PaiementCreditEntity.ModePaiement modePaiement,
                                                String note, LocalDate datePaiement, UserEntity employe) {
        // 1. Récupérer le crédit avec verrou pessimiste (empêche les paiements simultanés)
        CreditClientEntity credit = creditClientRepository.findByIdForUpdate(creditId)
                .orElseThrow(() -> new RuntimeException("Crédit introuvable : " + creditId));

        // 2. Double vérification tenant
        TenantEntity currentTenant = tenantService.getCurrentTenant();
        if (!credit.getTenant().getTenantUuid().equals(currentTenant.getTenantUuid())) {
            throw new RuntimeException("Accès non autorisé");
        }

        // 3. Vérifier que le crédit n'est pas déjà soldé
        if (credit.getStatut() == StatutCredit.SOLDE) {
            throw new IllegalStateException("Ce crédit est déjà soldé");
        }

        // 4. Validation métier
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        if (montant.compareTo(credit.getMontantRestant()) > 0) {
            throw new IllegalArgumentException(
                    "Le montant (" + montant + ") dépasse le restant dû (" + credit.getMontantRestant() + ")");
        }

        // 5. Créer le paiement (date du payload ou today dans la TZ du tenant)
        LocalDate dateEffective = datePaiement != null ? datePaiement : tenantService.todayInTenantTz();
        PaiementCreditEntity paiement = PaiementCreditEntity.builder()
                .credit(credit)
                .montantPaye(montant)
                .modePaiement(modePaiement)
                .datePaiement(dateEffective)
                .employe(employe)
                .employeNom(employe != null ? employe.getPrenom() + " " + employe.getNom() : "Inconnu")
                .note(note)
                .build();
        paiementCreditRepository.save(paiement);

        // 6. Mettre à jour le montant restant
        BigDecimal nouveauRestant = credit.getMontantRestant().subtract(montant);
        credit.setMontantRestant(nouveauRestant);

        ClientEntity client = credit.getClient();
        if (client == null) {
            throw new RuntimeException("Client introuvable pour ce crédit : " + creditId);
        }

        if (nouveauRestant.compareTo(BigDecimal.ZERO) == 0) {
            // Crédit soldé — soustraire uniquement le montant de CE paiement (les partiels précédents ont déjà réduit la dette)
            credit.setStatut(StatutCredit.SOLDE);
            BigDecimal nouvelleDette = client.getDetteTotale().subtract(montant);
            client.setDetteTotale(nouvelleDette.max(BigDecimal.ZERO));
            // Marquer la vente comme soldée — on GARDE modePaiement=CREDIT pour éviter
            // un double comptage dans la caisse (vente CREDIT + paiements crédit additionnés).
            // La vente reste historiquement une vente "à crédit", soldée par les paiements
            // enregistrés dans paiements_credit.
            if (credit.getVente() != null) {
                VenteEntity vente = credit.getVente();
                vente.setEstSoldee(true);
                venteRepository.save(vente);
            }
            log.info("Crédit #{} soldé pour {} (tenant: {})",
                    creditId, client.getNom(), currentTenant.getTenantUuid());
        } else {
            // Paiement partiel
            credit.setStatut(StatutCredit.PARTIEL);
            BigDecimal nouvelleDette = client.getDetteTotale().subtract(montant);
            client.setDetteTotale(nouvelleDette.max(BigDecimal.ZERO));
            log.info("Paiement partiel de {} sur crédit #{} — restant : {} (tenant: {})",
                    montant, creditId, nouveauRestant, currentTenant.getTenantUuid());
        }

        clientRepository.save(client);
        CreditClientEntity savedCredit = creditClientRepository.save(credit);

        return CreditClientDto.fromEntity(savedCredit);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CreditClientDto> obtenirCredits(int page, int size, String search,
                                                          String statut, LocalDate dateDebut, LocalDate dateFin) {
        Pageable pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdDate"));
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();

        Specification<CreditClientEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtre tenant (obligatoire)
            predicates.add(cb.equal(root.get("tenant").get("tenantUuid"), tenantUuid));

            // Filtre statut
            if (statut != null && !statut.isBlank() && !statut.equals("TOUS")) {
                try {
                    predicates.add(cb.equal(root.get("statut"), StatutCredit.valueOf(statut.toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }

            // Filtre recherche : nom ou téléphone du client
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Join<CreditClientEntity, ClientEntity> clientJoin = root.join("client", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(clientJoin.get("nom")), pattern),
                        cb.like(clientJoin.get("telephone"), "%" + search + "%")
                ));
            }

            // Filtre date (pour les soldés principalement)
            if (dateDebut != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"),
                        dateDebut.atStartOfDay()));
            }
            if (dateFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"),
                        dateFin.atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CreditClientEntity> pageResult = creditClientRepository.findAll(spec, pageable);
        Page<CreditClientDto> dtoPage = pageResult.map(CreditClientDto::fromEntity);
        return PagedResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public List<PaiementCreditDto> obtenirPaiements(String creditId) {
        CreditClientEntity credit = creditClientRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Crédit introuvable : " + creditId));
        TenantEntity currentTenant = tenantService.getCurrentTenant();
        if (!credit.getTenant().getTenantUuid().equals(currentTenant.getTenantUuid())) {
            throw new RuntimeException("Accès non autorisé");
        }
        return paiementCreditRepository.findByCreditOrderByCreatedDateDesc(credit).stream()
                .map(PaiementCreditDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Passe un crédit en perte (le client ne paiera jamais).
     *
     * Effet :
     *   - Le statut du crédit devient PERTE
     *   - datePassageEnPerte = aujourd'hui (sert à attribuer la perte FIFO à la bonne période)
     *   - Le montant restant est marqué comme perdu (resterDu × prorata FIFO côté stats)
     *   - Les paiements déjà reçus restent dans le CA / caisse (intacts)
     *   - Le crédit disparaît de la liste "actifs" (en_attente / partiel)
     *   - Pas de mouvement de stock (la marchandise est déjà partie)
     *
     * Validation :
     *   - Crédit doit être EN_ATTENTE ou PARTIEL (pas SOLDE ni déjà PERTE)
     *   - Reste dû doit être > 0
     */
    @Transactional
    public CreditClientDto passerEnPerte(String creditId) {
        CreditClientEntity credit = creditClientRepository.findByIdForUpdate(creditId)
                .orElseThrow(() -> new RuntimeException("Crédit introuvable : " + creditId));

        TenantEntity currentTenant = tenantService.getCurrentTenant();
        if (!credit.getTenant().getTenantUuid().equals(currentTenant.getTenantUuid())) {
            throw new RuntimeException("Accès non autorisé");
        }

        if (credit.getStatut() == StatutCredit.SOLDE) {
            throw new IllegalStateException("Ce crédit est déjà entièrement payé — on ne peut pas le passer en perte");
        }
        if (credit.getStatut() == StatutCredit.PERTE) {
            throw new IllegalStateException("Ce crédit est déjà passé en perte");
        }
        if (credit.getMontantRestant() == null || credit.getMontantRestant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Aucun reste dû sur ce crédit");
        }

        credit.setStatut(StatutCredit.PERTE);
        credit.setDatePassageEnPerte(tenantService.todayInTenantTz());
        creditClientRepository.save(credit);

        return CreditClientDto.fromEntity(credit);
    }

    /**
     * Aperçu de l'impact d'une suppression depuis le crédit.
     * Délègue au VenteService — le crédit n'a de sens qu'avec sa vente.
     */
    @Transactional(readOnly = true)
    public com.example.dijasaliou.dto.ImpactSuppressionVenteDto calculerImpactSuppressionDepuisCredit(String creditId) {
        CreditClientEntity credit = creditClientRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Crédit introuvable : " + creditId));
        TenantEntity tenant = tenantService.getCurrentTenant();
        if (!credit.getTenant().getTenantUuid().equals(tenant.getTenantUuid())) {
            throw new RuntimeException("Accès non autorisé");
        }
        if (credit.getVente() == null) {
            throw new IllegalStateException("Ce crédit n'a pas de vente associée — suppression impossible");
        }
        return venteService.calculerImpactSuppression(credit.getVente().getId());
    }

    /**
     * Supprime un crédit en cascade — c'est-à-dire supprime la vente associée
     * (qui à son tour supprime paiements + crédit + restaure stock).
     *
     * À utiliser après confirmation utilisateur (modale d'impact).
     */
    @Transactional
    public void supprimerCreditEtVenteEnCascade(String creditId) {
        CreditClientEntity credit = creditClientRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Crédit introuvable : " + creditId));
        TenantEntity tenant = tenantService.getCurrentTenant();
        if (!credit.getTenant().getTenantUuid().equals(tenant.getTenantUuid())) {
            throw new RuntimeException("Accès non autorisé");
        }
        if (credit.getVente() == null) {
            throw new IllegalStateException("Ce crédit n'a pas de vente associée — suppression impossible");
        }
        // Délègue au VenteService qui supprime tout en cascade (paiements + crédit + vente + stock)
        venteService.supprimerVenteEnCascade(credit.getVente().getId());
    }

    /** Détail d'un crédit (par id), filtré par tenant. */
    @Transactional(readOnly = true)
    public CreditClientDto obtenirCredit(String creditId) {
        CreditClientEntity credit = creditClientRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Crédit introuvable : " + creditId));
        TenantEntity currentTenant = tenantService.getCurrentTenant();
        if (!credit.getTenant().getTenantUuid().equals(currentTenant.getTenantUuid())) {
            throw new RuntimeException("Accès non autorisé");
        }
        return CreditClientDto.fromEntity(credit);
    }

    @Transactional(readOnly = true)
    public List<CreditClientDto> obtenirHistoriqueClient(String clientId) {
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable : " + clientId));
        TenantEntity currentTenant = tenantService.getCurrentTenant();
        if (!client.getTenant().getTenantUuid().equals(currentTenant.getTenantUuid())) {
            throw new RuntimeException("Accès non autorisé");
        }
        return creditClientRepository.findByClientOrderByCreatedDateAsc(client).stream()
                .map(CreditClientDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Met à jour le montant du crédit actif d'une vente (quand le prix total de la vente change).
     * Recalcule montantRestant = newPrixTotal - montantDéjàPayé.
     * Ajuste la dette du client en conséquence.
     */
    @Transactional
    public void mettreAJourCreditDeLaVente(String venteId, BigDecimal newPrixTotal) {
        List<CreditClientEntity> credits = creditClientRepository.findByVenteId(venteId);
        for (CreditClientEntity credit : credits) {
            if (credit.getStatut() != StatutCredit.SOLDE) {
                BigDecimal montantPaye = credit.getMontantInitial().subtract(credit.getMontantRestant());
                BigDecimal newMontantRestant = newPrixTotal.subtract(montantPaye).max(BigDecimal.ZERO);

                // Ajuster la dette du client selon la différence
                BigDecimal diff = newMontantRestant.subtract(credit.getMontantRestant());
                ClientEntity client = credit.getClient();
                if (client != null && diff.compareTo(BigDecimal.ZERO) != 0) {
                    client.setDetteTotale(client.getDetteTotale().add(diff).max(BigDecimal.ZERO));
                    clientRepository.save(client);
                }

                credit.setMontantInitial(newPrixTotal);
                credit.setMontantRestant(newMontantRestant);
                if (newMontantRestant.compareTo(BigDecimal.ZERO) == 0) {
                    credit.setStatut(StatutCredit.SOLDE);
                }
                creditClientRepository.save(credit);
            }
        }
    }

    /**
     * Solde tous les crédits actifs liés à une vente (quand le mode de paiement passe de CREDIT à autre chose).
     * Le crédit est conservé en historique avec statut SOLDE et montantRestant = 0.
     */
    @Transactional
    public void solderCreditsDeLaVente(String venteId) {
        List<CreditClientEntity> credits = creditClientRepository.findByVenteId(venteId);
        for (CreditClientEntity credit : credits) {
            if (credit.getStatut() != StatutCredit.SOLDE) {
                ClientEntity client = credit.getClient();
                if (client != null) {
                    BigDecimal nouvelleDette = client.getDetteTotale().subtract(credit.getMontantRestant());
                    client.setDetteTotale(nouvelleDette.max(BigDecimal.ZERO));
                    clientRepository.save(client);
                }
                credit.setMontantRestant(BigDecimal.ZERO);
                credit.setStatut(StatutCredit.SOLDE);
                creditClientRepository.save(credit);
            }
        }
    }

    /**
     * Option A — suppression d'une vente :
     * - Solde les crédits actifs (ajuste la dette client)
     * - Nullifie la référence vente sur TOUS les crédits (pour permettre la suppression de la vente sans violer la FK)
     * - L'historique des crédits et paiements est conservé
     */
    @Transactional
    public void solderEtDetacherCreditsDeLaVente(String venteId) {
        List<CreditClientEntity> credits = creditClientRepository.findByVenteId(venteId);
        for (CreditClientEntity credit : credits) {
            if (credit.getStatut() != StatutCredit.SOLDE) {
                ClientEntity client = credit.getClient();
                if (client != null) {
                    BigDecimal nouvelleDette = client.getDetteTotale().subtract(credit.getMontantRestant());
                    client.setDetteTotale(nouvelleDette.max(BigDecimal.ZERO));
                    clientRepository.save(client);
                }
                credit.setMontantRestant(BigDecimal.ZERO);
                credit.setStatut(StatutCredit.SOLDE);
            }
            // Détacher la référence à la vente pour éviter la violation de FK lors du DELETE
            credit.setVente(null);
            creditClientRepository.save(credit);
        }
    }

    @Transactional
    public void supprimerCreditsDeLaVente(String venteId) {
        List<CreditClientEntity> credits = creditClientRepository.findByVenteId(venteId);
        for (CreditClientEntity credit : credits) {
            if (credit.getStatut() != StatutCredit.SOLDE) {
                ClientEntity client = credit.getClient();
                if (client != null) {
                    BigDecimal nouvelleDette = client.getDetteTotale().subtract(credit.getMontantRestant());
                    client.setDetteTotale(nouvelleDette.max(BigDecimal.ZERO));
                    clientRepository.save(client);
                }
            }
            creditClientRepository.delete(credit);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenirStats() {
        Map<String, Object> stats = new HashMap<>();
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();

        BigDecimal montantTotalDu = creditClientRepository.sumMontantRestantActif(StatutCredit.SOLDE, tenantUuid);
        if (montantTotalDu == null) montantTotalDu = BigDecimal.ZERO;

        BigDecimal montantInitialTotal = creditClientRepository.sumMontantInitialActif(StatutCredit.SOLDE, tenantUuid);
        if (montantInitialTotal == null) montantInitialTotal = BigDecimal.ZERO;

        double tauxRecouvrement = 0.0;
        if (montantInitialTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal montantPaye = montantInitialTotal.subtract(montantTotalDu);
            tauxRecouvrement = montantPaye.multiply(new BigDecimal("100"))
                    .divide(montantInitialTotal, 1, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
        }

        stats.put("totalEnAttente", montantTotalDu);
        stats.put("montantTotalDu", montantTotalDu);
        stats.put("nombreCreditsActifs", creditClientRepository.countCreditsActifs(StatutCredit.SOLDE, tenantUuid));
        stats.put("nombreClientsCrediteurs", creditClientRepository.countClientsCrediteurs(StatutCredit.SOLDE, tenantUuid));
        stats.put("creditsEnRetard", creditClientRepository.countCreditsEnRetard(StatutCredit.SOLDE, tenantService.todayInTenantTz(), tenantUuid));
        stats.put("tauxRecouvrement", tauxRecouvrement);
        return stats;
    }
}
