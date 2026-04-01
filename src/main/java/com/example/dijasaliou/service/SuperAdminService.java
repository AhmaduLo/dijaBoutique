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
import lombok.extern.slf4j.Slf4j;
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

    public SuperAdminService(TenantRepository tenantRepository,
                             UserRepository userRepository,
                             VenteRepository venteRepository,
                             NoteInterneRepository noteInterneRepository,
                             AuditLogRepository auditLogRepository,
                             FactureService factureService,
                             FactureRepository factureRepository,
                             TenantCacheService tenantCacheService,
                             PaiementSuperAdminRepository paiementSuperAdminRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.venteRepository = venteRepository;
        this.noteInterneRepository = noteInterneRepository;
        this.auditLogRepository = auditLogRepository;
        this.factureService = factureService;
        this.factureRepository = factureRepository;
        this.tenantCacheService = tenantCacheService;
        this.paiementSuperAdminRepository = paiementSuperAdminRepository;
    }

    /**
     * Retourne tous les tenants avec leurs stats — paginé
     */
    public PagedResponse<TenantAdminDto> getAllTenants(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<TenantEntity> tenantsPage = tenantRepository.findByDeletedFalse(pageable);
        return PagedResponse.from(tenantsPage.map(this::toDto));
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

        // Mettre à jour le tenant
        tenant.setPlan(plan);
        tenant.setDateExpiration(LocalDateTime.now().plusDays(30));
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
                .note(req.note())
                .validePar(superAdminId)
                .build();
        paiementSuperAdminRepository.save(paiement);

        log.info("[SUPER_ADMIN] {} valide paiement pour tenant {} : {} → {} ({}F, {})",
                auteurEmail, tenant.getTenantUuid(), ancienPlan, plan, req.montant(), req.modePaiement());
        saveLog("VALIDER_PAIEMENT",
                ancienPlan + " → " + plan + " | " + req.montant() + " F | " + req.modePaiement(),
                tenant);

        return PaiementSuperAdminDto.fromEntity(paiement);
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
