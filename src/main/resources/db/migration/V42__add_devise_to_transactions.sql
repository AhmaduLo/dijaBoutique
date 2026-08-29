-- Multi-devises : chaque transaction stocke sa devise d'origine et le taux appliqué
-- Les données existantes sont considérées en XOF (devise par défaut Sénégal)

ALTER TABLE achats
    ADD COLUMN devise_code         VARCHAR(10)    NOT NULL DEFAULT 'XOF',
    ADD COLUMN taux_change_applique DECIMAL(15,6) NOT NULL DEFAULT 1.000000;

ALTER TABLE ventes
    ADD COLUMN devise_code         VARCHAR(10)    NOT NULL DEFAULT 'XOF',
    ADD COLUMN taux_change_applique DECIMAL(15,6) NOT NULL DEFAULT 1.000000;

ALTER TABLE depenses
    ADD COLUMN devise_code         VARCHAR(10)    NOT NULL DEFAULT 'XOF',
    ADD COLUMN taux_change_applique DECIMAL(15,6) NOT NULL DEFAULT 1.000000;
