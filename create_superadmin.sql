-- ============================================================
-- SCRIPT DE CRÉATION DU COMPTE SUPER ADMIN
-- À exécuter UNE SEULE FOIS en base de données
-- ============================================================

-- ÉTAPE 1 : Rendre tenant_id nullable dans la table utilisateurs
-- (Spring Boot ddl-auto=update le fait automatiquement en H2/dev)
-- Pour MySQL en production :
ALTER TABLE utilisateurs MODIFY COLUMN tenant_id BIGINT NULL;
-- Pour PostgreSQL en production :
-- ALTER TABLE utilisateurs ALTER COLUMN tenant_id DROP NOT NULL;


-- ÉTAPE 2 : Créer le compte SUPER_ADMIN
-- IMPORTANT : Remplacez le mot_de_passe par le hash BCrypt de votre mot de passe
-- Vous pouvez générer un hash BCrypt sur https://bcrypt-generator.com/
-- ou avec la commande : htpasswd -bnBC 10 "" votreMotDePasse | tr -d ':\n'

-- Exemple avec le mot de passe "SuperAdmin2025@" :
INSERT INTO utilisateurs (
    nom,
    prenom,
    email,
    mot_de_passe,
    nom_entreprise,
    numero_telephone,
    role,
    deleted,
    acceptation_cgu,
    acceptation_politique_confidentialite,
    date_creation,
    created_date
) VALUES (
    'Admin',
    'Super',
    'superadmin@dijasaliou.com',
    '$2a$10$N9qo8uLOickgx2ZmmYEQmOeH1234567890ABCDEFGHIJKLMNOpqrstu',
    -- ^ REMPLACEZ par le vrai hash BCrypt de votre mot de passe ^
    'Plateforme DijaSaliou',
    '000000000',
    'SUPER_ADMIN',
    false,
    true,
    true,
    NOW(),
    NOW()
);

-- ÉTAPE 3 : Vérification
SELECT id, email, nom, prenom, role, tenant_id
FROM utilisateurs
WHERE role = 'SUPER_ADMIN';

-- ============================================================
-- COMMENT GÉNÉRER LE HASH BCRYPT
-- ============================================================
-- Option 1 : En ligne → https://bcrypt-generator.com/
-- Option 2 : Java → new BCryptPasswordEncoder().encode("votreMotDePasse")
-- Option 3 : Endpoint temporaire → ajouter dans AuthController :
--   @PostMapping("/hash") public String hash(@RequestBody String pwd) {
--     return passwordEncoder.encode(pwd); }
-- ============================================================
