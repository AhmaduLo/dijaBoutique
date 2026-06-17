-- Ajoute le 2e bloc personnalisable "Mentions légales" en bas de facture.
-- Texte libre : pénalités de retard, juridiction compétente, TVA, etc.
ALTER TABLE tenants
    ADD COLUMN mentions_legales TEXT NULL;
