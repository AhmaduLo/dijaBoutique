-- Migration V2 : Ajout du champ prix_vente_suggere dans la table achats
-- Ce champ permet de renseigner un prix de vente suggéré lors de l'achat
-- pour faciliter la création de ventes ultérieures

ALTER TABLE achats
ADD COLUMN prix_vente_suggere DECIMAL(10,2);

-- Commentaire pour expliquer le champ
COMMENT ON COLUMN achats.prix_vente_suggere IS 'Prix de vente suggéré pour faciliter les ventes futures';
