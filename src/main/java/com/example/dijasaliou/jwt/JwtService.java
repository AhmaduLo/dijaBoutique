package com.example.dijasaliou.jwt;

import com.example.dijasaliou.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

/**
 * Service pour gérer les tokens JWT
 *
 * Responsabilités :
 * - Générer un token JWT
 * - Valider un token JWT
 * - Extraire l'email du token
 */
@Service
public class JwtService {

    // Clé secrète pour signer les tokens (à mettre dans application.properties)
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Durée de validité du token (24 heures)
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Générer un token JWT pour un utilisateur (sans tenant - pour compatibilité)
     *
     * @param email Email de l'utilisateur
     * @return Token JWT
     */
    public String generateToken(String email) {
        return generateToken(email, null);
    }

    /**
     * Générer un token JWT pour un utilisateur avec tenant_id et rôle
     *
     * MULTI-TENANT : Le tenant_id est CRUCIAL pour la sécurité
     * Le rôle est stocké dans le token pour éviter une requête BDD à chaque appel API
     *
     * @param email    Email de l'utilisateur
     * @param tenantId UUID du tenant (entreprise)
     * @param role     Rôle de l'utilisateur (ADMIN, USER, SUPER_ADMIN)
     * @return Token JWT
     */
    public String generateToken(String email, String tenantId, UserEntity.Role role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        var builder = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey());

        if (tenantId != null && !tenantId.trim().isEmpty()) {
            builder.claim("tenant_id", tenantId);
        }
        if (role != null) {
            builder.claim("role", role.name());
        }

        return builder.compact();
    }

    /**
     * @deprecated Utiliser generateToken(email, tenantId, role) pour éviter les requêtes BDD
     */
    @Deprecated
    public String generateToken(String email, String tenantId) {
        return generateToken(email, tenantId, null);
    }

    /**
     * Extraire l'email du token JWT
     *
     * @param token Token JWT
     * @return Email de l'utilisateur
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    /**
     * Extraire le rôle du token JWT (évite une requête BDD dans le filtre)
     *
     * @param token Token JWT
     * @return Nom du rôle ou null si absent
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extraire le tenant_id du token JWT
     *
     * MULTI-TENANT : Cette méthode est CRITIQUE pour la sécurité
     * Elle extrait l'UUID du tenant pour filtrer les données
     *
     * @param token Token JWT
     * @return UUID du tenant ou null si absent
     */
    public String getTenantIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("tenant_id", String.class);
    }

    /**
     * Extraire tous les claims du token
     *
     * @param token Token JWT
     * @return Claims
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Valider un token JWT
     *
     * @param token Token JWT
     * @return true si valide, false sinon
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token invalide ou expiré
            return false;
        }
    }

    /**
     * Obtenir la clé de signature
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
