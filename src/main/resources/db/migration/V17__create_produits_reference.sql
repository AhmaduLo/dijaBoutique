-- Table partagée de codes-barres — accessible par tous les tenants
-- Pas de tenant_id : données communautaires

CREATE TABLE IF NOT EXISTS produits_reference (
    id BIGSERIAL PRIMARY KEY,
    code_barre VARCHAR(50) NOT NULL UNIQUE,
    nom_produit VARCHAR(200) NOT NULL,
    photo_url VARCHAR(500),
    categorie VARCHAR(100),
    nb_utilisations INTEGER NOT NULL DEFAULT 1,
    contribue_par_tenant_nom VARCHAR(100),
    date_creation TIMESTAMP NOT NULL DEFAULT NOW(),
    date_modification TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prodref_code_barre ON produits_reference(code_barre);
CREATE INDEX IF NOT EXISTS idx_prodref_nom ON produits_reference(nom_produit);
