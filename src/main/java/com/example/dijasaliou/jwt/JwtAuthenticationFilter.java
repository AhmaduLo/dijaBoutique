package com.example.dijasaliou.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * 3. Extraire l'email du token
 * 4. Charger l'utilisateur
 * 5. Authentifier l'utilisateur dans le contexte Spring Security
 */
@Component
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
                    // 8. Créer l'objet d'authentification
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 9. Authentifier l'utilisateur dans le contexte Spring Security
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token invalide → Ne rien faire (l'utilisateur restera non authentifié)
            System.out.println("Token invalide : " + e.getMessage());
        }

        // 10. Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
}
