package com.example.dijasaliou.config;

import com.example.dijasaliou.filter.SubscriptionExpirationFilter;
import com.example.dijasaliou.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * Configuration de sécurité
 *
 * Sécurise l'application avec JWT et vérifie l'expiration des abonnements
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final SubscriptionExpirationFilter subscriptionExpirationFilter;

    public SecurityConfig(@Lazy JwtAuthenticationFilter jwtAuthFilter,
                          SubscriptionExpirationFilter subscriptionExpirationFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.subscriptionExpirationFilter = subscriptionExpirationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Désactiver CSRF (pas nécessaire avec JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Désactiver CORS pour autoriser Angular
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Pas de session (stateless avec JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Règles d'autorisation
                .authorizeHttpRequests(auth -> auth
                        // Routes publiques (login + register + logout + réinitialisation mot de passe)
                        .requestMatchers("/auth/login", "/auth/register", "/auth/logout",
                                "/auth/forgot-password", "/auth/reset-password").permitAll()

                        // Route de suppression de compte - ADMIN uniquement
                        .requestMatchers("/auth/delete-account").hasAuthority("ADMIN")

                        // Routes Super Admin - SUPER_ADMIN uniquement
                        .requestMatchers("/superadmin/**").hasAuthority("SUPER_ADMIN")

                        // Routes de paiement publiques (webhook Wave)
                        .requestMatchers("/payment/wave/webhook").permitAll() // Webhook Wave
                        // Autres routes de paiement publiques
                        .requestMatchers("/payment/plans").permitAll()
                        // Routes de paiement nécessitant l'authentification
                        .requestMatchers("/payment/**").authenticated()


                        // Routes de gestion des fichiers
                        .requestMatchers("/files/upload").hasAuthority("ADMIN") // Upload réservé aux ADMIN
                        .requestMatchers("/files/photos/**").authenticated() // Récupération des photos pour tous les utilisateurs authentifiés
                        .requestMatchers("/files/health").permitAll() // Health check public

                        // Routes ADMIN uniquement (création de compte, gestion utilisateurs)
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/users/**").hasAuthority("ADMIN")

                        // Routes spéciales accessibles à TOUS les utilisateurs authentifiés
                        .requestMatchers("/achats/produits-pour-vente").authenticated()
                        .requestMatchers("/tenant/info").authenticated() // Info entreprise pour factures
                        .requestMatchers("/contact").authenticated() // Formulaire de contact

                        // Routes accessibles aux GERANT et ADMIN : achats (lecture accessible à USER pour voir les produits)
                        .requestMatchers(HttpMethod.GET, "/achats/**").hasAnyAuthority("USER", "GERANT", "ADMIN") // USER peut lire les achats
                        .requestMatchers(HttpMethod.POST, "/achats/**").hasAnyAuthority("GERANT", "ADMIN") // Seuls GERANT/ADMIN peuvent créer
                        .requestMatchers(HttpMethod.PUT, "/achats/**").hasAnyAuthority("GERANT", "ADMIN") // Seuls GERANT/ADMIN peuvent modifier
                        .requestMatchers(HttpMethod.DELETE, "/achats/**").hasAnyAuthority("GERANT", "ADMIN") // Seuls GERANT/ADMIN peuvent supprimer
                        .requestMatchers("/depenses/**").hasAnyAuthority("GERANT", "ADMIN")
                        .requestMatchers("/tenant/**").hasAnyAuthority("GERANT", "ADMIN")

                        // Routes accessibles aux USER, GERANT et ADMIN : ventes, stock, bons de livraison
                        .requestMatchers("/ventes/**").hasAnyAuthority("USER", "GERANT", "ADMIN")
                        .requestMatchers("/stock/**").hasAnyAuthority("USER", "GERANT", "ADMIN")
                        .requestMatchers("/bons-de-livraison/**").hasAnyAuthority("USER", "GERANT", "ADMIN")

                        // Routes devises : lecture accessible à tous, modification ADMIN uniquement
                        .requestMatchers("/devises/**").authenticated()

                        // Toutes les autres routes nécessitent un token (par sécurité)
                        .anyRequest().authenticated()
                )
        // Retourner 401 (pas 403) pour les requêtes non authentifiées
        // Nécessaire pour que Angular redirige vers /login au lieu de bloquer
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"error\":\"Non authentifié\",\"message\":\"Veuillez vous connecter pour accéder à cette ressource.\"}");
                        })
                )

        // Ajouter les filtres dans l'ordre :
        // 1. JWT pour l'authentification (avant UsernamePasswordAuthenticationFilter)
        // 2. SubscriptionExpiration pour bloquer si l'abonnement est expiré (après UsernamePasswordAuthenticationFilter)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(subscriptionExpirationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuration CORS pour autoriser Angular
     *
     * SÉCURITÉ : Configuration stricte en production
     * - Autoriser uniquement les domaines de confiance
     * - Limiter les méthodes HTTP
     * - Spécifier les headers autorisés
     *
     * En production, remplacer localhost par votre domaine réel
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // DÉVELOPPEMENT : Autoriser localhost pour le développement local
        // PRODUCTION : Remplacer par votre domaine (ex: https://votredomaine.com)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "http://localhost:64390"
                // En production, ajouter votre domaine ici :
                // "https://votredomaine.com",
                // "https://www.votredomaine.com"
        ));

        // Méthodes HTTP autorisées (strictement nécessaires)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Headers autorisés (spécifiques au lieu de "*" pour plus de sécurité)
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Headers exposés au client
        configuration.setExposedHeaders(Arrays.asList(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        // Autoriser les cookies (JWT HttpOnly)
        configuration.setAllowCredentials(true);

        // Cache des preflight requests (OPTIONS) pendant 1 heure
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    /**
     * Bean pour hasher les mots de passe
     *
     * BCrypt :
     * - "password123" → "$2a$10$N9qo8uLOickgx2ZmmYEQmOeH..."
     * - Impossible de retrouver le mot de passe original
     * - Chaque hash est différent même pour le même mot de passe
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Empêche Spring Boot d'enregistrer JwtAuthenticationFilter comme filtre servlet autonome.
     * Sans ça, le filtre s'exécute 2 fois : une fois avant Spring Security (au niveau servlet)
     * et une fois dans la chaîne Spring Security. La 2ème exécution est skippée par OncePerRequestFilter,
     * mais SecurityContextHolderFilter a déjà effacé le SecurityContext entre-temps → 403 sur tout.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Même raison que ci-dessus pour SubscriptionExpirationFilter.
     */
    @Bean
    public FilterRegistrationBean<SubscriptionExpirationFilter> subscriptionFilterRegistration(SubscriptionExpirationFilter filter) {
        FilterRegistrationBean<SubscriptionExpirationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
