package com.example.dijasaliou.config;

import com.example.dijasaliou.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepteur pour activer automatiquement le filtre Hibernate tenant
 * sur TOUTES les requêtes HTTP
 */
@Component
public class HibernateFilterInterceptor implements HandlerInterceptor {

    private final EntityManager entityManager;

    public HibernateFilterInterceptor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Récupérer le tenant_id du contexte
        String tenantId = TenantContext.getCurrentTenant();

        if (tenantId != null && !tenantId.isEmpty()) {
            // Activer le filtre Hibernate avec le tenant_id
            Session session = entityManager.unwrap(Session.class);

            // Activer le filtre "tenantFilter" défini dans les entités
            session.enableFilter("tenantFilter")
                   .setParameter("tenantId", tenantId);
        }

        return true;
    }
}
