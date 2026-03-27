package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.AuditLogDto;
import com.example.dijasaliou.dto.FactureDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.dto.TenantAdminDto;
import com.example.dijasaliou.dto.UtilisateurTenantDto;
import com.example.dijasaliou.entity.AuditLog;
import com.example.dijasaliou.entity.FactureEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.NoteInterne;
import com.example.dijasaliou.repository.AuditLogRepository;
import com.example.dijasaliou.repository.FactureRepository;
import com.example.dijasaliou.repository.NoteInterneRepository;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.repository.VenteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public SuperAdminService(TenantRepository tenantRepository,
                             UserRepository userRepository,
                             VenteRepository venteRepository,
                             NoteInterneRepository noteInterneRepository,
                             AuditLogRepository auditLogRepository,
                             FactureService factureService,
                             FactureRepository factureRepository,
                             TenantCacheService tenantCacheService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.venteRepository = venteRepository;
        this.noteInterneRepository = noteInterneRepository;
        this.auditLogRepository = auditLogRepository;
        this.factureService = factureService;
        this.factureRepository = factureRepository;
        this.tenantCacheService = tenantCacheService;
    }

    /**
     * Retourne tous les tenants avec leurs stats — paginé
     */
    public PagedResponse<TenantAdminDto> getAllTenants(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<TenantEntity> tenantsPage = tenantRepository.findAll(pageable);
        return PagedResponse.from(tenantsPage.map(this::toDto));
    }

    /**
     * Retourne tous les tenants sans pagination (pour les stats globales uniquement)
     */
    private List<TenantEntity> getAllTenantsForStats() {
        return tenantRepository.findAll();
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
        tenant.setPlan(plan);
        tenant.setDateExpiration(LocalDateTime.now().plusDays(jours));
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
     * Suppression définitive d'un tenant (IRRÉVERSIBLE depuis l'interface)
     * - tenant.deleted = true + dateSuppression
     * - tous les utilisateurs soft-deletés
     * Les données restent en base.
     */
    @Transactional
    public void supprimerTenant(Long id) {
        TenantEntity tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + id));

        // Soft-delete tous les utilisateurs du tenant
        List<UserEntity> users = userRepository.findByDeletedFalse().stream()
                .filter(u -> tenant.equals(u.getTenant()))
                .collect(Collectors.toList());

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
        long totalEssaiHistorique = tenantRepository.countByEssaiUtiliseTrue();
        long totalConvertis = tenantRepository.countByEssaiUtiliseTrueAndPlanNot(TenantEntity.Plan.GRATUIT);
        double tauxConversion = totalEssaiHistorique > 0 ? (totalConvertis * 100.0 / totalEssaiHistorique) : 0;
        stats.put("totalEssaiHistorique", totalEssaiHistorique);
        stats.put("totalConvertis", totalConvertis);
        stats.put("tauxConversion", Math.round(tauxConversion * 10.0) / 10.0);

        return stats;
    }

    public List<UtilisateurTenantDto> getUtilisateursByTenant(Long tenantId) {
        return userRepository.findByTenantIdAndDeletedFalse(tenantId)
                .stream()
                .map(UtilisateurTenantDto::fromEntity)
                .toList();
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
