-- Ajoute le champ "conditions et garanties" personnalisable par boutique.
-- Affiché en bas du ticket caisse 80mm si renseigné, ignoré sinon.
ALTER TABLE tenants
    ADD COLUMN conditions_garanties TEXT NULL;
