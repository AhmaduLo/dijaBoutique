package com.example.dijasaliou.jwt;

import com.example.dijasaliou.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT qui intercepte CHAQUE requête
 *
 * Responsabilités :
 * 1. Extraire le token du header Authorization
 * 2. Valider le token
 * 3. Extraire l'email ET le tenant_id du token
 * 4. Charger l'utilisateur
 * 5. Authentifier l'utilisateur dans le contexte Spring Security
 * 6. MULTI-TENANT : Stocker le tenant_id dans TenantContext
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Extraire le header Authorization
        String authHeader = request.getHeader("Authorization");

        // 2. Vérifier si le header existe et commence par "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Pas de token → Continuer sans authentifier
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraire le token (enlever "Bearer ")
        String token = authHeader.substring(7);

        try {
            // 4. Extraire l'email du token
            String email = jwtService.getEmailFromToken(token);

            // 5. Si l'email existe ET l'utilisateur n'est pas déjà authentifié
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 6. Charger l'utilisateur depuis la base
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 7. Valider le token
                if (jwtService.validateToken(token)) {
                    // 8. MULTI-TENANT : Extraire et stocker le tenant_id
                    String tenantId = jwtService.getTenantIdFromToken(token);
                    if (tenantId != null && !tenantId.trim().isEmpty()) {
                        TenantContext.setCurrentTenant(tenantId);
                        log.debug("Tenant défini dans le contexte: {}", tenantId);
                    } else {
                        log.warn("SÉCURITÉ : Token sans tenant_id pour l'utilisateur: {}", email);
                    }

                    // 9. Créer l'objet d'authentification
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 10. Authentifier l'utilisateur dans le contexte Spring Security
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token invalide → Ne rien faire (l'utilisateur restera non authentifié)
            log.error("Token invalide : {}", e.getMessage());
            TenantContext.clear(); // Nettoyer le contexte en cas d'erreur
        }

        try {
            // 11. Continuer la chaîne de filtres
            filterChain.doFilter(request, response);
        } finally {
            // 12. CRITIQUE : Nettoyer le contexte tenant après chaque requête
            TenantContext.clear();
            log.debug("Contexte tenant nettoyé après la requête");
        }
    }
}
