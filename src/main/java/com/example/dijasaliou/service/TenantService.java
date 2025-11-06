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
                log.info("Mise à jour nom entreprise: {} -> {}", tenantActuel.getNomEntreprise(), nouveauNomEntreprise);
            }
        }

        if (request.getNumeroTelephone() != null && !request.getNumeroTelephone().trim().isEmpty()) {
            tenant.setNumeroTelephone(request.getNumeroTelephone().trim());
            log.info("Mise à jour numéro téléphone: {} -> {}", tenantActuel.getNumeroTelephone(), request.getNumeroTelephone());
        }

        if (request.getAdresse() != null) {
            tenant.setAdresse(request.getAdresse().trim().isEmpty() ? null : request.getAdresse().trim());
            log.info("Mise à jour adresse: {} -> {}", tenantActuel.getAdresse(), request.getAdresse());
        }

        // Forcer la sauvegarde du tenant
        TenantEntity tenantSauvegarde = tenantRepository.saveAndFlush(tenant);
        log.info("Tenant mis à jour avec succès : {} - {} - {} - {}",
                tenantSauvegarde.getNomEntreprise(),
                tenantSauvegarde.getNumeroTelephone(),
                tenantSauvegarde.getAdresse(),
                tenantSauvegarde.getTenantUuid());

        // Si le nom de l'entreprise a changé, mettre à jour tous les utilisateurs de ce tenant
        if (nomEntrepriseChange) {
            List<UserEntity> utilisateurs = tenant.getUtilisateurs();
            log.info("Mise à jour du nom d'entreprise pour {} utilisateurs", utilisateurs.size());

            for (UserEntity user : utilisateurs) {
                user.setNomEntreprise(nouveauNomEntreprise);
            }

            userRepository.saveAllAndFlush(utilisateurs);
            log.info("Tous les utilisateurs ont été mis à jour avec le nouveau nom d'entreprise: {}", nouveauNomEntreprise);
        }

        return tenantSauvegarde;
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
