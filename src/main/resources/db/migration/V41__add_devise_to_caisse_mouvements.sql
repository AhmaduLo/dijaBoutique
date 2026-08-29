-- Multi-devises : les mouvements manuels et transferts de caisse stockent désormais
-- leur devise d'origine et le taux appliqué, comme achats/ventes/dépenses (V16).
-- Les données existantes sont considérées en XOF (comportement inchangé).

ALTER TABLE transfert_caisse
    ADD COLUMN devise_code          VARCHAR(10)   NOT NULL DEFAULT 'XOF',
    ADD COLUMN taux_change_applique DECIMAL(15,6) NOT NULL DEFAULT 1.000000;

ALTER TABLE mouvement_caisse_manuel
    ADD COLUMN devise_code          VARCHAR(10)   NOT NULL DEFAULT 'XOF',
    ADD COLUMN taux_change_applique DECIMAL(15,6) NOT NULL DEFAULT 1.000000;
