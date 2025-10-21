package com.example.dijasaliou.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Désactiver CSRF (pas nécessaire avec JWT)
                .csrf(csrf -> csrf.disable())

                // Pas de session (stateless avec JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                )

                // Règles d'autorisation
                .authorizeHttpRequests(auth -> auth
                        // Routes publiques (pas besoin de token)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Toutes les autres routes nécessitent un token
                        .anyRequest().authenticated()
                );

        return http.build();
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
