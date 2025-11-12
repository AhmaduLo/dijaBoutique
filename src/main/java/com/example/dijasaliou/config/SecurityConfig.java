package com.example.dijasaliou.config;

import com.example.dijasaliou.filter.SubscriptionExpirationFilter;
import com.example.dijasaliou.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

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

                        // Routes de paiement - TOUTES publiques pour éviter les problèmes de filtre
                        .requestMatchers("/payment/**").permitAll()

                        // Routes ADMIN uniquement (création de compte, gestion utilisateurs)
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/users/**").hasAuthority("ADMIN")

                        // Routes spéciales accessibles à TOUS les utilisateurs authentifiés
                        .requestMatchers("/achats/produits-pour-vente").authenticated()
                        .requestMatchers("/tenant/info").authenticated() // Info entreprise pour factures
                        .requestMatchers("/contact").authenticated() // Formulaire de contact

                        // Routes accessibles aux GERANT et ADMIN : achats, dépenses, tableaux de bord
                        .requestMatchers("/achats/**").hasAnyAuthority("GERANT", "ADMIN")
                        .requestMatchers("/depenses/**").hasAnyAuthority("GERANT", "ADMIN")
                        .requestMatchers("/tenant/**").hasAnyAuthority("GERANT", "ADMIN")

                        // Routes accessibles aux USER, GERANT et ADMIN : ventes et stock
                        .requestMatchers("/ventes/**").hasAnyAuthority("USER", "GERANT", "ADMIN")
                        .requestMatchers("/stock/**").hasAnyAuthority("USER", "GERANT", "ADMIN")

                        // Routes devises : lecture accessible à tous, modification ADMIN uniquement
                        .requestMatchers("/devises/**").authenticated()

                        // Toutes les autres routes nécessitent un token (par sécurité)
                        .anyRequest().authenticated()
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
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

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
}
