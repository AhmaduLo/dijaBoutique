-- ============================================================
-- MIGRATION V39 : Unité par ligne d'ingrédient de production
-- ============================================================
-- Le formulaire "Nouvelle production" permet désormais de saisir
-- l'unité de chaque ingrédient (kg, litre, pièce...), comme pour
-- les achats classiques. Colonne additive et nullable : les lignes
-- déjà enregistrées restent valides sans unité renseignée.
-- ============================================================
ALTER TABLE production_ingredients
ADD COLUMN unite VARCHAR(20) NULL
COMMENT 'Unité de mesure de l''ingrédient (kg, litre, pièce...), optionnelle';
