package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.StockAlertHistory;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.AchatRepository;
import com.example.dijasaliou.repository.StockAlertHistoryRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.repository.VenteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour gérer les alertes de stock
 *
 * FONCTIONNALITÉ EXCLUSIVE AU PLAN ENTREPRISE
 *
 * Le système envoie des alertes par email quand le stock atteint les seuils suivants :
 * - 15 unités : Première alerte (stock devient bas)
 * - 10 unités : Deuxième alerte (stock faible)
 * - 5 unités : Troisième alerte (stock critique)
 * - 0 unités : Alerte finale (rupture de stock)
 *
 * LOGIQUE :
 * - Une alerte n'est envoyée qu'une seule fois par seuil
 * - Si le stock remonte puis redescend, une nouvelle alerte peut être envoyée
 * - Les alertes sont envoyées uniquement à l'admin principal du tenant
 */
@Service
@Slf4j
public class StockAlertService {

    private final AchatRepository achatRepository;
    private final VenteRepository venteRepository;
    private final StockAlertHistoryRepository stockAlertHistoryRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final TenantService tenantService;

    // Seuils d'alerte (en ordre décroissant)
    private static final int[] SEUILS_ALERTE = {15, 10, 5, 0};

    public StockAlertService(AchatRepository achatRepository,
                             VenteRepository venteRepository,
                             StockAlertHistoryRepository stockAlertHistoryRepository,
                             UserRepository userRepository,
                             EmailService emailService,
                             TenantService tenantService) {
        this.achatRepository = achatRepository;
        this.venteRepository = venteRepository;
        this.stockAlertHistoryRepository = stockAlertHistoryRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.tenantService = tenantService;
    }

    /**
     * Vérifie le stock d'un produit et envoie une alerte si nécessaire
     *
     * Cette méthode est appelée après chaque vente pour vérifier si le stock
     * a atteint un seuil d'alerte
     *
     * @param nomProduit Nom du produit vendu
     */
    @Transactional
    public void verifierEtEnvoyerAlerte(String nomProduit) {
        TenantEntity tenant = tenantService.getCurrentTenant();

        // IMPORTANT : Les alertes de stock ne sont disponibles que pour le plan ENTREPRISE
        if (tenant.getPlan() != TenantEntity.Plan.ENTREPRISE) {
            log.debug("Alertes de stock désactivées pour le plan {} (entreprise: {})",
                    tenant.getPlan(), tenant.getNomEntreprise());
            return;
        }

        // 1. Calculer le stock actuel du produit
        int stockActuel = calculerStockActuel(nomProduit, tenant);

        log.debug("Stock actuel pour {} : {} unités (tenant: {})",
                nomProduit, stockActuel, tenant.getNomEntreprise());

        // 2. Parcourir tous les seuils et envoyer une alerte pour chaque seuil non encore notifié
        // SEUILS_ALERTE = {15, 10, 5, 0}
        // Si stock = 5, on vérifie les seuils 15, 10, 5, 0
        // On envoie une alerte pour le seuil le plus bas franchi qui n'a pas encore été notifié

        LocalDateTime ilYa24h = LocalDateTime.now().minusHours(24);

        // Trouver le seuil le plus proche du stock actuel (le plus petit seuil >= stock)
        Integer seuilAEnvoyer = null;

        for (int i = SEUILS_ALERTE.length - 1; i >= 0; i--) {
            int seuil = SEUILS_ALERTE[i];

            // Si le stock est <= à ce seuil
            if (stockActuel <= seuil) {
                // Vérifier si une alerte a déjà été envoyée pour CE seuil spécifique
                boolean alerteDejaEnvoyee = stockAlertHistoryRepository.existsRecentAlert(
                        nomProduit, seuil, tenant, ilYa24h);

                if (!alerteDejaEnvoyee) {
                    // On a trouvé un seuil franchi sans alerte récente
                    seuilAEnvoyer = seuil;
                    break; // On prend le seuil le plus bas non notifié
                }
            }
        }

        // Si aucun seuil n'a besoin d'alerte
        if (seuilAEnvoyer == null) {
            log.debug("Aucune nouvelle alerte nécessaire pour {} (stock: {})", nomProduit, stockActuel);
            return;
        }

        log.info("Nouvelle alerte à envoyer pour {} - Stock: {} - Seuil: {}",
                nomProduit, stockActuel, seuilAEnvoyer);

        // 3. Récupérer l'admin du tenant pour l'email
        UserEntity admin = userRepository.findFirstByTenantAndRole(tenant, UserEntity.Role.ADMIN)
                .orElse(null);

        if (admin == null) {
            log.error("Aucun admin trouvé pour le tenant {} - impossible d'envoyer l'alerte",
                    tenant.getNomEntreprise());
            return;
        }

        // 4. Envoyer l'email d'alerte
        String userName = admin.getPrenom() + " " + admin.getNom();
        emailService.sendStockAlertEmail(
                admin.getEmail(),
                userName,
                tenant.getNomEntreprise(),
                nomProduit,
                stockActuel,
                seuilAEnvoyer
        );

        // 5. Enregistrer l'alerte dans l'historique
        StockAlertHistory history = StockAlertHistory.builder()
                .nomProduit(nomProduit)
                .seuilAlerte(seuilAEnvoyer)
                .stockActuel(stockActuel)
                .emailDestinataire(admin.getEmail())
                .tenant(tenant)
                .dateEnvoi(LocalDateTime.now())
                .build();

        stockAlertHistoryRepository.save(history);

        log.info("Alerte de stock envoyée pour {} (stock: {}, seuil: {}) à {} (entreprise: {})",
                nomProduit, stockActuel, seuilAEnvoyer, admin.getEmail(), tenant.getNomEntreprise());
    }

    /**
     * Calcule le stock actuel d'un produit
     *
     * Stock = Total des achats - Total des ventes
     *
     * @param nomProduit Nom du produit
     * @param tenant Tenant concerné
     * @return Stock actuel (peut être négatif si plus de ventes que d'achats)
     */
    private int calculerStockActuel(String nomProduit, TenantEntity tenant) {
        // Calculer le total des achats pour ce produit
        Integer totalAchats = achatRepository.sumQuantiteByNomProduitAndTenant(nomProduit, tenant);
        if (totalAchats == null) {
            totalAchats = 0;
        }

        // Calculer le total des ventes pour ce produit
        Integer totalVentes = venteRepository.sumQuantiteByNomProduitAndTenant(nomProduit, tenant);
        if (totalVentes == null) {
            totalVentes = 0;
        }

        return totalAchats - totalVentes;
    }

    /**
     * Récupère l'historique des alertes pour un produit
     *
     * @param nomProduit Nom du produit
     * @return Liste des alertes envoyées pour ce produit
     */
    public List<StockAlertHistory> getHistoriqueAlertes(String nomProduit) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        // Note: Vous devez créer cette méthode dans le repository si nécessaire
        // Pour l'instant, on retourne une liste vide
        return List.of();
    }
}
