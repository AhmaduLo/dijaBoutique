package com.example.dijasaliou.config;

import com.example.dijasaliou.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Aspect AOP pour activer automatiquement le filtre tenant
 * sur TOUTES les méthodes des repositories
 */
@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Active le filtre tenant AVANT chaque appel aux repositories
     */
    @Before("execution(* com.example.dijasaliou.repository.*.*(..))")
    public void enableTenantFilter(JoinPoint joinPoint) {
        String tenantId = TenantContext.getCurrentTenant();

        if (tenantId != null && !tenantId.isEmpty()) {
            Session session = entityManager.unwrap(Session.class);

            // Désactiver le filtre s'il est déjà actif (pour éviter les erreurs)
            Filter filter = session.getEnabledFilter("tenantFilter");
            if (filter != null) {
                session.disableFilter("tenantFilter");
            }

            // Activer le filtre avec le nouveau tenant_id
            session.enableFilter("tenantFilter")
                   .setParameter("tenantId", tenantId);

            System.out.println("✅ [TenantFilterAspect] Filtre activé pour tenant: " + tenantId
                + " - Méthode: " + joinPoint.getSignature().getName());
        } else {
            System.out.println("⚠️ [TenantFilterAspect] Pas de tenant_id dans le contexte pour: "
                + joinPoint.getSignature().getName());
        }
    }
}
