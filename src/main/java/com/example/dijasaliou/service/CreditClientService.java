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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreditClientService {

    private final CreditClientRepository creditClientRepository;
    private final PaiementCreditRepository paiementCreditRepository;
    private final ClientRepository clientRepository;
    private final VenteRepository venteRepository;
    private final TenantService tenantService;

    /**
     * Appelé par VenteService quand mode_paiement = CREDIT
     */
    @Transactional
    public CreditClientEntity creerCreditDepuisVente(VenteEntity vente, ClientEntity client,
                                                      UserEntity employe, LocalDate dateEcheance) {
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
     * Enregistre un paiement (partiel ou total) sur un crédit
     */
    @Transactional
    public CreditClientDto enregistrerPaiement(Long creditId, BigDecimal montant,
                                                PaiementCreditEntity.ModePaiement modePaiement,
                                                String note, UserEntity employe) {
        // 1. Récupérer le crédit (filtre tenant actif via Hibernate)
        CreditClientEntity credit = creditClientRepository.findById(creditId)
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

        // 5. Créer le paiement
        PaiementCreditEntity paiement = PaiementCreditEntity.builder()
                .credit(credit)
                .montantPaye(montant)
                .modePaiement(modePaiement)
                .datePaiement(LocalDate.now())
                .employe(employe)
                .employeNom(employe != null ? employe.getPrenom() + " " + employe.getNom() : "Inconnu")
                .note(note)
                .build();
        paiementCreditRepository.save(paiement);

        // 6. Mettre à jour le montant restant
        BigDecimal nouveauRestant = credit.getMontantRestant().subtract(montant);
        credit.setMontantRestant(nouveauRestant);

        ClientEntity client = credit.getClient();

        if (nouveauRestant.compareTo(BigDecimal.ZERO) == 0) {
            // Crédit soldé
            credit.setStatut(StatutCredit.SOLDE);
            BigDecimal nouvelleDette = client.getDetteTotale().subtract(credit.getMontantInitial());
            client.setDetteTotale(nouvelleDette.max(BigDecimal.ZERO));
            // Marquer la vente comme soldée
            if (credit.getVente() != null) {
                credit.getVente().setEstSoldee(true);
                venteRepository.save(credit.getVente());
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

    public PagedResponse<CreditClientDto> obtenirCredits(int page, int size, String search, String statut) {
        Pageable pageable = PageRequest.of(page, size);
        StatutCredit statutEnum = null;
        if (statut != null && !statut.isBlank() && !statut.equals("TOUS")) {
            try {
                statutEnum = StatutCredit.valueOf(statut);
            } catch (IllegalArgumentException ignored) {}
        }
        Page<CreditClientEntity> pageResult = creditClientRepository.findAllWithSearch(
                statutEnum,
                (search != null && !search.isBlank()) ? search : null,
                pageable);
        Page<CreditClientDto> dtoPage = pageResult.map(CreditClientDto::fromEntity);
        return PagedResponse.from(dtoPage);
    }

    public List<PaiementCreditDto> obtenirPaiements(Long creditId) {
        CreditClientEntity credit = creditClientRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Crédit introuvable : " + creditId));
        return paiementCreditRepository.findByCreditOrderByCreatedDateDesc(credit).stream()
                .map(PaiementCreditDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<CreditClientDto> obtenirHistoriqueClient(Long clientId) {
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable : " + clientId));
        return creditClientRepository.findByClientOrderByCreatedDateAsc(client).stream()
                .map(CreditClientDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Map<String, Object> obtenirStats() {
        Map<String, Object> stats = new HashMap<>();
        BigDecimal montantTotalDu = creditClientRepository.sumMontantRestantActif(StatutCredit.SOLDE);
        if (montantTotalDu == null) montantTotalDu = BigDecimal.ZERO;

        // Calculer le taux de recouvrement = montant payé / montant initial total
        BigDecimal montantInitialTotal = creditClientRepository.sumMontantInitialActif(StatutCredit.SOLDE);
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
        stats.put("nombreCreditsActifs", creditClientRepository.countCreditsActifs(StatutCredit.SOLDE));
        stats.put("nombreClientsCrediteurs", creditClientRepository.countClientsCrediteurs(StatutCredit.SOLDE));
        stats.put("creditsEnRetard", creditClientRepository.countCreditsEnRetard(StatutCredit.SOLDE, LocalDate.now()));
        stats.put("tauxRecouvrement", tauxRecouvrement);
        return stats;
    }
}
