package com.example.dijasaliou.config;

import com.example.dijasaliou.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuration de sécurité
 *
 * TEMPORAIRE : Désactive complètement la sécurité pour les tests
 *
 * À ACTIVER PLUS TARD avec JWT !
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(@Lazy JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
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

                        // Routes accessibles aux USER, GERANT et ADMIN : ventes et stock
                        .requestMatchers("/ventes/**").hasAnyAuthority("USER", "GERANT", "ADMIN")
                        .requestMatchers("/stock/**").hasAnyAuthority("USER", "GERANT", "ADMIN")

                        // Routes devises : lecture accessible à tous, modification ADMIN uniquement
                        .requestMatchers("/devises/**").authenticated()

                        // Toutes les autres routes nécessitent un token (par sécurité)
                        .anyRequest().authenticated()
                )
        // Ajouter le filtre JWT AVANT le filtre d'authentification standard
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuration CORS pour autoriser Angular
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Autoriser plusieurs ports pour le développement
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "http://localhost:64390"  // Port utilisé par votre frontend
        ));
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
