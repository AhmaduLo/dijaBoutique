-- ============================================
-- MIGRATION MULTI-TENANT
-- ============================================
-- Cette migration transforme l'application en SaaS multi-tenant
-- Chaque entreprise est isolée dans son propre tenant
--
-- ATTENTION : Exécuter cette migration nécessite une stratégie de données existantes
-- Si vous avez des données existantes, vous devrez créer un tenant par défaut
-- et assigner toutes les données existantes à ce tenant
-- ============================================

-- ============================================
-- 1. TABLE TENANTS (ENTREPRISES)
-- ============================================
CREATE TABLE IF NOT EXISTS tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_uuid VARCHAR(36) NOT NULL UNIQUE COMMENT 'UUID unique pour identifier le tenant',
    nom_entreprise VARCHAR(100) NOT NULL COMMENT 'Nom de l entreprise',
    numero_telephone VARCHAR(20) NOT NULL COMMENT 'Numero de telephone de l entreprise',
    adresse VARCHAR(255) COMMENT 'Adresse de l entreprise',
    ville VARCHAR(100) COMMENT 'Ville',
    pays VARCHAR(100) COMMENT 'Pays',
    actif BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Statut actif/inactif du tenant',
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de creation du tenant',
    date_expiration DATETIME COMMENT 'Date d expiration (pour gestion d abonnements)',
    plan VARCHAR(20) NOT NULL DEFAULT 'GRATUIT' COMMENT 'Plan d abonnement (GRATUIT, BASIC, PREMIUM, ENTREPRISE)',

    INDEX idx_tenant_uuid (tenant_uuid),
    INDEX idx_tenant_actif (actif)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Table des tenants (entreprises) pour le mode SaaS multi-tenant';

-- ============================================
-- 2. MODIFICATION TABLE UTILISATEURS
-- ============================================
-- Ajouter la colonne tenant_id (d'abord nullable pour migration)
ALTER TABLE utilisateurs
ADD COLUMN tenant_id BIGINT COMMENT 'ID du tenant auquel appartient l utilisateur';

-- Ajouter l'index
ALTER TABLE utilisateurs
ADD INDEX idx_user_tenant (tenant_id);

-- Ajouter la contrainte de clé étrangère
ALTER TABLE utilisateurs
ADD CONSTRAINT fk_user_tenant
FOREIGN KEY (tenant_id) REFERENCES tenants(id)
ON DELETE RESTRICT ON UPDATE CASCADE;

-- IMPORTANT : Pour les données existantes, créer un tenant par défaut
-- et assigner tous les utilisateurs existants à ce tenant
INSERT INTO tenants (tenant_uuid, nom_entreprise, numero_telephone, actif, plan)
VALUES (UUID(), 'Entreprise Par Defaut', '+221000000000', TRUE, 'PREMIUM')
ON DUPLICATE KEY UPDATE id=id;

-- Assigner tous les utilisateurs existants au tenant par défaut
UPDATE utilisateurs
SET tenant_id = (SELECT id FROM tenants WHERE nom_entreprise = 'Entreprise Par Defaut')
WHERE tenant_id IS NULL;

-- Maintenant, rendre la colonne NOT NULL
ALTER TABLE utilisateurs
MODIFY COLUMN tenant_id BIGINT NOT NULL;

-- ============================================
-- 3. MODIFICATION TABLE ACHATS
-- ============================================
ALTER TABLE achats
ADD COLUMN tenant_id BIGINT COMMENT 'ID du tenant auquel appartient l achat';

ALTER TABLE achats
ADD INDEX idx_achat_tenant (tenant_id);

ALTER TABLE achats
ADD CONSTRAINT fk_achat_tenant
FOREIGN KEY (tenant_id) REFERENCES tenants(id)
ON DELETE RESTRICT ON UPDATE CASCADE;

-- Assigner tous les achats existants au tenant de leur utilisateur
UPDATE achats a
INNER JOIN utilisateurs u ON a.utilisateur_id = u.id
SET a.tenant_id = u.tenant_id
WHERE a.tenant_id IS NULL;

ALTER TABLE achats
MODIFY COLUMN tenant_id BIGINT NOT NULL;

-- ============================================
-- 4. MODIFICATION TABLE VENTES
-- ============================================
ALTER TABLE ventes
ADD COLUMN tenant_id BIGINT COMMENT 'ID du tenant auquel appartient la vente';

ALTER TABLE ventes
ADD INDEX idx_vente_tenant (tenant_id);

ALTER TABLE ventes
ADD CONSTRAINT fk_vente_tenant
FOREIGN KEY (tenant_id) REFERENCES tenants(id)
ON DELETE RESTRICT ON UPDATE CASCADE;

-- Assigner toutes les ventes existantes au tenant de leur utilisateur
UPDATE ventes v
INNER JOIN utilisateurs u ON v.utilisateur_id = u.id
SET v.tenant_id = u.tenant_id
WHERE v.tenant_id IS NULL;

ALTER TABLE ventes
MODIFY COLUMN tenant_id BIGINT NOT NULL;

-- ============================================
-- 5. MODIFICATION TABLE DEPENSES
-- ============================================
ALTER TABLE depenses
ADD COLUMN tenant_id BIGINT COMMENT 'ID du tenant auquel appartient la depense';

ALTER TABLE depenses
ADD INDEX idx_depense_tenant (tenant_id);

ALTER TABLE depenses
ADD CONSTRAINT fk_depense_tenant
FOREIGN KEY (tenant_id) REFERENCES tenants(id)
ON DELETE RESTRICT ON UPDATE CASCADE;

-- Assigner toutes les dépenses existantes au tenant de leur utilisateur
UPDATE depenses d
INNER JOIN utilisateurs u ON d.utilisateur_id = u.id
SET d.tenant_id = u.tenant_id
WHERE d.tenant_id IS NULL;

ALTER TABLE depenses
MODIFY COLUMN tenant_id BIGINT NOT NULL;

-- ============================================
-- 6. VÉRIFICATIONS FINALES
-- ============================================
-- Vérifier que tous les enregistrements ont un tenant_id
SELECT 'Utilisateurs sans tenant' AS verification,
       COUNT(*) AS count
FROM utilisateurs
WHERE tenant_id IS NULL;

SELECT 'Achats sans tenant' AS verification,
       COUNT(*) AS count
FROM achats
WHERE tenant_id IS NULL;

SELECT 'Ventes sans tenant' AS verification,
       COUNT(*) AS count
FROM ventes
WHERE tenant_id IS NULL;

SELECT 'Depenses sans tenant' AS verification,
       COUNT(*) AS count
FROM depenses
WHERE tenant_id IS NULL;

-- ============================================
-- FIN DE LA MIGRATION
-- ============================================
-- IMPORTANT : Après cette migration :
-- 1. Redémarrer l'application
-- 2. Tester la création d'un nouveau tenant (inscription)
-- 3. Vérifier l'isolation des données
-- 4. SUPPRIMER le tenant par défaut si nécessaire (après migration des données)
-- ============================================
