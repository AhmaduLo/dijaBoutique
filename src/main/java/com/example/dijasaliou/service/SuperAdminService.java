package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.FactureDto;
import com.example.dijasaliou.dto.TenantAdminDto;
import com.example.dijasaliou.entity.FactureEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.FactureRepository;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
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
    private final FactureService factureService;
    private final FactureRepository factureRepository;

    public SuperAdminService(TenantRepository tenantRepository,
                             UserRepository userRepository,
                             FactureService factureService,
                             FactureRepository factureRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.factureService = factureService;
        this.factureRepository = factureRepository;
    }

    /**
     * Retourne tous les tenants avec leurs stats
     */
    public List<TenantAdminDto> getAllTenants() {
        List<TenantEntity> tenants = tenantRepository.findAll();
        return tenants.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
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
        log.info("[SUPER_ADMIN] Tenant {} activé", tenant.getTenantUuid());
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
        log.info("[SUPER_ADMIN] Tenant {} désactivé", tenant.getTenantUuid());
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

        LocalDateTime ancienPlanExpiration = tenant.getDateExpiration();
        tenant.setPlan(plan);
        tenant.setDateExpiration(LocalDateTime.now().plusDays(jours));
        tenant.setActif(true);

        if (!Boolean.TRUE.equals(tenant.getEssaiUtilise())) {
            tenant.setEssaiUtilise(true);
        }

        tenantRepository.save(tenant);
        log.info("[SUPER_ADMIN] Tenant {} : plan {} → {} (expire dans {} jours, était: {})",
                tenant.getTenantUuid(), ancienPlanExpiration, plan, jours, ancienPlanExpiration);

        // Créer une facture pour tracer ce changement de plan
        factureService.creerFacture(tenant, plan, jours, FactureEntity.StatutFacture.MANUELLE, null);

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
     * Supprimer un tenant et tous ses utilisateurs (soft delete)
     */
    @Transactional
    public void supprimerTenant(Long id) {
        TenantEntity tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + id));

        // Désactiver tous les utilisateurs du tenant
        List<UserEntity> users = userRepository.findByDeletedFalse().stream()
                .filter(u -> tenant.equals(u.getTenant()))
                .collect(Collectors.toList());

        for (UserEntity user : users) {
            user.setDeleted(true);
            user.setDateSuppression(LocalDateTime.now());
        }
        userRepository.saveAll(users);

        // Désactiver le tenant
        tenant.setActif(false);
        tenantRepository.save(tenant);

        log.info("[SUPER_ADMIN] Tenant {} supprimé ({} utilisateurs désactivés)",
                tenant.getTenantUuid(), users.size());
    }

    /**
     * Stats globales de la plateforme
     */
    public Map<String, Object> getGlobalStats() {
        List<TenantEntity> tenants = tenantRepository.findAll();

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

        return stats;
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private TenantAdminDto toDto(TenantEntity tenant) {
        long nbUsers = userRepository.countByTenantAndDeletedFalse(tenant);
        UserEntity admin = userRepository.findFirstByTenantAndRole(tenant, UserEntity.Role.ADMIN).orElse(null);
        return TenantAdminDto.fromEntity(tenant, nbUsers, admin);
    }
}
