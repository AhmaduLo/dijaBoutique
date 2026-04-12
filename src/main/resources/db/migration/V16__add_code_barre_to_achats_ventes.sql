-- Ajout du champ code_barre aux tables achats et ventes
-- Permet le scan code-barre (EAN-13, EAN-8, UPC-A, etc.)

ALTER TABLE achats ADD COLUMN IF NOT EXISTS code_barre VARCHAR(50);
ALTER TABLE ventes ADD COLUMN IF NOT EXISTS code_barre VARCHAR(50);
