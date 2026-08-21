-- ============================================================
-- MIGRATION V21 : Suivi du bénéfice net par méthode FIFO
-- ============================================================
-- Ajoute la traçabilité FIFO (First In, First Out) pour calculer
-- le bénéfice net réel de chaque vente, lot par lot.
--
-- Changements :
--   1. Colonne quantite_restante sur achats : suit le stock restant
--      de chaque lot d'achat.
--   2. Nouvelle table vente_lot_consommation : enregistre dans quel(s)
--      lot(s) d'achat chaque vente a puisé, avec snapshot du prix d'achat
--      et du bénéfice au moment de la vente.
--
-- Migration ADDITIVE : aucune donnée existante n'est modifiée.
-- Les nouvelles colonnes/tables sont nullable ou vides au début ;
-- elles seront remplies par le code Java et par un script de backfill.
-- ============================================================

-- ============================================================
-- 1. AJOUT DE quantite_restante SUR achats
-- ============================================================
ALTER TABLE achats
ADD COLUMN quantite_restante DOUBLE NULL
COMMENT 'Quantité restante du lot pour FIFO (décrémentée à chaque vente). NULL = pas encore traité par le backfill.';

-- Index composite pour accélérer les requêtes FIFO :
-- "trouver tous les achats d'un produit pour un tenant, triés par date,
--  avec quantité restante > 0"
CREATE INDEX idx_achat_fifo_lookup
ON achats(tenant_id, nom_produit, date_achat);

-- ============================================================
-- 2. NOUVELLE TABLE vente_lot_consommation
-- ============================================================
CREATE TABLE IF NOT EXISTS vente_lot_consommation (
    id                            BIGINT AUTO_INCREMENT PRIMARY KEY,

    vente_id                      VARCHAR(36) NOT NULL
        COMMENT 'FK vers la vente concernée',
    achat_id                      VARCHAR(36) NOT NULL
        COMMENT 'FK vers le lot d''achat puisé',
    tenant_id                     BIGINT NOT NULL
        COMMENT 'FK vers le tenant (multi-tenant)',

    quantite_consommee            DOUBLE NOT NULL
        COMMENT 'Nombre d''unités consommées dans ce lot pour cette vente',

    prix_achat_unitaire_snapshot  DECIMAL(10,2) NOT NULL
        COMMENT 'Prix d''achat unitaire du lot, figé au moment de la vente',
    prix_vente_unitaire_snapshot  DECIMAL(10,2) NOT NULL
        COMMENT 'Prix de vente unitaire au moment de la vente',
    benefice_unitaire             DECIMAL(10,2) NOT NULL
        COMMENT 'prix_vente - prix_achat (snapshot)',
    benefice_total_ligne          DECIMAL(12,2) NOT NULL
        COMMENT 'quantite_consommee * benefice_unitaire (dénormalisé pour SUM rapide)',

    date_vente_snapshot           DATETIME NOT NULL
        COMMENT 'Date de la vente, dupliquée pour filtres par période sans jointure',

    -- Audit (hérité de BaseEntity / AbstractAuditable)
    version                       BIGINT NULL,
    created_date                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date            DATETIME NULL,
    created_by                    VARCHAR(50) NULL,
    last_modified_by              VARCHAR(50) NULL,

    CONSTRAINT fk_vlc_vente
        FOREIGN KEY (vente_id)  REFERENCES ventes(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_vlc_achat
        FOREIGN KEY (achat_id)  REFERENCES achats(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_vlc_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Lignes FIFO : pour chaque vente, dans quels lots elle a puisé.';

-- Index pour les requêtes courantes
CREATE INDEX idx_vlc_vente       ON vente_lot_consommation(vente_id);
CREATE INDEX idx_vlc_achat       ON vente_lot_consommation(achat_id);
CREATE INDEX idx_vlc_tenant      ON vente_lot_consommation(tenant_id);
CREATE INDEX idx_vlc_date_vente  ON vente_lot_consommation(date_vente_snapshot);

-- Index composite pour les agrégats par tenant + période (cas le plus fréquent)
CREATE INDEX idx_vlc_tenant_date ON vente_lot_consommation(tenant_id, date_vente_snapshot);
