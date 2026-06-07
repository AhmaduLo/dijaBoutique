package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.VenteEntity;
import com.example.dijasaliou.entity.VenteLotConsommationEntity;
import com.example.dijasaliou.repository.AchatRepository;
import com.example.dijasaliou.repository.VenteLotConsommationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de calcul du bénéfice net par méthode FIFO (First In, First Out).
 *
 * Principe :
 *   - Chaque achat constitue un "lot" avec un prix d'achat et une quantité restante.
 *   - À chaque vente, on puise dans les lots du plus ancien au plus récent.
 *   - On enregistre dans vente_lot_consommation chaque "puisage" avec un snapshot
 *     du prix d'achat → le bénéfice de la vente est figé et traçable lot par lot.
 *
 * Ce service ne fait que la logique FIFO : il NE SAUVEGARDE PAS la vente elle-même.
 * C'est à l'appelant (VenteService) de gérer la transaction globale.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FifoCalculService {

    private final AchatRepository achatRepository;
    private final VenteLotConsommationRepository consommationRepository;

    /**
     * Consomme le stock FIFO pour une vente et crée les lignes de consommation.
     *
     * Comportement :
     *   - Récupère tous les lots disponibles du produit (quantite_restante > 0), triés par date ASC.
     *   - Puise dans chaque lot jusqu'à couvrir la quantité vendue.
     *   - Décrémente quantite_restante des lots utilisés.
     *   - Crée et sauvegarde les VenteLotConsommationEntity correspondantes.
     *   - Si le stock total des lots est insuffisant, consomme ce qui est disponible
     *     et logge un avertissement (la vente reste valide, le delta est non-tracé).
     *
     * Doit être appelé dans une transaction (côté VenteService).
     *
     * @param vente La vente à associer (déjà sauvegardée, avec id non null).
     * @return La liste des lignes de consommation créées (peut être vide si aucun lot disponible).
     */
    @Transactional
    public List<VenteLotConsommationEntity> consommerStockFifo(VenteEntity vente) {
        if (vente == null || vente.getId() == null) {
            throw new IllegalArgumentException("La vente doit être sauvegardée (id non null) avant le FIFO");
        }
        if (vente.getQuantite() == null || vente.getQuantite() <= 0) {
            return List.of();
        }

        final TenantEntity tenant = vente.getTenant();
        final String nomProduit = vente.getNomProduit();
        final BigDecimal prixVenteUnitaire = vente.getPrixUnitaire();

        // 1. Récupérer les lots disponibles, triés par date ASC (FIFO)
        List<AchatEntity> lots = achatRepository.findLotsDisponiblesFifo(nomProduit, tenant);

        if (lots.isEmpty()) {
            log.warn("FIFO : aucun lot d'achat disponible pour le produit '{}' (tenant={}). " +
                    "Vente {} créée sans calcul de bénéfice.",
                    nomProduit, tenant.getTenantUuid(), vente.getId());
            return List.of();
        }

        // 2. Puiser dans chaque lot jusqu'à couvrir la quantité vendue
        double quantiteRestanteAVendre = vente.getQuantite();
        List<VenteLotConsommationEntity> consommations = new ArrayList<>();

        for (AchatEntity lot : lots) {
            if (quantiteRestanteAVendre <= 0) {
                break;
            }

            double quantiteDispoLot = lot.getQuantiteRestante() != null ? lot.getQuantiteRestante() : 0.0;
            if (quantiteDispoLot <= 0) {
                continue;
            }

            double quantiteAPuiser = Math.min(quantiteDispoLot, quantiteRestanteAVendre);

            VenteLotConsommationEntity consommation = VenteLotConsommationEntity.builder()
                    .vente(vente)
                    .achat(lot)
                    .tenant(tenant)
                    .quantiteConsommee(quantiteAPuiser)
                    .prixAchatUnitaireSnapshot(lot.getPrixUnitaire())
                    .prixVenteUnitaireSnapshot(prixVenteUnitaire)
                    .dateVenteSnapshot(vente.getDateVente())
                    .build();
            consommation.recalculerBenefice();

            consommations.add(consommation);

            // Décrémenter le lot
            lot.setQuantiteRestante(quantiteDispoLot - quantiteAPuiser);
            quantiteRestanteAVendre -= quantiteAPuiser;
        }

        // 3. Sauvegarder les consommations et les lots modifiés en un seul flush
        consommationRepository.saveAll(consommations);
        // achatRepository.saveAll(lots) : optionnel, les lots sont déjà gérés par le contexte JPA
        // mais on le fait explicitement pour la clarté du flush
        achatRepository.saveAll(lots);

        if (quantiteRestanteAVendre > 0) {
            log.warn("FIFO : stock insuffisant pour le produit '{}' (tenant={}). " +
                    "Vente {} : {} unité(s) non tracée(s) (vendues sans lot d'achat correspondant).",
                    nomProduit, tenant.getTenantUuid(), vente.getId(), quantiteRestanteAVendre);
        }

        BigDecimal beneficeTotal = consommations.stream()
                .map(VenteLotConsommationEntity::getBeneficeTotalLigne)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.debug("FIFO : vente {} → {} ligne(s) de consommation, bénéfice total = {}",
                vente.getId(), consommations.size(), beneficeTotal);

        return consommations;
    }

    /**
     * Annule la consommation FIFO d'une vente : rend les unités aux lots et supprime
     * les lignes de consommation. À utiliser lors d'une suppression de vente ou
     * d'une modification qui nécessite de refaire le FIFO.
     *
     * @param venteId L'id de la vente dont on annule la consommation.
     */
    @Transactional
    public void annulerConsommationFifo(String venteId) {
        if (venteId == null) {
            return;
        }

        List<VenteLotConsommationEntity> consommations = consommationRepository.findByVenteId(venteId);
        if (consommations.isEmpty()) {
            return;
        }

        // Rendre les unités aux lots
        for (VenteLotConsommationEntity c : consommations) {
            AchatEntity lot = c.getAchat();
            double quantiteActuelle = lot.getQuantiteRestante() != null ? lot.getQuantiteRestante() : 0.0;
            lot.setQuantiteRestante(quantiteActuelle + c.getQuantiteConsommee());
        }
        achatRepository.saveAll(consommations.stream().map(VenteLotConsommationEntity::getAchat).toList());

        // Supprimer les consommations
        consommationRepository.deleteAll(consommations);

        log.debug("FIFO : annulation de {} ligne(s) de consommation pour la vente {}",
                consommations.size(), venteId);
    }

    /**
     * Refait le calcul FIFO pour une vente existante :
     *   1. Annule la consommation actuelle (rend les unités aux lots).
     *   2. Refait la consommation avec l'état actuel de la vente.
     *
     * À utiliser lors d'une modification de vente (changement de produit, quantité ou prix).
     *
     * @param vente La vente à recalculer (id non null, déjà à jour en base).
     * @return Les nouvelles lignes de consommation.
     */
    @Transactional
    public List<VenteLotConsommationEntity> recalculerFifo(VenteEntity vente) {
        annulerConsommationFifo(vente.getId());
        return consommerStockFifo(vente);
    }
}
