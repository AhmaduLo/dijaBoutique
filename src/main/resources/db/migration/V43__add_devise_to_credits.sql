-- Devise sur les crédits clients : chaque crédit stocke la devise de la vente d'origine
-- Valeur par défaut XOF/1.0 pour les enregistrements existants

ALTER TABLE credits_clients
    ADD COLUMN devise_code         VARCHAR(10)   NOT NULL DEFAULT 'XOF',
    ADD COLUMN taux_change_applique DECIMAL(15,6) NOT NULL DEFAULT 1.000000;

-- Devise sur les paiements de crédit
ALTER TABLE paiements_credit
    ADD COLUMN devise_code         VARCHAR(10)   NOT NULL DEFAULT 'XOF',
    ADD COLUMN taux_change_applique DECIMAL(15,6) NOT NULL DEFAULT 1.000000;

-- Rétro-remplir les crédits liés à des ventes avec le taux de la vente
UPDATE credits_clients c
    JOIN ventes v ON c.vente_id = v.id
SET c.devise_code          = v.devise_code,
    c.taux_change_applique = v.taux_change_applique
WHERE c.vente_id IS NOT NULL;

-- Rétro-remplir les paiements avec le taux du crédit parent
UPDATE paiements_credit p
    JOIN credits_clients c ON p.credit_id = c.id
SET p.devise_code          = c.devise_code,
    p.taux_change_applique = c.taux_change_applique;
