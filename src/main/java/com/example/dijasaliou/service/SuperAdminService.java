package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.AuditLogDto;
import com.example.dijasaliou.dto.FactureDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.ModifierPaiementRequest;
import com.example.dijasaliou.dto.PaiementSuperAdminDto;
import com.example.dijasaliou.dto.TenantAdminDto;
import com.example.dijasaliou.dto.UtilisateurTenantDto;
import com.example.dijasaliou.dto.ValiderPaiementRequest;
import com.example.dijasaliou.entity.AuditLog;
import com.example.dijasaliou.entity.FactureEntity;
import com.example.dijasaliou.entity.PaiementSuperAdminEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.NoteInterne;
import com.example.dijasaliou.repository.AuditLogRepository;
import com.example.dijasaliou.repository.FactureRepository;
import com.example.dijasaliou.repository.NoteInterneRepository;
import com.example.dijasaliou.repository.PaiementSuperAdminRepository;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.repository.VenteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service pour le Super Admin — accès cross-tenant
 *
 * SÉCURITÉ : Ces méthodes sont accessibles UNIQUEMENT via SuperAdminController
 * qui est protégé par @PreAuthorize("hasAuthority('SUPER_ADMIN')")
 *
 * Pas de filtre Hibernate activé pour ces requêtes :
 * le SUPER_ADMIN n'a pas de tenant_id → TenantContext est null →
 * TenantFilterAspect ne active pas le filtre → toutes les données sont visibles
 */
@Service
@Slf4j
public class SuperAdminService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final VenteRepository venteRepository;
    private final NoteInterneRepository noteInterneRepository;
    private final AuditLogRepository auditLogRepository;
    private final FactureService factureService;
    private final FactureRepository factureRepository;
    private final TenantCacheService tenantCacheService;
    private final PaiementSuperAdminRepository paiementSuperAdminRepository;
    private final AuthService authService;
    private final PushNotificationService pushService;

    @PersistenceContext
    private EntityManager entityManager;

    public SuperAdminService(TenantRepository tenantRepository,
                             UserRepository userRepository,
                             VenteRepository venteRepository,
                             NoteInterneRepository noteInterneRepository,
                             AuditLogRepository auditLogRepository,
                             FactureService factureService,
                             FactureRepository factureRepository,
                             TenantCacheService tenantCacheService,
                             PaiementSuperAdminRepository paiementSuperAdminRepository,
                             @Lazy AuthService authService,
                             PushNotificationService pushService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.venteRepository = venteRepository;
        this.noteInterneRepository = noteInterneRepository;
        this.auditLogRepository = auditLogRepository;
        this.factureService = factureService;
        this.factureRepository = factureRepository;
        this.tenantCacheService = tenantCacheService;
        this.paiementSuperAdminRepository = paiementSuperAdminRepository;
        this.authService = authService;
        this.pushService = pushService;
    }

    /**
     * Retourne tous les tenants avec leurs stats — paginé.
     * Le filtre {@code activite} permet de ne garder que les tenants matchant
     * une fenêtre d'activité d'usage (par dernière connexion d'au moins un user) :
     *   - "today"        : connecté aujourd'hui
     *   - "week"         : connecté cette semaine
     *   - "month"        : connecté ce mois
     *   - "inactive_30d" : aucun user connecté depuis 30 jours (ou jamais)
     *   - "ghost"        : créé > 5 mois sans qu'aucun user ne se soit jamais connecté
     *   - null/vide      : tous les tenants
     */
    public PagedResponse<TenantAdminDto> getAllTenants(int page, int size, String search, String activite,
                                                       String emailVerifie, String aVendu) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String searchParam = (search != null && !search.isBlank()) ? search : null;

        boolean hasActivite = activite != null && !activite.isBlank();
        boolean hasEmailFilter = emailVerifie != null && !emailVerifie.isBlank();
        boolean hasVenduFilter = aVendu != null && !aVendu.isBlank();

        // Aucun filtre custom → comportement direct paginé par la DB
        if (!hasActivite && !hasEmailFilter && !hasVenduFilter) {
            Page<TenantEntity> tenantsPage = tenantRepository.findByDeletedFalseWithSearch(searchParam, pageable);
            return PagedResponse.from(tenantsPage.map(this::toDto));
        }

        // Au moins un filtre custom → fetch tout, filtre en mémoire, pagine manuellement.
        // Acceptable jusqu'à quelques centaines de tenants. Les filtres se cumulent.
        List<TenantEntity> tous = tenantRepository
                .findByDeletedFalseWithSearch(searchParam, PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "id")))
                .getContent();

        // Filtre activité (réutilise la logique existante)
        if (hasActivite) {
            Set<Long> ids = getTenantIdsForActivite(activite);
            tous = tous.stream().filter(t -> ids.contains(t.getId())).toList();
        }

        // Filtre email vérifié (oui / non) — basé sur une seule requête optimisée
        if (hasEmailFilter) {
            Set<Long> verifiedIds = Set.copyOf(tenantRepository.findTenantIdsWithVerifiedEmail());
            boolean wantVerified = "oui".equalsIgnoreCase(emailVerifie);
            tous = tous.stream()
                    .filter(t -> verifiedIds.contains(t.getId()) == wantVerified)
                    .toList();
        }

        // Filtre "a déjà vendu" (oui / non) — basé sur une seule requête optimisée
        if (hasVenduFilter) {
            Set<Long> idsWithVentes = Set.copyOf(venteRepository.findDistinctTenantIdsWithVentes());
            boolean wantSold = "oui".equalsIgnoreCase(aVendu);
            tous = tous.stream()
                    .filter(t -> idsWithVentes.contains(t.getId()) == wantSold)
                    .toList();
        }

        // Pagination manuelle sur la liste filtrée
        int total = tous.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<TenantAdminDto> contentDto = tous.subList(from, to).stream().map(this::toDto).toList();

        return PagedResponse.<TenantAdminDto>builder()
                .content(contentDto)
                .currentPage(page)
                .pageSize(size)
                .totalElements(total)
                .totalPages((int) Math.ceil((double) total / size))
                .first(page == 0)
                .last(to >= total)
                .build();
    }

    /**
     * Compteurs globaux pour les filtres du super admin.
     * Renvoie : nombre total de tenants (non supprimés), nombre dont l'admin a vérifié son email,
     * nombre dont l'admin ne l'a PAS vérifié, nombre ayant au moins une vente, nombre n'ayant pas
     * encore vendu. Permet d'afficher les compteurs dans les sélecteurs côté UI.
     */
    public java.util.Map<String, Long> getTenantsStats() {
        java.util.Set<Long> nonDeletedIds = tenantRepository.findByDeletedFalse().stream()
                .map(TenantEntity::getId).collect(Collectors.toSet());
        long total = nonDeletedIds.size();
        // Note : on intersecte avec nonDeletedIds pour ne pas compter les tenants supprimés.
        long emailVerifie = tenantRepository.findTenantIdsWithVerifiedEmail().stream()
                .filter(nonDeletedIds::contains).count();
        long aVendu = venteRepository.findDistinctTenantIdsWithVentes().stream()
                .filter(nonDeletedIds::contains).count();
        java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
        stats.put("total", total);
        stats.put("emailVerifie", emailVerifie);
        stats.put("emailNonVerifie", Math.max(0, total - emailVerifie));
        stats.put("aVendu", aVendu);
        stats.put("pasVendu", Math.max(0, total - aVendu));
        return stats;
    }

    /** IDs des tenants matchant un filtre activité donné. */
    private Set<Long> getTenantIdsForActivite(String activite) {
        LocalDateTime now = LocalDateTime.now();
        return switch (activite.toLowerCase()) {
            case "today" ->
                    tenantRepository.findActiveSince(now.toLocalDate().atStartOfDay())
                            .stream().map(TenantEntity::getId).collect(Collectors.toSet());
            case "week" ->
                    tenantRepository.findActiveSince(now.minusDays(7))
                            .stream().map(TenantEntity::getId).collect(Collectors.toSet());
            case "month" ->
                    tenantRepository.findActiveSince(now.minusDays(30))
                            .stream().map(TenantEntity::getId).collect(Collectors.toSet());
            case "inactive_30d" ->
                    tenantRepository.findInactiveSince(now.minusDays(30))
                            .stream().map(TenantEntity::getId).collect(Collectors.toSet());
            case "ghost" ->
                    tenantRepository.findFantomes(now.minusMonths(5))
                            .stream().map(TenantEntity::getId).collect(Collectors.toSet());
            default -> tenantRepository.findByDeletedFalse()
                    .stream().map(TenantEntity::getId).collect(Collectors.toSet());
        };
    }

    /**
     * Retourne les tenants supprimés (deleted = true)
     */
    public List<TenantAdminDto> getSupprimesTenants() {
        return tenantRepository.findByDeletedTrue().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Retourne tous les tenants sans pagination (pour les stats globales uniquement)
     */
    private List<TenantEntity> getAllTenantsForStats() {
        return tenantRepository.findByDeletedFalse();
    }

    /**
     * Retourne les détails d'un tenant par ID
     */
    public TenantAdminDto getTenantById(Long id) {
        TenantEntity tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + id));
        return toDto(tenant);
    }

    /**
     * Activer un tenant
     */
    @Transactional
    public void activerTenant(Long id) {
        TenantEntity tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + id));
        tenant.setActif(true);
        tenantRepository.save(tenant);
        tenantCacheService.evict(tenant.getTenantUuid());
        log.info("[SUPER_ADMIN] Tenant {} activé", tenant.getTenantUuid());
        saveLog("ACTIVATE", "Tenant activé", tenant);
    }

    /**
     * Désactiver un tenant (suspendre)
     */
    @Transactional
    public void desactiverTenant(Long id) {
        TenantEntity tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + id));
        tenant.setActif(false);
        tenantRepository.save(tenant);
        tenantCacheService.evict(tenant.getTenantUuid());
        log.info("[SUPER_ADMIN] Tenant {} désactivé", tenant.getTenantUuid());
        saveLog("DEACTIVATE", "Tenant suspendu", tenant);
    }

    /**
     * Changer le plan d'un tenant et prolonger l'abonnement
     *
     * @param id    ID du tenant
     * @param plan  Nouveau plan
     * @param jours Nombre de jours d'abonnement à partir d'aujourd'hui
     */
    @Transactional
    public TenantAdminDto changerPlan(Long id, TenantEntity.Plan plan, int jours) {
        TenantEntity tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + id));

        TenantEntity.Plan ancienPlan = tenant.getPlan();
        LocalDateTime ancienPlanExpiration = tenant.getDateExpiration();
        LocalDateTime now = LocalDateTime.now();
        tenant.setPlan(plan);
        tenant.setDateDebutEssai(now); // Sert aussi de "date de début d'abonnement" (affichée à l'admin tenant)
        tenant.setDateExpiration(now.plusDays(jours));
        tenant.setActif(true);

        if (!Boolean.TRUE.equals(tenant.getEssaiUtilise())) {
            tenant.setEssaiUtilise(true);
        }

        tenantRepository.save(tenant);
        tenantCacheService.evict(tenant.getTenantUuid());
        log.info("[SUPER_ADMIN] Tenant {} : plan {} → {} (expire dans {} jours, était: {})",
                tenant.getTenantUuid(), ancienPlan, plan, jours, ancienPlanExpiration);

        // Créer une facture pour tracer ce changement de plan
        factureService.creerFacture(tenant, plan, jours, FactureEntity.StatutFacture.MANUELLE, null);

        saveLog("CHANGE_PLAN", ancienPlan + " → " + plan + " (" + jours + " jours)", tenant);

        return toDto(tenant);
    }

    /**
     * Liste toutes les factures d'un tenant
     */
    public List<FactureDto> getFacturesByTenant(Long tenantId) {
        return factureService.getFacturesByTenant(tenantId);
    }

    /**
     * Envoie une facture par email
     */
    @Transactional
    public FactureDto envoyerFacture(Long factureId) {
        return factureService.envoyerFactureParEmail(factureId);
    }

    /**
     * Nombre de paiements d'un tenant
     */
    public long getNbPaiements(Long tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + tenantId));
        return factureRepository.countByTenant(tenant);
    }

    /**
     * Suspendre / réactiver un tenant (bascule actif/inactif — RÉVERSIBLE)
     * Les utilisateurs ne sont pas touchés.
     */
    @Transactional
    public void basculerSuspensionTenant(Long id) {
        TenantEntity tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + id));

        if (Boolean.TRUE.equals(tenant.getDeleted())) {
            throw new IllegalStateException("Impossible de modifier un tenant définitivement supprimé");
        }

        boolean nouvelEtat = !Boolean.TRUE.equals(tenant.getActif());
        tenant.setActif(nouvelEtat);
        tenantRepository.save(tenant);
        tenantCacheService.evict(tenant.getTenantUuid());

        String action = nouvelEtat ? "ACTIVATE" : "DEACTIVATE";
        String msg = nouvelEtat ? "Tenant réactivé (suspension levée)" : "Tenant suspendu";
        log.info("[SUPER_ADMIN] Tenant {} → actif={}", tenant.getTenantUuid(), nouvelEtat);
        saveLog(action, msg, tenant);
    }

    /**
     * Restaurer un tenant supprimé (soft delete → actif)
     * - tenant.deleted = false + actif = true + dateSuppression = null
     * - tous les utilisateurs du tenant remis actif
     */
    @Transactional
    public void restaurerTenant(Long id) {
        TenantEntity tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + id));

        // Remettre le tenant actif
        tenant.setDeleted(false);
        tenant.setActif(true);
        tenant.setDateSuppression(null);
        tenantRepository.save(tenant);

        // Remettre actif tous les utilisateurs du tenant
        List<UserEntity> users = userRepository.findByTenantId(tenant.getId());
        for (UserEntity user : users) {
            user.setDeleted(false);
            user.setDateSuppression(null);
        }
        userRepository.saveAll(users);

        tenantCacheService.evict(tenant.getTenantUuid());
        log.info("[SUPER_ADMIN] Tenant {} restauré ({} utilisateurs réactivés)",
                tenant.getTenantUuid(), users.size());
        saveLog("RESTORE", "Tenant restauré après suppression", tenant);
    }

    /**
     * Suppression définitive d'un tenant (IRRÉVERSIBLE depuis l'interface)
     * - tenant.deleted = true + dateSuppression
     * - tous les utilisateurs soft-deletés
     * Les données restent en base.
     */
    @Transactional
    public void supprimerTenant(Long id) {
        TenantEntity tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + id));

        // Soft-delete tous les utilisateurs du tenant (requête ciblée, sans filtre Hibernate)
        List<UserEntity> users = userRepository.findByTenantIdAndDeletedFalse(tenant.getId());

        LocalDateTime maintenant = LocalDateTime.now();
        for (UserEntity user : users) {
            user.setDeleted(true);
            user.setDateSuppression(maintenant);
        }
        userRepository.saveAll(users);

        // Marquer le tenant comme définitivement supprimé
        tenant.setActif(false);
        tenant.setDeleted(true);
        tenant.setDateSuppression(maintenant);
        tenantRepository.save(tenant);
        tenantCacheService.evict(tenant.getTenantUuid());

        log.info("[SUPER_ADMIN] Tenant {} définitivement supprimé ({} utilisateurs désactivés)",
                tenant.getTenantUuid(), users.size());
        saveLog("DELETE", "Suppression définitive — " + users.size() + " utilisateurs désactivés", tenant);
    }

    /**
     * Stats globales de la plateforme
     */
    public Map<String, Object> getGlobalStats() {
        List<TenantEntity> tenants = getAllTenantsForStats();

        long total = tenants.size();
        long actifs = tenants.stream().filter(t -> Boolean.TRUE.equals(t.getActif())).count();
        long enEssai = tenants.stream().filter(t -> Boolean.TRUE.equals(t.getActif()) && t.essaiGratuitValide()).count();
        long expires = tenants.stream().filter(t -> {
            if (!Boolean.TRUE.equals(t.getActif())) return false;
            if (t.essaiGratuitValide()) return false;
            if (t.getPlan() == TenantEntity.Plan.GRATUIT) return true;
            return t.getDateExpiration() != null && LocalDateTime.now().isAfter(t.getDateExpiration());
        }).count();
        long suspendu = tenants.stream().filter(t -> !Boolean.TRUE.equals(t.getActif())).count();

        // Répartition par plan
        Map<String, Long> parPlan = new HashMap<>();
        for (TenantEntity.Plan plan : TenantEntity.Plan.values()) {
            parPlan.put(plan.name(), tenants.stream().filter(t -> t.getPlan() == plan).count());
        }

        // Abonnements expirant dans 7 jours
        LocalDateTime dans7Jours = LocalDateTime.now().plusDays(7);
        long expirantBientot = tenants.stream().filter(t ->
                Boolean.TRUE.equals(t.getActif())
                && t.getDateExpiration() != null
                && t.getDateExpiration().isAfter(LocalDateTime.now())
                && t.getDateExpiration().isBefore(dans7Jours)
        ).count();

        // Total utilisateurs
        long totalUtilisateurs = userRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTenants", total);
        stats.put("actifs", actifs);
        stats.put("enEssai", enEssai);
        stats.put("expires", expires);
        stats.put("suspendu", suspendu);
        stats.put("parPlan", parPlan);
        stats.put("expirantBientot", expirantBientot);
        stats.put("totalUtilisateurs", totalUtilisateurs);

        // Taux de conversion essai → payant
        // Un tenant est "converti" seulement s'il a au moins un paiement réel enregistré.
        long totalEssaiHistorique = tenantRepository.countByEssaiUtiliseTrue();
        long totalConvertis = paiementSuperAdminRepository.countDistinctTenants();
        double tauxConversion = totalEssaiHistorique > 0 ? (totalConvertis * 100.0 / totalEssaiHistorique) : 0;
        stats.put("totalEssaiHistorique", totalEssaiHistorique);
        stats.put("totalConvertis", totalConvertis);
        stats.put("tauxConversion", Math.round(tauxConversion * 10.0) / 10.0);

        return stats;
    }

    /**
     * Stats de monitoring : utilisateurs connectés, BDD, activité récente
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMonitoringStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        LocalDateTime maintenant = LocalDateTime.now();

        // 1. Utilisateurs connectés (dernière activité < 5 min)
        LocalDateTime il5min = maintenant.minusMinutes(5);
        List<UserEntity> allUsers = userRepository.findByDeletedFalse();
        long connectesMaintenant = allUsers.stream()
                .filter(u -> u.getDerniereConnexion() != null && u.getDerniereConnexion().isAfter(il5min))
                .count();
        stats.put("connectesMaintenant", connectesMaintenant);

        // 2. Dernières connexions (top 20)
        List<Map<String, Object>> dernieresConnexions = allUsers.stream()
                .filter(u -> u.getDerniereConnexion() != null)
                .sorted((a, b) -> b.getDerniereConnexion().compareTo(a.getDerniereConnexion()))
                .limit(20)
                .map(u -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("nom", u.getPrenom() + " " + u.getNom());
                    entry.put("email", u.getEmail());
                    entry.put("role", u.getRole().name());
                    entry.put("boutique", u.getTenant() != null ? u.getTenant().getNomEntreprise() : "Super Admin");
                    // tenantId : nécessaire pour le bouton "Détail" du frontend (sinon lookup
                    // local côté Angular limité à la page courante de getAllTenants).
                    entry.put("tenantId", u.getTenant() != null ? u.getTenant().getId() : null);
                    entry.put("derniereConnexion", u.getDerniereConnexion());
                    entry.put("enLigne", u.getDerniereConnexion().isAfter(il5min));
                    return entry;
                })
                .toList();
        stats.put("dernieresConnexions", dernieresConnexions);

        // 3. Boutiques actives aujourd'hui (au moins 1 utilisateur connecté aujourd'hui)
        LocalDateTime debutJour = maintenant.toLocalDate().atStartOfDay();
        long boutiquesActivesAujourdhui = allUsers.stream()
                .filter(u -> u.getDerniereConnexion() != null && u.getDerniereConnexion().isAfter(debutJour))
                .map(u -> u.getTenant() != null ? u.getTenant().getId() : -1L)
                .distinct()
                .filter(id -> id != -1L)
                .count();
        stats.put("boutiquesActivesAujourdhui", boutiquesActivesAujourdhui);

        // 4. Taille de la BDD
        try {
            @SuppressWarnings("deprecation")
            List<Object[]> dbSize = entityManager
                    .createNativeQuery("SELECT table_name, data_length, index_length FROM information_schema.tables WHERE table_schema = DATABASE()")
                    .getResultList();
            long totalDataBytes = 0;
            long totalIndexBytes = 0;
            List<Map<String, Object>> tables = new ArrayList<>();
            for (Object[] row : dbSize) {
                String tableName = row[0].toString();
                long dataLen = row[1] != null ? ((Number) row[1]).longValue() : 0;
                long indexLen = row[2] != null ? ((Number) row[2]).longValue() : 0;
                totalDataBytes += dataLen;
                totalIndexBytes += indexLen;
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("table", tableName);
                t.put("dataKB", dataLen / 1024);
                t.put("indexKB", indexLen / 1024);
                t.put("totalKB", (dataLen + indexLen) / 1024);
                tables.add(t);
            }
            long totalBytes = totalDataBytes + totalIndexBytes;
            long totalMB = totalBytes / (1024 * 1024);
            long maxMB = 1000; // 1 GB par défaut Railway
            int pourcentageBdd = maxMB > 0 ? (int) (totalMB * 100 / maxMB) : 0;

            stats.put("bddTotalMB", totalMB);
            stats.put("bddMaxMB", maxMB);
            stats.put("bddPourcentage", pourcentageBdd);
            stats.put("bddStatut", pourcentageBdd < 60 ? "OK" : pourcentageBdd < 80 ? "ATTENTION" : "CRITIQUE");
            stats.put("bddMessage", pourcentageBdd < 60
                    ? "Base de données en bonne santé"
                    : pourcentageBdd < 80
                    ? "Attention : base de données à " + pourcentageBdd + "%, pensez à upgrader"
                    : "URGENT : base de données presque pleine (" + pourcentageBdd + "%) !");
            stats.put("bddTables", tables);
        } catch (Exception e) {
            stats.put("bddTotalMB", "N/A");
            stats.put("bddMaxMB", 1000);
            stats.put("bddPourcentage", 0);
            stats.put("bddStatut", "N/A");
            stats.put("bddMessage", "Impossible de récupérer la taille de la BDD");
            stats.put("bddTables", List.of());
        }

        // 5. Connexions BDD actives + alerte
        int maxConnexions = 100;
        try {
            @SuppressWarnings("deprecation")
            Object connResult = entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM information_schema.processlist")
                    .getSingleResult();
            int connexionsActives = ((Number) connResult).intValue();
            int pourcentageConn = maxConnexions > 0 ? (connexionsActives * 100 / maxConnexions) : 0;

            stats.put("connexionsBddActives", connexionsActives);
            stats.put("connexionsBddMax", maxConnexions);
            stats.put("connexionsPourcentage", pourcentageConn);
            stats.put("connexionsStatut", pourcentageConn < 60 ? "OK" : pourcentageConn < 80 ? "ATTENTION" : "CRITIQUE");
            stats.put("connexionsMessage", pourcentageConn < 60
                    ? "Connexions normales"
                    : pourcentageConn < 80
                    ? "Charge élevée : " + connexionsActives + "/" + maxConnexions + " connexions"
                    : "CRITIQUE : connexions saturées (" + connexionsActives + "/" + maxConnexions + ") !");
        } catch (Exception e) {
            stats.put("connexionsBddActives", "N/A");
            stats.put("connexionsBddMax", maxConnexions);
            stats.put("connexionsPourcentage", 0);
            stats.put("connexionsStatut", "N/A");
            stats.put("connexionsMessage", "Impossible de récupérer les connexions");
        }

        // 6. Alertes globales
        List<Map<String, String>> alertes = new ArrayList<>();
        Object bddStatut = stats.get("bddStatut");
        if ("ATTENTION".equals(bddStatut) || "CRITIQUE".equals(bddStatut)) {
            Map<String, String> alerte = new LinkedHashMap<>();
            alerte.put("type", bddStatut.toString());
            alerte.put("message", stats.get("bddMessage").toString());
            alertes.add(alerte);
        }
        Object connStatut = stats.get("connexionsStatut");
        if ("ATTENTION".equals(connStatut) || "CRITIQUE".equals(connStatut)) {
            Map<String, String> alerte = new LinkedHashMap<>();
            alerte.put("type", connStatut.toString());
            alerte.put("message", stats.get("connexionsMessage").toString());
            alertes.add(alerte);
        }
        stats.put("alertes", alertes);

        return stats;
    }

    public List<UtilisateurTenantDto> getUtilisateursByTenant(Long tenantId) {
        return userRepository.findByTenantIdAndDeletedFalse(tenantId)
                .stream()
                .map(UtilisateurTenantDto::fromEntity)
                .toList();
    }

    /**
     * Renvoie l'email de vérification à un utilisateur d'un tenant.
     * Utilisé par le super admin pour aider un utilisateur qui n'a pas vérifié son email.
     */
    @Transactional
    public void resendVerificationEmailForUser(Long tenantId, Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + userId));
        if (user.getTenant() == null || !tenantId.equals(user.getTenant().getId())) {
            throw new RuntimeException("Cet utilisateur n'appartient pas à ce tenant.");
        }
        authService.resendVerificationEmail(user.getEmail());
    }

    // ==================== NOTES INTERNES ====================

    public List<NoteInterne> getNotesByTenant(Long tenantId) {
        return noteInterneRepository.findByTenantIdOrderByDateCreationDesc(tenantId);
    }

    @Transactional
    public NoteInterne ajouterNote(Long tenantId, String contenu, String auteur) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + tenantId));
        NoteInterne note = NoteInterne.builder()
                .tenant(tenant)
                .contenu(contenu)
                .auteur(auteur)
                .dateCreation(LocalDateTime.now())
                .build();
        return noteInterneRepository.save(note);
    }

    @Transactional
    public void supprimerNote(Long tenantId, Long noteId) {
        NoteInterne note = noteInterneRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note non trouvée : " + noteId));
        if (!note.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Note n'appartient pas au tenant " + tenantId);
        }
        noteInterneRepository.delete(note);
    }

    @Transactional
    public TenantAdminDto mettreAJourSourceAcquisition(Long tenantId, String sourceAcquisition) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + tenantId));
        tenant.setSourceAcquisition(sourceAcquisition);
        tenantRepository.save(tenant);
        return toDto(tenant);
    }

    // ==================== AUDIT LOGS ====================

    public List<AuditLogDto> getAuditLogs(Long tenantId) {
        return auditLogRepository.findByTenantIdOrderByDateActionDesc(tenantId)
                .stream()
                .map(AuditLogDto::fromEntity)
                .toList();
    }

    // ==================== PAIEMENTS MANUELS ====================

    /**
     * Valider un paiement manuel (WhatsApp / Wave / Cash / Orange Money).
     * - Change le plan du tenant
     * - Réinitialise dateExpiration à maintenant + 30 jours
     * - Enregistre le paiement dans paiements_super_admin
     */
    @Transactional
    public PaiementSuperAdminDto validerPaiement(Long tenantId, ValiderPaiementRequest req, String auteurEmail) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + tenantId));

        TenantEntity.Plan plan = TenantEntity.Plan.valueOf(req.plan());
        TenantEntity.Plan ancienPlan = tenant.getPlan();

        // Période : MENSUEL (défaut) → +30j, ANNUEL → +365j
        String periode = (req.periode() != null && req.periode().equalsIgnoreCase("ANNUEL"))
                ? "ANNUEL" : "MENSUEL";
        int joursAjoutes = "ANNUEL".equals(periode) ? 365 : 30;

        // Mettre à jour le tenant
        LocalDateTime now = LocalDateTime.now();
        tenant.setPlan(plan);
        tenant.setDateDebutEssai(now); // Sert aussi de "date de début d'abonnement" (affichée à l'admin tenant)
        tenant.setDateExpiration(now.plusDays(joursAjoutes));
        tenant.setActif(true);
        tenant.setEssaiUtilise(true);
        tenantRepository.save(tenant);
        tenantCacheService.evict(tenant.getTenantUuid());

        // Retrouver l'ID du super admin à partir de son email
        Long superAdminId = userRepository.findByEmailAndDeletedFalse(auteurEmail)
                .map(UserEntity::getId)
                .orElse(null);

        // Enregistrer le paiement
        PaiementSuperAdminEntity paiement = PaiementSuperAdminEntity.builder()
                .tenant(tenant)
                .plan(plan)
                .montant(req.montant())
                .datePaiement(LocalDateTime.now())
                .moisDebut(req.moisDebut())
                .modePaiement(req.modePaiement())
                .periode(periode)
                .note(req.note())
                .validePar(superAdminId)
                .build();
        paiementSuperAdminRepository.save(paiement);

        log.info("[SUPER_ADMIN] {} valide paiement pour tenant {} : {} → {} ({}F, {}, {})",
                auteurEmail, tenant.getTenantUuid(), ancienPlan, plan, req.montant(), req.modePaiement(), periode);
        saveLog("VALIDER_PAIEMENT",
                ancienPlan + " → " + plan + " | " + req.montant() + " F | " + req.modePaiement() + " | " + periode,
                tenant);

        // ─── Notifications push contextuelles ────────────────────────────────
        envoyerNotificationsPaiement(tenant, ancienPlan, plan, req.montant(), periode);

        return PaiementSuperAdminDto.fromEntity(paiement);
    }

    /**
     * Détermine quelles notifications envoyer en fonction du changement de plan.
     * Plusieurs notifications peuvent être envoyées pour un même paiement.
     */
    private void envoyerNotificationsPaiement(TenantEntity tenant,
                                              TenantEntity.Plan ancienPlan,
                                              TenantEntity.Plan nouveauPlan,
                                              java.math.BigDecimal montant,
                                              String periode) {
        String urlDetail = "/superadmin/tenants/" + tenant.getId();
        String nom = tenant.getNomEntreprise();
        String montantFormate = montant != null ? montant.toPlainString() + " CFA" : "";

        // 1. Paiement reçu — toujours envoyé
        pushService.notify(
                com.example.dijasaliou.entity.NotificationType.PAIEMENT_RECU,
                "💰 Paiement reçu",
                nom + " — " + nouveauPlan + " " + periode.toLowerCase() + " (" + montantFormate + ")",
                urlDetail
        );

        // 2. Paiement annuel — gros revenu
        if ("ANNUEL".equals(periode)) {
            pushService.notify(
                    com.example.dijasaliou.entity.NotificationType.PAIEMENT_ANNUEL,
                    "💎 Paiement annuel reçu",
                    nom + " — Plan " + nouveauPlan + " pour 1 an (" + montantFormate + ")",
                    urlDetail
            );
        }

        // 3. Conversion essai → payant
        boolean estPayant = nouveauPlan != TenantEntity.Plan.GRATUIT;
        boolean etaitEnEssaiOuGratuit = ancienPlan == TenantEntity.Plan.GRATUIT
                || ancienPlan == TenantEntity.Plan.BUSINESS && tenant.essaiGratuitValide();
        if (estPayant && etaitEnEssaiOuGratuit && ancienPlan != nouveauPlan) {
            pushService.notify(
                    com.example.dijasaliou.entity.NotificationType.CONVERSION_ESSAI,
                    "⭐ Conversion essai → payant",
                    nom + " est passé au plan " + nouveauPlan + " (" + montantFormate + ")",
                    urlDetail
            );
        }

        // 4. Upgrade de plan (STARTER < PRO < BUSINESS)
        if (estUpgrade(ancienPlan, nouveauPlan)) {
            pushService.notify(
                    com.example.dijasaliou.entity.NotificationType.UPGRADE_PLAN,
                    "⬆️ Upgrade de plan",
                    nom + " : " + ancienPlan + " → " + nouveauPlan,
                    urlDetail
            );
        }
    }

    private boolean estUpgrade(TenantEntity.Plan ancien, TenantEntity.Plan nouveau) {
        int rangAncien = rangPlan(ancien);
        int rangNouveau = rangPlan(nouveau);
        return rangNouveau > rangAncien && rangAncien >= 0;
    }

    private int rangPlan(TenantEntity.Plan p) {
        if (p == null) return -1;
        return switch (p) {
            case GRATUIT -> 0;
            case STARTER -> 1;
            case PRO -> 2;
            case BUSINESS -> 3;
        };
    }

    /**
     * Retourne les paiements manuels d'un tenant (historique).
     */
    @Transactional(readOnly = true)
    public List<PaiementSuperAdminDto> getPaiementsByTenant(Long tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + tenantId));
        return paiementSuperAdminRepository.findByTenantOrderByDatePaiementDesc(tenant)
                .stream()
                .map(PaiementSuperAdminDto::fromEntity)
                .toList();
    }

    /**
     * Modifier les champs d'un paiement existant (correction d'erreur).
     * Ne change PAS le plan courant du tenant — utiliser validerPaiement() pour ça.
     */
    @Transactional
    public PaiementSuperAdminDto modifierPaiement(Long paiementId, ModifierPaiementRequest req) {
        PaiementSuperAdminEntity paiement = paiementSuperAdminRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé : " + paiementId));

        if (req.plan() != null) paiement.setPlan(TenantEntity.Plan.valueOf(req.plan()));
        if (req.montant() != null) paiement.setMontant(req.montant());
        if (req.modePaiement() != null) paiement.setModePaiement(req.modePaiement());
        if (req.note() != null) paiement.setNote(req.note());

        paiementSuperAdminRepository.save(paiement);
        log.info("[SUPER_ADMIN] Paiement {} modifié", paiementId);
        saveLog("MODIFIER_PAIEMENT", "Paiement #" + paiementId + " corrigé", paiement.getTenant());
        return PaiementSuperAdminDto.fromEntity(paiement);
    }

    /**
     * Supprimer un paiement.
     * Si c'était le dernier paiement du tenant → rétrograder le tenant vers GRATUIT.
     */
    @Transactional
    public void supprimerPaiement(Long paiementId) {
        PaiementSuperAdminEntity paiement = paiementSuperAdminRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé : " + paiementId));

        TenantEntity tenant = paiement.getTenant();
        paiementSuperAdminRepository.delete(paiement);

        long restants = paiementSuperAdminRepository.countByTenant(tenant);
        if (restants == 0) {
            tenant.setPlan(TenantEntity.Plan.GRATUIT);
            tenant.setDateExpiration(null);
            tenantRepository.save(tenant);
            tenantCacheService.evict(tenant.getTenantUuid());
            log.info("[SUPER_ADMIN] Dernier paiement supprimé pour tenant {} → rétrogradé GRATUIT",
                    tenant.getTenantUuid());
            saveLog("SUPPRIMER_PAIEMENT",
                    "Paiement #" + paiementId + " supprimé — aucun paiement restant → GRATUIT", tenant);
        } else {
            log.info("[SUPER_ADMIN] Paiement {} supprimé, {} paiement(s) restant(s) pour tenant {}",
                    paiementId, restants, tenant.getTenantUuid());
            saveLog("SUPPRIMER_PAIEMENT",
                    "Paiement #" + paiementId + " supprimé — " + restants + " paiement(s) actifs conservés", tenant);
        }
    }

    // ==================== ACTIVITÉ RÉELLE & FANTÔMES ====================

    /**
     * Stats d'activité d'usage (vs stats commerciales du dashboard).
     * Compte les utilisateurs UNIQUES qui se sont connectés récemment + les
     * boutiques actives + les comptes "fantômes" (créés > 5 mois, jamais connectés).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getActivityStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime debutJour = now.toLocalDate().atStartOfDay();
        LocalDateTime debutSemaine = now.minusDays(7);
        LocalDateTime debutMois = now.minusDays(30);
        LocalDateTime seuilFantome = now.minusMonths(5);

        List<UserEntity> tousUsers = userRepository.findByDeletedFalse();

        long usersAujourdhui = tousUsers.stream()
                .filter(u -> u.getDerniereConnexion() != null && u.getDerniereConnexion().isAfter(debutJour))
                .count();
        long usersSemaine = tousUsers.stream()
                .filter(u -> u.getDerniereConnexion() != null && u.getDerniereConnexion().isAfter(debutSemaine))
                .count();
        long usersMois = tousUsers.stream()
                .filter(u -> u.getDerniereConnexion() != null && u.getDerniereConnexion().isAfter(debutMois))
                .count();

        long boutiquesActivesAujourdhui = tenantRepository.findActiveSince(debutJour).size();
        long boutiquesActivesSemaine = tenantRepository.findActiveSince(debutSemaine).size();
        long boutiquesActivesMois = tenantRepository.findActiveSince(debutMois).size();
        long boutiquesInactives30j = tenantRepository.findInactiveSince(now.minusDays(30)).size();
        long fantomes = tenantRepository.findFantomes(seuilFantome).size();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("usersUniquesAujourdhui", usersAujourdhui);
        stats.put("usersUniquesSemaine", usersSemaine);
        stats.put("usersUniquesMois", usersMois);
        stats.put("boutiquesActivesAujourdhui", boutiquesActivesAujourdhui);
        stats.put("boutiquesActivesSemaine", boutiquesActivesSemaine);
        stats.put("boutiquesActivesMois", boutiquesActivesMois);
        stats.put("boutiquesInactives30j", boutiquesInactives30j);
        stats.put("comptesFantomes", fantomes);
        stats.put("seuilFantomeMois", 5);
        return stats;
    }

    /**
     * Liste des comptes fantômes : créés il y a > 5 mois, aucun user jamais connecté.
     * Candidats au nettoyage.
     */
    @Transactional(readOnly = true)
    public List<TenantAdminDto> getFantomes() {
        LocalDateTime seuil = LocalDateTime.now().minusMonths(5);
        return tenantRepository.findFantomes(seuil).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Suppression en masse de tenants (soft-delete via supprimerTenant pour
     * chaque ID). Renvoie le nombre de tenants effectivement supprimés.
     */
    @Transactional
    public int supprimerTenantsBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        int count = 0;
        for (Long id : ids) {
            try {
                supprimerTenant(id);
                count++;
            } catch (Exception e) {
                log.warn("[SUPER_ADMIN] Suppression batch : échec pour tenant {} : {}", id, e.getMessage());
            }
        }
        log.info("[SUPER_ADMIN] Suppression batch : {}/{} tenants supprimés", count, ids.size());
        return count;
    }

    /**
     * Revenus mensuels détaillés — dashboard super admin.
     * Retourne pour chaque mois : total, nbPaiements et la liste complète des paiements.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRevenusMenuels() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        // Grouper tous les paiements par mois (ordre DESC déjà garanti par la requête)
        Map<String, List<PaiementSuperAdminEntity>> parMois = paiementSuperAdminRepository
                .findAllByOrderByDatePaiementDesc()
                .stream()
                .collect(Collectors.groupingBy(p -> p.getDatePaiement().format(fmt),
                        LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>(parMois.size());
        for (Map.Entry<String, List<PaiementSuperAdminEntity>> entry : parMois.entrySet()) {
            List<PaiementSuperAdminEntity> paiements = entry.getValue();
            BigDecimal total = paiements.stream()
                    .map(PaiementSuperAdminEntity::getMontant)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("mois", entry.getKey());
            item.put("total", total);
            item.put("nbPaiements", paiements.size());
            item.put("paiements", paiements.stream().map(PaiementSuperAdminDto::fromEntity).toList());
            result.add(item);
        }
        return result;
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private void saveLog(String action, String details, TenantEntity tenant) {
        String auteur = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .details(details)
                .auteur(auteur)
                .tenant(tenant)
                .dateAction(LocalDateTime.now())
                .build());
    }

    private TenantAdminDto toDto(TenantEntity tenant) {
        long nbUsers = userRepository.countByTenantAndDeletedFalse(tenant);
        long nbVentes = venteRepository.countByTenantId(tenant.getId());
        UserEntity admin = userRepository.findFirstByTenantAndRole(tenant, UserEntity.Role.ADMIN).orElse(null);
        LocalDateTime derniereActivite = userRepository.findDerniereActiviteByTenantId(tenant.getId()).orElse(null);
        return TenantAdminDto.fromEntity(tenant, nbUsers, nbVentes, admin, derniereActivite);
    }
}
