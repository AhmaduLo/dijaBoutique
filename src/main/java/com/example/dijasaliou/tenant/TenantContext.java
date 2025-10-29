package com.example.dijasaliou.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * TenantContext - Stockage du tenant actuel dans le contexte de la requête
 *
 * PRINCIPE :
 * - Utilise ThreadLocal pour stocker le tenant_id de l'utilisateur connecté
 * - Chaque requête HTTP a son propre Thread → Isolation automatique
 * - Le tenant_id est extrait du JWT et stocké au début de chaque requête
 *
 * SÉCURITÉ :
 * - ThreadLocal garantit qu'un utilisateur ne peut PAS accéder au tenant d'un autre
 * - Le contexte est nettoyé après chaque requête (important !)
 *
 * CYCLE DE VIE :
 * 1. JWT reçu → TenantInterceptor extrait tenant_id
 * 2. tenant_id stocké dans TenantContext
 * 3. JPA Filter utilise TenantContext pour filtrer les requêtes
 * 4. Après la requête → clear() pour éviter les fuites de mémoire
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    /**
     * Définit le tenant actuel pour le thread courant
     *
     * @param tenantId UUID du tenant
     */
    public static void setCurrentTenant(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.warn("Tentative de définir un tenant null ou vide - SÉCURITÉ COMPROMISE");
            throw new IllegalArgumentException("Tenant ID ne peut pas être null ou vide");
        }
        log.debug("Définition du tenant actuel: {}", tenantId);
        currentTenant.set(tenantId);
    }

    /**
     * Récupère le tenant actuel du thread courant
     *
     * @return UUID du tenant ou null si non défini
     */
    public static String getCurrentTenant() {
        String tenantId = currentTenant.get();

        if (tenantId == null) {
            log.warn("ATTENTION : Tentative d'accès aux données sans tenant défini - SÉCURITÉ COMPROMISE");
            // En production, vous pourriez vouloir lancer une exception
            // throw new IllegalStateException("Aucun tenant défini dans le contexte");
        }

        return tenantId;
    }

    /**
     * Vérifie si un tenant est défini
     *
     * @return true si un tenant est défini
     */
    public static boolean isCurrentTenantSet() {
        return currentTenant.get() != null;
    }

    /**
     * Nettoie le contexte tenant
     *
     * CRITIQUE : Doit être appelé après CHAQUE requête
     * Sinon → Fuite mémoire + Risque sécurité si le thread est réutilisé
     */
    public static void clear() {
        String tenantId = currentTenant.get();
        if (tenantId != null) {
            log.debug("Nettoyage du contexte tenant: {}", tenantId);
        }
        currentTenant.remove();
    }

    /**
     * Méthode utilitaire pour debug
     */
    public static String getDebugInfo() {
        String tenantId = currentTenant.get();
        return String.format("TenantContext[thread=%s, tenant=%s]",
            Thread.currentThread().getName(),
            tenantId != null ? tenantId : "NON_DEFINI");
    }
}
