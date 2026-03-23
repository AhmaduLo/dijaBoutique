package com.example.dijasaliou.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.TimeUnit;

/**
 * Configuration globale :
 * - Cache Caffeine (remplace les requêtes BDD répétitives)
 * - @Async (envoi d'emails en arrière-plan)
 * - @Scheduled (nettoyage automatique des tokens expirés)
 */
@Configuration
@EnableCaching
@EnableAsync
@EnableScheduling
public class AppConfig {

    /**
     * Cache en mémoire Caffeine — léger et performant.
     *
     * Caches configurés :
     * - "tenants"  : statut abonnement tenant (TTL 5 min) — évite 1 requête BDD par appel API
     * - "stocks"   : stocks calculés (TTL 2 min) — évite 5 recalculs par endpoint
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Cache tenant : 5 min (fréquence des requêtes vs fraîcheur des données)
        manager.registerCustomCache("tenants",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .build());

        // Cache stocks : 2 min (données changent à chaque vente/achat)
        manager.registerCustomCache("stocks",
                Caffeine.newBuilder()
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .build());

        return manager;
    }
}
