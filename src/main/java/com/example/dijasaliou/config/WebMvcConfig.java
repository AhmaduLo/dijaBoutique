package com.example.dijasaliou.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration Spring MVC
 *
 * Enregistre l'intercepteur HibernateFilterInterceptor pour activer
 * automatiquement le filtre tenant sur toutes les requêtes
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final HibernateFilterInterceptor hibernateFilterInterceptor;

    public WebMvcConfig(HibernateFilterInterceptor hibernateFilterInterceptor) {
        this.hibernateFilterInterceptor = hibernateFilterInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Enregistrer l'intercepteur pour TOUTES les requêtes
        // sauf les endpoints publics (auth, etc.)
        registry.addInterceptor(hibernateFilterInterceptor)
                .addPathPatterns("/api/**")  // Toutes les routes /api/*
                .excludePathPatterns(
                        "/api/auth/register",    // Exclure l'inscription
                        "/api/auth/login"        // Exclure la connexion
                );
    }
}
