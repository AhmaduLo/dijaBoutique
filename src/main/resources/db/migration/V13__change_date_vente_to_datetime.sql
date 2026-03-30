-- V13 : Changer date_vente de DATE à DATETIME pour stocker l'heure exacte de la vente
-- Contexte : le ticket doit afficher l'heure réelle de la transaction (ex: 00:02)
--            et cette valeur ne doit jamais changer, même des années plus tard.
-- Impact sur les données existantes : les lignes existantes auront l'heure 00:00:00
--   (valeur par défaut lors de la conversion DATE → DATETIME). Acceptable.
ALTER TABLE ventes MODIFY COLUMN date_vente DATETIME NOT NULL;
ALTER TABLE achats MODIFY COLUMN date_achat DATETIME NOT NULL;
ALTER TABLE depenses MODIFY COLUMN date_depense DATETIME NOT NULL;
