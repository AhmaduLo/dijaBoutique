package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.UpdateTenantRequest;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.example.dijasaliou.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Service utilitaire pour la gestion des tenants
 *
 * Fournit des méthodes pour récupérer le tenant actuel
 * et assigner le tenant aux entités
 */
@Service
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public TenantService(TenantRepository tenantRepository, UserRepository userRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    /**
     * Récupère le tenant actuel depuis le contexte
     *
     * @return TenantEntity du tenant actuel
     * @throws IllegalStateException si aucun tenant n'est défini
     */
    public TenantEntity getCurrentTenant() {
        String tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.error("SÉCURITÉ CRITIQUE : Aucun tenant défini dans le contexte");
            throw new IllegalStateException(
                "Impossible d'accéder aux données sans tenant. " +
                "Assurez-vous que l'utilisateur est authentifié."
            );
        }

        return tenantRepository.findByTenantUuid(tenantId)
                .orElseThrow(() -> new IllegalStateException(
                    "Tenant introuvable pour l'UUID: " + tenantId
                ));
    }

    /**
     * Retourne l'heure courante dans le fuseau horaire du tenant.
     *
     * À utiliser à la place de {@link LocalDateTime#now()} pour toutes les
     * dates métier (activation caisse, transferts, mouvements, etc.) afin que
     * l'heure stockée corresponde à celle vue par l'utilisateur côté frontend,
     * indépendamment du fuseau horaire du serveur Railway/AWS.
     */
    public LocalDateTime nowInTenantTz() {
        TenantEntity tenant = getCurrentTenant();
        String tz = tenant.getTimezone();
        if (tz == null || tz.isBlank()) tz = "Africa/Dakar";
        try {
            return LocalDateTime.now(ZoneId.of(tz));
        } catch (Exception e) {
            log.warn("Fuseau invalide '{}' sur tenant {}, fallback Africa/Dakar",
                    tz, tenant.getTenantUuid());
            return LocalDateTime.now(ZoneId.of("Africa/Dakar"));
        }
    }

    /** Variante {@link LocalDate} pour les champs jour-seul (datePaiement). */
    public LocalDate todayInTenantTz() {
        return nowInTenantTz().toLocalDate();
    }

    /**
     * Met à jour le fuseau horaire du tenant courant (paramétrage utilisateur).
     * Vérifie que le fuseau est un identifiant IANA valide.
     */
    @Transactional
    public TenantEntity updateTimezone(String newTimezone) {
        if (newTimezone == null || newTimezone.isBlank()) {
            throw new IllegalArgumentException("Le fuseau horaire est obligatoire");
        }
        try {
            ZoneId.of(newTimezone);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Fuseau horaire invalide : " + newTimezone + " (ex: Africa/Dakar)");
        }
        TenantEntity tenant = getCurrentTenant();
        tenant.setTimezone(newTimezone);
        TenantEntity saved = tenantRepository.save(tenant);
        log.info("Fuseau horaire mis à jour pour tenant {} : {}",
                tenant.getTenantUuid(), newTimezone);
        return saved;
    }

    /**
     * Vérifie si un tenant est défini dans le contexte
     *
     * @return true si un tenant est défini
     */
    public boolean isTenantDefined() {
        return TenantContext.isCurrentTenantSet();
    }

    /**
     * Récupère un tenant par son UUID
     *
     * @param tenantUuid UUID du tenant
     * @return TenantEntity
     */
    public TenantEntity getTenantByUuid(String tenantUuid) {
        return tenantRepository.findByTenantUuid(tenantUuid)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Tenant introuvable pour l'UUID: " + tenantUuid
                ));
    }

    /**
     * Met à jour les informations de l'entreprise (tenant)
     * Seul un ADMIN peut modifier les informations de son entreprise
     *
     * @param request Contient nomEntreprise et numeroTelephone
     * @return TenantEntity mise à jour
     */
    @Transactional
    public TenantEntity updateTenant(UpdateTenantRequest request) {
        // Récupérer le tenant actuel
        TenantEntity tenantActuel = getCurrentTenant();

        // Recharger depuis la base de données avec son ID pour éviter les problèmes de session
        TenantEntity tenant = tenantRepository.findById(tenantActuel.getId())
                .orElseThrow(() -> new IllegalStateException("Tenant introuvable"));

        boolean nomEntrepriseChange = false;
        String nouveauNomEntreprise = null;

        // Mettre à jour les champs
        if (request.getNomEntreprise() != null && !request.getNomEntreprise().trim().isEmpty()) {
            nouveauNomEntreprise = request.getNomEntreprise().trim();
            if (!nouveauNomEntreprise.equals(tenant.getNomEntreprise())) {
                nomEntrepriseChange = true;
                tenant.setNomEntreprise(nouveauNomEntreprise);
            }
        }

        if (request.getNumeroTelephone() != null && !request.getNumeroTelephone().trim().isEmpty()) {
            tenant.setNumeroTelephone(request.getNumeroTelephone().trim());
        }

        if (request.getAdresse() != null) {
            tenant.setAdresse(request.getAdresse().trim().isEmpty() ? null : request.getAdresse().trim());
        }

        if (request.getVille() != null) {
            tenant.setVille(request.getVille().trim().isEmpty() ? null : request.getVille().trim());
        }

        if (request.getPays() != null) {
            tenant.setPays(request.getPays().trim().isEmpty() ? null : request.getPays().trim());
        }

        // Mise à jour du NINEA/SIRET (optionnel - peut être null ou vide)
        if (request.getNineaSiret() != null) {
            String nineaSiret = request.getNineaSiret().trim().isEmpty() ? null : request.getNineaSiret().trim();
            tenant.setNineaSiret(nineaSiret);
        }

        // Mise à jour de l'URL du logo (optionnel)
        if (request.getLogoUrl() != null) {
            tenant.setLogoUrl(request.getLogoUrl().trim().isEmpty() ? null : request.getLogoUrl().trim());
        }

        // Forcer la sauvegarde du tenant
        TenantEntity tenantSauvegarde = tenantRepository.saveAndFlush(tenant);
        log.info("Tenant mis à jour : {} (uuid={})", tenantSauvegarde.getNomEntreprise(), tenantSauvegarde.getTenantUuid());

        // Si le nom de l'entreprise a changé, mettre à jour tous les utilisateurs de ce tenant
        if (nomEntrepriseChange) {
            List<UserEntity> utilisateurs = tenant.getUtilisateurs();
            for (UserEntity user : utilisateurs) {
                user.setNomEntreprise(nouveauNomEntreprise);
            }
            userRepository.saveAllAndFlush(utilisateurs);
        }

        return tenantSauvegarde;
    }

    @Transactional
    public void clearLogoUrl(TenantEntity tenant) {
        TenantEntity t = tenantRepository.findById(tenant.getId())
                .orElseThrow(() -> new IllegalStateException("Tenant introuvable"));
        t.setLogoUrl(null);
        tenantRepository.save(t);
    }

    /**
     * Récupère l'administrateur (propriétaire) qui a créé le tenant
     *
     * @param tenant Le tenant dont on veut récupérer l'admin
     * @return UserEntity de l'admin ou null si non trouvé
     */
    public UserEntity getAdminProprietaire(TenantEntity tenant) {
        return tenant.getUtilisateurs().stream()
                .filter(user -> user.getRole() == UserEntity.Role.ADMIN)
                .findFirst()
                .orElse(null);
    }
}
