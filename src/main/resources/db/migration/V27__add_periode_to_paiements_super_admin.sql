-- ============================================================
-- V27 : Ajout de la période (MENSUEL / ANNUEL) sur les paiements super admin
-- ============================================================
-- Permet de tracer si un paiement couvre 1 mois ou 12 mois (avec -15%).
-- Tous les paiements existants sont par défaut MENSUEL (rétrocompatible).

ALTER TABLE paiements_super_admin
    ADD COLUMN periode VARCHAR(10) NOT NULL DEFAULT 'MENSUEL';

-- Index utile si plus tard on veut filtrer les revenus par type de période
CREATE INDEX idx_psa_periode ON paiements_super_admin (periode);
