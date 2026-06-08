-- ============================================================
-- MIGRATION V22 : Module Caisse multi-comptes (BUSINESS only)
-- ============================================================
-- Ajoute le suivi de la trésorerie en temps réel :
--   - Solde initial par compte (Espèces / Wave / OM)
--   - Mode de paiement sur achats et dépenses (déjà sur ventes)
--   - Transferts entre comptes
--   - Mouvements manuels (entrée / sortie)
--
-- Calcul du solde : agrégat en temps réel à partir de la date d'activation.
-- Les anciens achats/ventes/dépenses (avant activation) ne sont PAS pris en
-- compte — la caisse démarre au solde initial saisi par le commerçant.
--
-- Réservé au plan BUSINESS (vérifié dans le controller via @RequiresPlan).
-- ============================================================

-- ── 1. Configuration de la caisse (1 ligne par tenant) ──────────
CREATE TABLE IF NOT EXISTS caisse_config (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT NOT NULL UNIQUE
        COMMENT '1 seule config par tenant',

    solde_initial_especes       DECIMAL(15,2) NOT NULL DEFAULT 0,
    solde_initial_wave          DECIMAL(15,2) NOT NULL DEFAULT 0,
    solde_initial_om            DECIMAL(15,2) NOT NULL DEFAULT 0,

    date_activation             DATETIME NOT NULL
        COMMENT 'Date d''activation — seuls les flux à partir d''ici comptent',
    active_par                  VARCHAR(36)
        COMMENT 'UUID de l''utilisateur qui a activé',

    -- Audit
    version                     BIGINT NULL,
    created_date                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date          DATETIME NULL,
    created_by                  VARCHAR(50) NULL,
    last_modified_by            VARCHAR(50) NULL,

    CONSTRAINT fk_caisse_config_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Configuration de la caisse multi-comptes (1 par tenant).';

-- ── 2. Transferts entre comptes ─────────────────────────────────
CREATE TABLE IF NOT EXISTS transfert_caisse (
    id                          VARCHAR(36) PRIMARY KEY,
    tenant_id                   BIGINT NOT NULL,

    compte_source               ENUM('ESPECES','WAVE','ORANGE_MONEY') NOT NULL,
    compte_destination          ENUM('ESPECES','WAVE','ORANGE_MONEY') NOT NULL,
    montant                     DECIMAL(15,2) NOT NULL,
    motif                       VARCHAR(255),
    date_transfert              DATETIME NOT NULL,

    fait_par                    VARCHAR(36),
    -- Audit
    version                     BIGINT NULL,
    created_date                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date          DATETIME NULL,
    created_by                  VARCHAR(50) NULL,
    last_modified_by            VARCHAR(50) NULL,

    CONSTRAINT fk_transfert_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_transfert_diff CHECK (compte_source <> compte_destination)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Transferts d''argent entre les comptes de la caisse.';

CREATE INDEX idx_transfert_tenant       ON transfert_caisse(tenant_id);
CREATE INDEX idx_transfert_date         ON transfert_caisse(date_transfert);
CREATE INDEX idx_transfert_tenant_date  ON transfert_caisse(tenant_id, date_transfert);

-- ── 3. Mouvements manuels (entrée / sortie) ─────────────────────
CREATE TABLE IF NOT EXISTS mouvement_caisse_manuel (
    id                          VARCHAR(36) PRIMARY KEY,
    tenant_id                   BIGINT NOT NULL,

    type_mouvement              ENUM('ENTREE','SORTIE') NOT NULL,
    compte                      ENUM('ESPECES','WAVE','ORANGE_MONEY') NOT NULL,
    montant                     DECIMAL(15,2) NOT NULL,
    motif                       VARCHAR(255) NOT NULL
        COMMENT 'Raison obligatoire (ex: retrait perso, argent trouvé)',
    date_mouvement              DATETIME NOT NULL,

    fait_par                    VARCHAR(36),
    -- Audit
    version                     BIGINT NULL,
    created_date                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date          DATETIME NULL,
    created_by                  VARCHAR(50) NULL,
    last_modified_by            VARCHAR(50) NULL,

    CONSTRAINT fk_mvt_manuel_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Entrées/sorties manuelles (hors achats/ventes/dépenses).';

CREATE INDEX idx_mvt_manuel_tenant      ON mouvement_caisse_manuel(tenant_id);
CREATE INDEX idx_mvt_manuel_date        ON mouvement_caisse_manuel(date_mouvement);
CREATE INDEX idx_mvt_manuel_tenant_date ON mouvement_caisse_manuel(tenant_id, date_mouvement);

-- ── 4. mode_paiement sur achats ─────────────────────────────────
-- NULL pour les anciens achats (avant activation caisse) → ignorés du calcul
ALTER TABLE achats
ADD COLUMN mode_paiement ENUM('ESPECES','WAVE','ORANGE_MONEY','VIREMENT') NULL
COMMENT 'Mode de paiement (utilisé par la caisse). NULL = ancien achat.';

CREATE INDEX idx_achat_mode_paiement ON achats(mode_paiement);

-- ── 5. mode_paiement sur depenses ───────────────────────────────
ALTER TABLE depenses
ADD COLUMN mode_paiement ENUM('ESPECES','WAVE','ORANGE_MONEY','VIREMENT') NULL
COMMENT 'Mode de paiement (utilisé par la caisse). NULL = ancienne dépense.';

CREATE INDEX idx_depense_mode_paiement ON depenses(mode_paiement);
