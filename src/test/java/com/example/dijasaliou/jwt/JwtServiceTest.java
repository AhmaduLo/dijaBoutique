package com.example.dijasaliou.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests unitaires — JwtService (sécurité multi-tenant)")
class JwtServiceTest {

    // Clé de 32+ caractères requise pour HMAC-SHA256
    private static final String SECRET = "dijasaliou-secret-key-for-tests!!";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 heure

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", EXPIRATION_MS);
    }

    // =========================================================
    // generateToken — format et contenu
    // =========================================================

    @Test
    @DisplayName("generateToken(email) — retourne un token non null et non vide")
    void generateToken_retourneTokenNonNull() {
        String token = jwtService.generateToken("amadou@example.com");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("generateToken(email, tenantId) — le token contient bien le tenant_id")
    void generateToken_avecTenant_contientTenantId() {
        String token = jwtService.generateToken("amadou@example.com", "uuid-boutique-dijasal");

        assertThat(jwtService.getTenantIdFromToken(token)).isEqualTo("uuid-boutique-dijasal");
    }

    @Test
    @DisplayName("generateToken(email, null) — le tenant_id est absent du token")
    void generateToken_tenantIdNull_abscentDuToken() {
        String token = jwtService.generateToken("amadou@example.com", null);

        assertThat(jwtService.getTenantIdFromToken(token)).isNull();
    }

    @Test
    @DisplayName("generateToken(email) surcharge sans tenant — le tenant_id est absent")
    void generateToken_surchargeEmail_tenantIdNull() {
        String token = jwtService.generateToken("amadou@example.com");

        assertThat(jwtService.getTenantIdFromToken(token)).isNull();
    }

    // =========================================================
    // getEmailFromToken
    // =========================================================

    @Test
    @DisplayName("getEmailFromToken() — retourne exactement l'email inclus dans le token")
    void getEmailFromToken_retourneEmailCorrect() {
        String token = jwtService.generateToken("amadou@example.com", "uuid-tenant");

        String email = jwtService.getEmailFromToken(token);

        assertThat(email).isEqualTo("amadou@example.com");
    }

    // =========================================================
    // getTenantIdFromToken — CRITIQUE sécurité multi-tenant
    // =========================================================

    @Test
    @DisplayName("getTenantIdFromToken() — retourne le tenant_id exact [CRITIQUE]")
    void getTenantIdFromToken_retourneTenantIdCorrect() {
        String token = jwtService.generateToken("amadou@example.com", "uuid-boutique-dijasal");

        String tenantId = jwtService.getTenantIdFromToken(token);

        assertThat(tenantId).isEqualTo("uuid-boutique-dijasal");
    }

    @Test
    @DisplayName("getTenantIdFromToken() — retourne null si absent du token [CRITIQUE]")
    void getTenantIdFromToken_retourneNullSiAbsent() {
        String token = jwtService.generateToken("amadou@example.com");

        String tenantId = jwtService.getTenantIdFromToken(token);

        // SÉCURITÉ : null signifie aucun tenant → ne doit pas donner accès aux données
        assertThat(tenantId).isNull();
    }

    @Test
    @DisplayName("getTenantIdFromToken() — deux tenants ne se confondent jamais [CRITIQUE isolation]")
    void getTenantIdFromToken_isolationEntreDeuxTenants() {
        String tokenTenant1 = jwtService.generateToken("user@boutique1.com", "uuid-tenant-1");
        String tokenTenant2 = jwtService.generateToken("user@boutique2.com", "uuid-tenant-2");

        String tenantId1 = jwtService.getTenantIdFromToken(tokenTenant1);
        String tenantId2 = jwtService.getTenantIdFromToken(tokenTenant2);

        assertThat(tenantId1).isEqualTo("uuid-tenant-1");
        assertThat(tenantId2).isEqualTo("uuid-tenant-2");
        // Les deux tokens ne doivent jamais retourner le même tenant_id
        assertThat(tenantId1).isNotEqualTo(tenantId2);
    }

    // =========================================================
    // validateToken
    // =========================================================

    @Test
    @DisplayName("validateToken() — retourne true pour un token valide")
    void validateToken_retourneTrue_tokenValide() {
        String token = jwtService.generateToken("amadou@example.com", "uuid-tenant");

        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken() — retourne false pour un token expiré")
    void validateToken_retourneFalse_tokenExpire() {
        // Service configuré avec expiration négative → token immédiatement expiré
        JwtService serviceExpire = new JwtService();
        ReflectionTestUtils.setField(serviceExpire, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(serviceExpire, "jwtExpirationMs", -1000L);

        String tokenExpire = serviceExpire.generateToken("amadou@example.com", "uuid-tenant");

        assertThat(jwtService.validateToken(tokenExpire)).isFalse();
    }

    @Test
    @DisplayName("validateToken() — retourne false pour un token falsifié")
    void validateToken_retourneFalse_tokenFalsifie() {
        // Token JWT avec signature inventée
        String tokenFalsifie = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJzdWIiOiJoYWNrZXJAZXZpbC5jb20iLCJ0ZW5hbnRfaWQiOiJ1dWlkLXZpY3RpbWUifQ"
                + ".signature-totalement-inventee";

        assertThat(jwtService.validateToken(tokenFalsifie)).isFalse();
    }

    @Test
    @DisplayName("validateToken() — retourne false pour un token signé avec une autre clé secrète")
    void validateToken_retourneFalse_autreSignature() {
        // Quelqu'un génère un token avec une clé différente
        JwtService autreService = new JwtService();
        ReflectionTestUtils.setField(autreService, "jwtSecret", "autre-cle-secrete-inconnue-256b!!");
        ReflectionTestUtils.setField(autreService, "jwtExpirationMs", EXPIRATION_MS);

        String tokenAutreSecret = autreService.generateToken("amadou@example.com", "uuid-tenant");

        // Notre service DOIT rejeter ce token — signature invalide
        assertThat(jwtService.validateToken(tokenAutreSecret)).isFalse();
    }

    @Test
    @DisplayName("validateToken() — retourne false pour une chaîne vide")
    void validateToken_retourneFalse_chaineVide() {
        assertThat(jwtService.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken() — retourne false pour une chaîne aléatoire non JWT")
    void validateToken_retourneFalse_chaineAleatoire() {
        assertThat(jwtService.validateToken("ceci-nest-pas-un-token")).isFalse();
    }
}
