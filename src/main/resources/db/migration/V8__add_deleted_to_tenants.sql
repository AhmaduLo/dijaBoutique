-- V8 : Ajout champs suppression définitive sur les tenants
-- Distinction entre suspension (actif=false, réversible) et suppression (deleted=true, irréversible)
-- Utilise IF NOT EXISTS (MySQL 8.0+) pour éviter l'erreur si les colonnes existent déjà

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS deleted          BIT(1)   NOT NULL DEFAULT b'0',
    ADD COLUMN IF NOT EXISTS date_suppression DATETIME NULL;
