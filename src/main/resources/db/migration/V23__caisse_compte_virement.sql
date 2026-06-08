-- ============================================================
-- MIGRATION V23 : Ajout du 4e compte caisse VIREMENT (banque)
-- ============================================================
-- Le compte VIREMENT devient un compte de caisse à part entière
-- (avec solde initial, transferts, mouvements manuels et impact
-- ventes/achats/dépenses).
-- ============================================================

-- ── 1. Solde initial Virement sur caisse_config ─────────────────
ALTER TABLE caisse_config
ADD COLUMN solde_initial_virement DECIMAL(15,2) NOT NULL DEFAULT 0
AFTER solde_initial_om;

-- ── 2. Ajouter VIREMENT à l'ENUM transfert_caisse ───────────────
ALTER TABLE transfert_caisse
MODIFY COLUMN compte_source      ENUM('ESPECES','WAVE','ORANGE_MONEY','VIREMENT') NOT NULL,
MODIFY COLUMN compte_destination ENUM('ESPECES','WAVE','ORANGE_MONEY','VIREMENT') NOT NULL;

-- ── 3. Ajouter VIREMENT à l'ENUM mouvement_caisse_manuel ────────
ALTER TABLE mouvement_caisse_manuel
MODIFY COLUMN compte ENUM('ESPECES','WAVE','ORANGE_MONEY','VIREMENT') NOT NULL;

-- ── 4. Ajouter VIREMENT à l'ENUM ventes.mode_paiement ───────────
-- (achats et depenses ont déjà VIREMENT depuis V22)
ALTER TABLE ventes
MODIFY COLUMN mode_paiement ENUM('ESPECES','WAVE','ORANGE_MONEY','VIREMENT','CREDIT') NULL;
