-- Isolation des devises personnalisees par boutique.
--
-- Jusqu'ici, la table `devises` etait un catalogue GLOBAL partage par toutes
-- les boutiques du SaaS : n'importe quel admin pouvait creer/modifier/supprimer
-- n'importe quelle devise, y compris celles utilisees par d'autres boutiques
-- (ex: changer le taux de l'EUR affectait immediatement tout le monde).
--
-- tenant_id = NULL   -> devise "systeme" partagee (XOF/EUR/USD de depart, et
--                       tout ce qui existe deja a ce jour : on ne peut pas
--                       determiner retroactivement qui a cree quoi, donc
--                       statu quo pour l'existant).
-- tenant_id = <id>   -> devise personnalisee, creee et visible uniquement par
--                       cette boutique.
--
-- La contrainte UNIQUE (tenant_id, code) protege une boutique contre la
-- creation de deux devises avec le meme code. Elle ne protege PAS contre deux
-- lignes tenant_id NULL avec le meme code (MySQL ne compare pas les NULL entre
-- eux dans un index compose) — sans consequence ici car les collisions
-- systeme-vs-tenant sont verifiees cote application (DeviseService), pas par
-- cette contrainte seule.
--
-- Chaque etape est ecrite pour etre rejouable sans erreur (verifie l'etat
-- courant avant d'agir) : du DDL MySQL n'est pas transactionnel, une premiere
-- execution partiellement echouee ne doit pas bloquer un second passage.

-- Supprime l'ancienne contrainte unique sur `code` seul, quel que soit son
-- nom exact (genere automatiquement par Hibernate ddl-auto, potentiellement
-- different d'un environnement a l'autre) — si elle existe encore.
SET @idx_name = (
    SELECT INDEX_NAME FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'devises'
      AND COLUMN_NAME = 'code'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'uk_devise_tenant_code'
    LIMIT 1
);
SET @drop_sql = IF(@idx_name IS NOT NULL,
    CONCAT('ALTER TABLE devises DROP INDEX `', @idx_name, '`'),
    'DO 0');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ajoute la colonne tenant_id, si absente.
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'devises' AND COLUMN_NAME = 'tenant_id'
);
SET @add_col_sql = IF(@col_exists = 0,
    'ALTER TABLE devises ADD COLUMN tenant_id BIGINT NULL',
    'DO 0');
PREPARE stmt FROM @add_col_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ajoute la contrainte de cle etrangere vers tenants, si absente.
SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'devises'
      AND CONSTRAINT_NAME = 'fk_devise_tenant' AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @add_fk_sql = IF(@fk_exists = 0,
    'ALTER TABLE devises ADD CONSTRAINT fk_devise_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)',
    'DO 0');
PREPARE stmt FROM @add_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ajoute la cle unique composite (tenant_id, code), si absente.
SET @uk_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'devises' AND INDEX_NAME = 'uk_devise_tenant_code'
);
SET @add_uk_sql = IF(@uk_exists = 0,
    'ALTER TABLE devises ADD UNIQUE KEY uk_devise_tenant_code (tenant_id, code)',
    'DO 0');
PREPARE stmt FROM @add_uk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
