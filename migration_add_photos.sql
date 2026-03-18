-- ============================================
-- MIGRATION : Ajout du support des photos
-- Date : 2025-01-12
-- Description : Ajoute la colonne photo_url dans les tables achats et ventes
-- ============================================

-- 1. Ajouter la colonne photo_url dans la table achats
ALTER TABLE achats
ADD COLUMN photo_url VARCHAR(500);

-- 2. Ajouter un commentaire sur la colonne
COMMENT ON COLUMN achats.photo_url IS 'URL relative de la photo du produit acheté (optionnel)';

-- 3. Ajouter un index pour améliorer les performances des recherches
CREATE INDEX idx_achat_photo ON achats(photo_url);

-- ============================================

-- 4. Ajouter la colonne photo_url dans la table ventes
ALTER TABLE ventes
ADD COLUMN photo_url VARCHAR(500);

-- 5. Ajouter un commentaire sur la colonne
COMMENT ON COLUMN ventes.photo_url IS 'URL relative de la photo du produit vendu (optionnel)';

-- 6. Ajouter un index pour améliorer les performances des recherches
CREATE INDEX idx_vente_photo ON ventes(photo_url);

-- ============================================

-- 7. (OPTIONNEL) Ajouter aussi dans la table dépenses si vous voulez des photos pour les dépenses
-- ALTER TABLE depenses
-- ADD COLUMN photo_url VARCHAR(500);
--
-- COMMENT ON COLUMN depenses.photo_url IS 'URL relative de la photo justificative de la dépense (optionnel)';
--
-- CREATE INDEX idx_depense_photo ON depenses(photo_url);

-- ============================================
-- VÉRIFICATION
-- ============================================

-- Vérifier que les colonnes ont bien été ajoutées
SELECT
    table_name,
    column_name,
    data_type,
    character_maximum_length,
    is_nullable
FROM information_schema.columns
WHERE table_name IN ('achats', 'ventes')
  AND column_name = 'photo_url';

-- Vérifier que les index ont bien été créés
SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename IN ('achats', 'ventes')
  AND indexname LIKE '%photo%';

-- ============================================
-- ROLLBACK (en cas de besoin)
-- ============================================

-- Si vous voulez annuler cette migration :
-- DROP INDEX IF EXISTS idx_achat_photo;
-- ALTER TABLE achats DROP COLUMN IF EXISTS photo_url;
--
-- DROP INDEX IF EXISTS idx_vente_photo;
-- ALTER TABLE ventes DROP COLUMN IF EXISTS photo_url;
