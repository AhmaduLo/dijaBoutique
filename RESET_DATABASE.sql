-- ============================================
-- Script pour RÉINITIALISER la base de données
-- ============================================
-- ATTENTION : Ce script SUPPRIME TOUTES les données !
-- Utilisez-le uniquement pour les tests
-- ============================================

-- Désactiver les vérifications de clés étrangères temporairement
SET FOREIGN_KEY_CHECKS = 0;

-- Supprimer toutes les données dans l'ordre inverse des dépendances

-- 1. Supprimer les ventes (dépendent de users et tenants)
TRUNCATE TABLE ventes;

-- 2. Supprimer les achats (dépendent de users et tenants)
TRUNCATE TABLE achats;

-- 3. Supprimer les dépenses (dépendent de users et tenants)
TRUNCATE TABLE depenses;

-- 4. Supprimer les utilisateurs (dépendent de tenants)
TRUNCATE TABLE utilisateurs;

-- 5. Supprimer les tenants (table principale)
TRUNCATE TABLE tenants;

-- 6. Supprimer les devises (si vous voulez tout effacer)
-- TRUNCATE TABLE devises;  -- Décommentez si vous voulez aussi effacer les devises

-- Réactiver les vérifications de clés étrangères
SET FOREIGN_KEY_CHECKS = 1;

-- Vérifier que tout est vide
SELECT 'tenants' AS table_name, COUNT(*) AS count FROM tenants
UNION ALL
SELECT 'utilisateurs', COUNT(*) FROM utilisateurs
UNION ALL
SELECT 'achats', COUNT(*) FROM achats
UNION ALL
SELECT 'ventes', COUNT(*) FROM ventes
UNION ALL
SELECT 'depenses', COUNT(*) FROM depenses;

-- Résultat attendu : count = 0 pour toutes les tables
