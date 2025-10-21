package com.example.dijasaliou.jwt;

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
     * Générer un token JWT pour un utilisateur
     *
     * @param email Email de l'utilisateur
     * @return Token JWT
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(email)                    // Email dans le token
                .setIssuedAt(now)                     // Date de création
                .setExpiration(expiryDate)            // Date d'expiration
                .signWith(getSigningKey())            // Signature avec la clé secrète
                .compact();
    }

    /**
     * Extraire l'email du token JWT
     *
     * @param token Token JWT
     * @return Email de l'utilisateur
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
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
