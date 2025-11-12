package com.example.dijasaliou.filter;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtre pour bloquer l'accès si l'abonnement est expiré
 *
 * FONCTIONNEMENT :
 * - Si le tenant a une dateExpiration
 * - Et que cette date est dépassée
 * - Alors on bloque l'accès à toutes les routes sauf :
 *   - /auth/login (pour se connecter)
 *   - /payment/** (pour renouveler)
 *   - /tenant/info (pour voir les infos)
 *
 * IMPORTANT : Ce filtre s'exécute APRÈS l'authentification JWT
 */
@Component
@Slf4j
public class SubscriptionExpirationFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    public SubscriptionExpirationFilter(TenantRepository tenantRepository, ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Routes autorisées même si l'abonnement est expiré OU routes publiques
        if (isAllowedRoute(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Récupérer le tenant depuis le contexte (déjà défini par le filtre JWT)
        String tenantId = TenantContext.getCurrentTenant();

        // Si pas de tenant, laisser passer (utilisateur non authentifié - sera géré par Spring Security)
        if (tenantId == null || tenantId.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Vérifier l'expiration
        TenantEntity tenant = tenantRepository.findByTenantUuid(tenantId).orElse(null);

        if (tenant != null && isSubscriptionExpired(tenant)) {
            log.warn("Accès bloqué pour le tenant {} - Abonnement expiré le {}",
                    tenant.getTenantUuid(), tenant.getDateExpiration());

            // Bloquer l'accès
            sendExpiredResponse(response);
            return;
        }

        // Tout est OK, continuer
        filterChain.doFilter(request, response);
    }

    /**
     * Vérifie si l'abonnement est expiré
     */
    private boolean isSubscriptionExpired(TenantEntity tenant) {
        if (tenant.getDateExpiration() == null) {
            return false; // Pas de date d'expiration = pas de limite
        }

        return LocalDateTime.now().isAfter(tenant.getDateExpiration());
    }

    /**
     * Routes autorisées même si l'abonnement est expiré
     */
    private boolean isAllowedRoute(String requestURI) {
        return requestURI.startsWith("/api/auth/login")
                || requestURI.startsWith("/api/auth/register")
                || requestURI.startsWith("/api/auth/logout")
                || requestURI.startsWith("/api/payment/")
                || requestURI.startsWith("/api/tenant/info")
                || requestURI.startsWith("/api/admin/entreprise");
    }

    /**
     * Envoie une réponse 403 avec un message d'erreur
     */
    private void sendExpiredResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Paiement requis");
        errorResponse.put("message", "Veuillez souscrire à un abonnement pour accéder à l'application. Choisissez un plan et effectuez votre paiement.");
        errorResponse.put("code", "PAYMENT_REQUIRED");

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
