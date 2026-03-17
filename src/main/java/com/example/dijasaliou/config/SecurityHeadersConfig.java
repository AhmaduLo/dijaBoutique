package com.example.dijasaliou.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * Configuration des headers de sécurité HTTP
 *
 * SÉCURITÉ : Headers recommandés par OWASP
 * - HSTS : Force HTTPS pour tous les futurs accès
 * - CSP : Prévient les attaques XSS en contrôlant les sources de contenu
 * - X-Content-Type-Options : Empêche le MIME sniffing
 * - X-Frame-Options : Empêche le clickjacking
 * - X-XSS-Protection : Active la protection XSS du navigateur
 * - Referrer-Policy : Contrôle les informations envoyées dans le header Referer
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersConfig implements Filter {

    @Value("${app.cookie.secure:false}")
    private boolean isProduction;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // HSTS : Force HTTPS (UNIQUEMENT EN PRODUCTION)
        // max-age=31536000 = 1 an
        // includeSubDomains = Applique à tous les sous-domaines
        // preload = Permet l'inclusion dans la liste de preload des navigateurs
        if (isProduction) {
            httpResponse.setHeader("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains; preload");
        }

        // Content-Security-Policy : Prévient les attaques XSS
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: blob: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'");

        // X-Content-Type-Options : Empêche le MIME sniffing
        // Le navigateur ne devine pas le type MIME, il utilise celui déclaré
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // X-Frame-Options : Empêche le clickjacking
        // DENY = Ne peut jamais être affiché dans une iframe
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // X-XSS-Protection : Active la protection XSS du navigateur (ancienne protection, CSP est préféré)
        // 1; mode=block = Active et bloque la page si XSS détecté
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer-Policy : Contrôle les informations envoyées dans le header Referer
        // strict-origin-when-cross-origin = Envoie l'origine complète pour same-origin,
        // seulement l'origine pour cross-origin, rien pour HTTPS→HTTP
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions-Policy : Contrôle les fonctionnalités du navigateur
        // Désactive les fonctionnalités non nécessaires (géolocalisation, caméra, microphone, etc.)
        httpResponse.setHeader("Permissions-Policy",
                "geolocation=(), " +
                "microphone=(), " +
                "camera=(), " +
                "payment=(), " +
                "usb=(), " +
                "magnetometer=(), " +
                "gyroscope=(), " +
                "accelerometer=()");

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialisation si nécessaire
    }

    @Override
    public void destroy() {
        // Nettoyage si nécessaire
    }
}
