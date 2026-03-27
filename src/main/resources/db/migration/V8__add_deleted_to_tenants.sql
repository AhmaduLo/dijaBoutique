-- V8 : Ajout champs suppression définitive sur les tenants
-- Distinction entre suspension (actif=false, réversible) et suppression (deleted=true, irréversible)

ALTER TABLE tenants
    ADD COLUMN deleted      BIT(1)      NOT NULL DEFAULT b'0',
    ADD COLUMN date_suppression DATETIME    NULL;
