package com.example.dijasaliou.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de rate limiting pour protéger contre les attaques par force brute
 *
 * SÉCURITÉ : Protection contre les attaques suivantes :
 * - Brute force sur le login (5 tentatives par minute par IP)
 * - Spam de création de comptes (3 comptes par heure par IP)
 * - Spam de réinitialisation de mot de passe (3 tentatives par heure par email)
 *
 * Utilise Bucket4j avec l'algorithme Token Bucket
 */
@Service
@Slf4j
public class RateLimitService {

    // Stockage en mémoire des buckets par IP/email
    // Note : En production avec plusieurs serveurs, utiliser Redis ou Hazelcast
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> passwordResetBuckets = new ConcurrentHashMap<>();

    /**
     * Vérifie si une tentative de login est autorisée
     *
     * LIMITE : 5 tentatives par minute par adresse IP
     *
     * @param ipAddress Adresse IP du client
     * @return true si autorisé, false si limite dépassée
     */
    public boolean allowLogin(String ipAddress) {
        Bucket bucket = loginBuckets.computeIfAbsent(ipAddress, k -> createLoginBucket());
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("⚠️ RATE LIMIT: Tentative de login bloquée pour IP {} (trop de tentatives)", ipAddress);
        }

        return allowed;
    }

    /**
     * Vérifie si une création de compte est autorisée
     *
     * LIMITE : 3 comptes par heure par adresse IP
     *
     * @param ipAddress Adresse IP du client
     * @return true si autorisé, false si limite dépassée
     */
    public boolean allowRegister(String ipAddress) {
        Bucket bucket = registerBuckets.computeIfAbsent(ipAddress, k -> createRegisterBucket());
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("⚠️ RATE LIMIT: Création de compte bloquée pour IP {} (trop de créations)", ipAddress);
        }

        return allowed;
    }

    /**
     * Vérifie si une réinitialisation de mot de passe est autorisée
     *
     * LIMITE : 3 tentatives par heure par email
     *
     * @param email Email de l'utilisateur
     * @return true si autorisé, false si limite dépassée
     */
    public boolean allowPasswordReset(String email) {
        Bucket bucket = passwordResetBuckets.computeIfAbsent(email, k -> createPasswordResetBucket());
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("⚠️ RATE LIMIT: Réinitialisation de mot de passe bloquée pour {} (trop de tentatives)", email);
        }

        return allowed;
    }

    /**
     * Crée un bucket pour les tentatives de login
     *
     * Configuration : 5 tokens, refill 5 tokens par minute
     * = Maximum 5 tentatives de login par minute
     */
    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Crée un bucket pour les créations de compte
     *
     * Configuration : 3 tokens, refill 3 tokens par heure
     * = Maximum 3 créations de compte par heure
     */
    private Bucket createRegisterBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Crée un bucket pour les réinitialisations de mot de passe
     *
     * Configuration : 3 tokens, refill 3 tokens par heure
     * = Maximum 3 réinitialisations par heure
     */
    private Bucket createPasswordResetBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Réinitialise les limites pour une IP donnée (utile après un login réussi)
     *
     * @param ipAddress Adresse IP du client
     */
    public void resetLoginAttempts(String ipAddress) {
        loginBuckets.remove(ipAddress);
        log.debug("Rate limit réinitialisé pour IP {}", ipAddress);
    }

    /**
     * Nettoie les buckets inactifs (à appeler périodiquement)
     *
     * En production, utiliser un @Scheduled pour nettoyer toutes les heures
     */
    public void cleanupInactiveBuckets() {
        int loginSize = loginBuckets.size();
        int registerSize = registerBuckets.size();
        int passwordResetSize = passwordResetBuckets.size();

        // Nettoyer les maps (simpliste, en production utiliser un cache avec TTL)
        loginBuckets.clear();
        registerBuckets.clear();
        passwordResetBuckets.clear();

        log.info("🧹 Nettoyage rate limit buckets - Login: {}, Register: {}, PasswordReset: {}",
                loginSize, registerSize, passwordResetSize);
    }
}
