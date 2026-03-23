package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service de cache pour les tenants.
 *
 * Évite une requête BDD à chaque appel API dans SubscriptionExpirationFilter.
 * TTL : 5 minutes (configuré dans AppConfig.cacheManager).
 *
 * Invalider le cache après tout changement de plan/abonnement.
 */
@Service
@RequiredArgsConstructor
public class TenantCacheService {

    private final TenantRepository tenantRepository;

    /**
     * Récupère un tenant par UUID — résultat mis en cache 5 min.
     * Clé de cache = tenantUuid.
     */
    @Cacheable(value = "tenants", key = "#tenantUuid")
    public Optional<TenantEntity> findByUuid(String tenantUuid) {
        return tenantRepository.findByTenantUuid(tenantUuid);
    }

    /**
     * Invalide le cache pour un tenant donné.
     * À appeler après tout changement de plan ou d'abonnement.
     */
    @CacheEvict(value = "tenants", key = "#tenantUuid")
    public void evict(String tenantUuid) {
        // méthode vide — l'annotation fait le travail
    }
}
