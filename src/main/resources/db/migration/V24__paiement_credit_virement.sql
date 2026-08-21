-- ============================================================
-- MIGRATION V24 : VIREMENT comme mode de paiement crédit
-- ============================================================
-- Permet de rembourser un crédit client par virement bancaire,
-- ce qui alimente le compte caisse VIREMENT.
-- ============================================================

ALTER TABLE paiements_credit
MODIFY COLUMN mode_paiement ENUM('ESPECES','WAVE','ORANGE_MONEY','VIREMENT') NOT NULL;
