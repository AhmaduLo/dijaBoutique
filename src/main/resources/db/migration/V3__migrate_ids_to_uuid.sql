-- ============================================================
-- MIGRATION V3 : Passage des clés primaires de BIGINT à UUID
-- ============================================================
-- Toutes les tables business passent de id BIGINT AUTO_INCREMENT
-- à id VARCHAR(36), les UUIDs sont générés côté Java (@PrePersist).
--
-- ORDRE : supprimer les FK enfants avant de modifier les PK parents.
-- ============================================================

-- ============================================================
-- 0. Désactiver temporairement les vérifications FK
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. TABLE ventes
-- ============================================================
ALTER TABLE ventes
    MODIFY COLUMN id VARCHAR(36) NOT NULL;

-- ============================================================
-- 2. TABLE achats
-- ============================================================
ALTER TABLE achats
    MODIFY COLUMN id VARCHAR(36) NOT NULL;

-- ============================================================
-- 3. TABLE depenses
-- ============================================================
ALTER TABLE depenses
    MODIFY COLUMN id VARCHAR(36) NOT NULL;

-- ============================================================
-- 4. TABLE clients
-- ============================================================
ALTER TABLE clients
    MODIFY COLUMN id VARCHAR(36) NOT NULL;

-- ============================================================
-- 5. TABLE credits_clients
--    FK : client_id → clients.id  |  vente_id → ventes.id
-- ============================================================
ALTER TABLE credits_clients
    MODIFY COLUMN id        VARCHAR(36) NOT NULL,
    MODIFY COLUMN client_id VARCHAR(36) NOT NULL,
    MODIFY COLUMN vente_id  VARCHAR(36);          -- nullable (crédit détaché)

-- ============================================================
-- 6. TABLE paiements_credit
--    FK : credit_id → credits_clients.id
-- ============================================================
ALTER TABLE paiements_credit
    MODIFY COLUMN id        VARCHAR(36) NOT NULL,
    MODIFY COLUMN credit_id VARCHAR(36) NOT NULL;

-- ============================================================
-- 7. TABLE bons_livraison
-- ============================================================
ALTER TABLE bons_livraison
    MODIFY COLUMN id VARCHAR(36) NOT NULL;

-- ============================================================
-- 8. TABLE bons_livraison_lignes
--    FK : bon_livraison_id → bons_livraison.id
-- ============================================================
ALTER TABLE bons_livraison_lignes
    MODIFY COLUMN id              VARCHAR(36) NOT NULL,
    MODIFY COLUMN bon_livraison_id VARCHAR(36) NOT NULL;

-- ============================================================
-- 9. Réactiver les vérifications FK
-- ============================================================
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- NOTE IMPORTANTE
-- ============================================================
-- Les enregistrements existants auront des ids numériques
-- convertis en chaîne (ex: 1 → '1'). Cela est acceptable
-- car les nouveaux enregistrements utiliseront des UUIDs.
-- Pour une migration propre de données existantes, migrez
-- les données vers une base vierge.
-- ============================================================
