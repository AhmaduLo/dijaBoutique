package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.StockAlertHistory;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.UserNotificationType;
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
    private final UserPushNotificationService userPushService;

    // Seuils d'alerte (en ordre décroissant)
    private static final double[] SEUILS_ALERTE = {15, 10, 5, 0};

    public StockAlertService(AchatRepository achatRepository,
                             VenteRepository venteRepository,
                             StockAlertHistoryRepository stockAlertHistoryRepository,
                             UserRepository userRepository,
                             EmailService emailService,
                             TenantService tenantService,
                             UserPushNotificationService userPushService) {
        this.achatRepository = achatRepository;
        this.venteRepository = venteRepository;
        this.stockAlertHistoryRepository = stockAlertHistoryRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.tenantService = tenantService;
        this.userPushService = userPushService;
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

        // IMPORTANT : Les alertes de stock sont disponibles pour les plans PRO et BUSINESS
        if (tenant.getPlan() != TenantEntity.Plan.PRO && tenant.getPlan() != TenantEntity.Plan.BUSINESS) {
            log.debug("Alertes de stock désactivées pour le plan {} (entreprise: {})",
                    tenant.getPlan(), tenant.getNomEntreprise());
            return;
        }

        // 1. Calculer le stock actuel du produit
        double stockActuel = calculerStockActuel(nomProduit, tenant);

        log.debug("Stock actuel pour {} : {} unités (tenant: {})",
                nomProduit, stockActuel, tenant.getNomEntreprise());

        // 2. Vérifier si le stock actuel correspond EXACTEMENT à un seuil d'alerte
        // SEUILS_ALERTE = {15, 10, 5, 0}
        // On envoie une alerte UNIQUEMENT si stock == 15, stock == 10, stock == 5 ou stock == 0
        // PAS si stock < ou > à ces valeurs

        boolean seuilExactTrouve = false;
        for (double seuil : SEUILS_ALERTE) {
            if (Double.compare(stockActuel, seuil) == 0) {
                seuilExactTrouve = true;
                break;
            }
        }

        // Si le stock ne correspond à aucun seuil exact, pas d'alerte
        if (!seuilExactTrouve) {
            log.debug("Stock de {} ({} unités) ne correspond à aucun seuil d'alerte exact",
                    nomProduit, stockActuel);
            return;
        }

        // Le stock correspond exactement à un seuil, vérifier si l'alerte a déjà été envoyée
        int stockActuelInt = (int) stockActuel;
        LocalDateTime ilYa24h = LocalDateTime.now().minusHours(24);
        boolean alerteDejaEnvoyee = stockAlertHistoryRepository.existsRecentAlert(
                nomProduit, stockActuelInt, tenant, ilYa24h);

        if (alerteDejaEnvoyee) {
            log.debug("Alerte déjà envoyée pour {} au seuil exact {} dans les dernières 24h",
                    nomProduit, stockActuel);
            return;
        }

        log.info("Stock EXACTEMENT égal à un seuil ! Envoi d'alerte pour {} - Stock: {}",
                nomProduit, stockActuel);

        // 3. Récupérer l'admin du tenant pour l'email
        UserEntity admin = userRepository.findFirstByTenantAndRole(tenant, UserEntity.Role.ADMIN)
                .orElse(null);

        if (admin == null) {
            log.error("Aucun admin trouvé pour le tenant {} - impossible d'envoyer l'alerte",
                    tenant.getNomEntreprise());
            return;
        }

        // 4. Envoyer l'email d'alerte (le seuil = stock actuel car on vérifie l'égalité exacte)
        String userName = admin.getPrenom() + " " + admin.getNom();
        emailService.sendStockAlertEmail(
                admin.getEmail(),
                userName,
                tenant.getNomEntreprise(),
                nomProduit,
                stockActuelInt,
                stockActuelInt  // Le seuil d'alerte = le stock actuel (car stock == seuil)
        );

        // 4bis. Envoyer aussi une notification push (respecte les préférences user)
        // Type RUPTURE si stock == 0, sinon STOCK_BAS pour les paliers 15/10/5.
        // Non bloquant (@Async) — un échec push n'empêche pas l'enregistrement historique.
        UserNotificationType typeNotif = stockActuelInt == 0
                ? UserNotificationType.RUPTURE
                : UserNotificationType.STOCK_BAS;
        String titrePush = stockActuelInt == 0
                ? "🛑 Rupture de stock — " + nomProduit
                : "📉 Stock faible — " + nomProduit + " (" + stockActuelInt + " restants)";
        String bodyPush = stockActuelInt == 0
                ? "Le produit est en rupture. Réapprovisionner rapidement pour ne plus perdre de ventes."
                : "Le stock est descendu à " + stockActuelInt + " unités. Pense à commander.";
        userPushService.notifyUser(admin, typeNotif, titrePush, bodyPush, "/stock");

        // 5. Enregistrer l'alerte dans l'historique
        StockAlertHistory history = StockAlertHistory.builder()
                .nomProduit(nomProduit)
                .seuilAlerte(stockActuelInt)  // Le seuil = stock actuel
                .stockActuel(stockActuelInt)
                .emailDestinataire(admin.getEmail())
                .tenant(tenant)
                .dateEnvoi(LocalDateTime.now())
                .build();

        stockAlertHistoryRepository.save(history);

        log.info("Alerte de stock envoyée pour {} (stock: {}, seuil: {}) à {} (entreprise: {})",
                nomProduit, stockActuel, stockActuel, admin.getEmail(), tenant.getNomEntreprise());
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
    private double calculerStockActuel(String nomProduit, TenantEntity tenant) {
        // Calculer le total des achats pour ce produit
        Double totalAchats = achatRepository.sumQuantiteByNomProduitAndTenant(nomProduit, tenant);
        if (totalAchats == null) {
            totalAchats = 0.0;
        }

        // Calculer le total des ventes pour ce produit
        Double totalVentes = venteRepository.sumQuantiteByNomProduitAndTenant(nomProduit, tenant);
        if (totalVentes == null) {
            totalVentes = 0.0;
        }

        return totalAchats - totalVentes;
    }

    /**
     * Récupère l'historique des alertes pour un produit
     *
     * @param nomProduit Nom du produit
     * @return Liste des alertes envoyées pour ce produit
     */
    @Transactional(readOnly = true)
    public List<StockAlertHistory> getHistoriqueAlertes(String nomProduit) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        // Note: Vous devez créer cette méthode dans le repository si nécessaire
        // Pour l'instant, on retourne une liste vide
        return List.of();
    }
}
