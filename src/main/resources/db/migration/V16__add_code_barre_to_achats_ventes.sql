-- Ajout du champ code_barre aux tables achats et ventes
-- Permet le scan code-barre (EAN-13, EAN-8, UPC-A, etc.)

-- MySQL n'a pas ALTER TABLE ... ADD COLUMN IF NOT EXISTS
-- Si la colonne existe déjà (créée par ddl-auto=update), cette migration échouera
-- mais repair-on-migrate=true corrigera automatiquement

ALTER TABLE achats ADD COLUMN code_barre VARCHAR(50);
ALTER TABLE ventes ADD COLUMN code_barre VARCHAR(50);
