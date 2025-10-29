package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
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
}
