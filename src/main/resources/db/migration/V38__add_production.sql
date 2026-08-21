-- ============================================================
-- MIGRATION V38 : Fonctionnalité "Production" (produits fabriqués)
-- ============================================================
-- Permet aux commerçants qui transforment des ingrédients en produit
-- fini (restaurants, vendeurs de jus, savonniers...) d'enregistrer
-- une production : liste d'ingrédients (coût total par ingrédient) +
-- nombre d'unités produites.
--
-- Architecture : une production ne crée AUCUN nouveau moteur de stock.
-- Elle génère simplement un lot `achats` normal pour le produit fabriqué
-- (nom_produit = nom du produit fabriqué, quantite = quantité produite,
-- prix_total = coût total des ingrédients). Le FIFO existant
-- (quantite_restante, FifoCalculService) s'applique ensuite sans aucune
-- modification de code : le bénéfice est automatiquement rattaché aux
-- unités VENDUES, pas produites.
--
-- Les lignes d'ingrédients ne sont JAMAIS insérées dans `achats`
-- (ça compterait le coût de production deux fois) : elles sont stockées
-- à part, uniquement pour la traçabilité/le détail du coût.
--
-- Migration ADDITIVE : aucune donnée existante n'est modifiée.
-- ============================================================

-- ============================================================
-- 1. produits_fabriques — catalogue des noms de produits "fabriqués"
-- ============================================================
-- Pilote la visibilité du bouton "Nouvelle production" côté frontend
-- (masqué tant qu'aucune ligne n'existe pour le tenant).
CREATE TABLE IF NOT EXISTS produits_fabriques (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,

    tenant_id           BIGINT NOT NULL
        COMMENT 'FK vers le tenant (multi-tenant)',
    nom_produit         VARCHAR(100) NOT NULL
        COMMENT 'Nom du produit fabriqué, partagé avec achats.nom_produit / ventes.nom_produit',
    unite               VARCHAR(20) NULL DEFAULT 'pièce'
        COMMENT 'Unité de vente du produit fabriqué (plat, litre, savon...)',
    description         VARCHAR(200) NULL,
    actif               TINYINT(1) NOT NULL DEFAULT 1
        COMMENT 'Désactivé = ne plus apparaître dans le sélecteur de production (soft toggle, jamais de suppression dure)',

    -- Audit (hérité de BaseEntity / AbstractAuditable)
    version             BIGINT NULL,
    created_date        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  DATETIME NULL,
    created_by          VARCHAR(50) NULL,
    last_modified_by    VARCHAR(50) NULL,

    CONSTRAINT fk_produit_fabrique_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uk_produit_fabrique_tenant_nom
        UNIQUE (tenant_id, nom_produit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Catalogue des produits fabriqués (issus de production) déclarés par le tenant.';

CREATE INDEX idx_produit_fabrique_tenant ON produits_fabriques(tenant_id);

-- ============================================================
-- 2. productions — un événement de production = un lot achats généré
-- ============================================================
CREATE TABLE IF NOT EXISTS productions (
    id                       VARCHAR(36) PRIMARY KEY
        COMMENT 'UUID, même convention que achats/ventes',

    tenant_id                BIGINT NOT NULL
        COMMENT 'FK vers le tenant (multi-tenant)',
    produit_fabrique_id      BIGINT NOT NULL
        COMMENT 'FK vers produits_fabriques',
    achat_id                 VARCHAR(36) NOT NULL
        COMMENT 'FK vers le lot achats créé automatiquement pour cette production',
    utilisateur_id           BIGINT NOT NULL
        COMMENT 'FK vers utilisateurs : qui a enregistré la production',

    quantite_produite        DOUBLE NOT NULL
        COMMENT 'Nombre d''unités produites lors de ce batch',
    cout_ingredients_total   DECIMAL(12,2) NOT NULL
        COMMENT 'Somme des coûts ingrédients = dépense totale de production',
    cout_unitaire            DECIMAL(10,2) NOT NULL
        COMMENT 'cout_ingredients_total / quantite_produite (arrondi HALF_UP), dénormalisé pour affichage rapide',

    date_production          DATETIME NOT NULL,
    notes                    VARCHAR(200) NULL,

    -- Audit (hérité de BaseEntity / AbstractAuditable)
    version                  BIGINT NULL,
    created_date              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date       DATETIME NULL,
    created_by                VARCHAR(50) NULL,
    last_modified_by         VARCHAR(50) NULL,

    CONSTRAINT fk_production_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_production_produit_fabrique
        FOREIGN KEY (produit_fabrique_id) REFERENCES produits_fabriques(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_production_achat
        FOREIGN KEY (achat_id) REFERENCES achats(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_production_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uk_production_achat
        UNIQUE (achat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Un événement de production = un lot achats créé automatiquement pour le produit fabriqué.';

CREATE INDEX idx_production_tenant  ON productions(tenant_id);
CREATE INDEX idx_production_produit ON productions(produit_fabrique_id);
CREATE INDEX idx_production_date    ON productions(tenant_id, date_production);

-- ============================================================
-- 3. production_ingredients — détail des ingrédients (traçabilité)
-- ============================================================
-- N'alimente JAMAIS `achats` : ce n'est pas du stock, juste le détail
-- du coût de production, rattaché à l'unique lot achats de productions.achat_id.
CREATE TABLE IF NOT EXISTS production_ingredients (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,

    production_id       VARCHAR(36) NOT NULL
        COMMENT 'FK vers productions',
    tenant_id           BIGINT NOT NULL
        COMMENT 'FK vers le tenant (multi-tenant)',

    nom_ingredient      VARCHAR(150) NOT NULL
        COMMENT 'Libre — riz, poisson, huile... (pas de FK vers achats : traçabilité de coût seulement)',
    cout                DECIMAL(10,2) NOT NULL
        COMMENT 'Coût total de cet ingrédient pour ce batch (montant dépensé, pas un prix unitaire)',
    ordre               INT NOT NULL DEFAULT 0
        COMMENT 'Ordre d''affichage dans le formulaire',

    -- Audit (hérité de BaseEntity / AbstractAuditable)
    version             BIGINT NULL,
    created_date        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date  DATETIME NULL,
    created_by          VARCHAR(50) NULL,
    last_modified_by    VARCHAR(50) NULL,

    CONSTRAINT fk_prod_ingredient_production
        FOREIGN KEY (production_id) REFERENCES productions(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_prod_ingredient_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Détail des ingrédients (coût) d''une production — traçabilité uniquement, ne génère pas d''achats.';

CREATE INDEX idx_prod_ingredient_production ON production_ingredients(production_id);
CREATE INDEX idx_prod_ingredient_tenant      ON production_ingredients(tenant_id);
