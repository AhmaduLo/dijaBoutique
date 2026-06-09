-- ============================================================
-- MIGRATION V25 : Timezone par tenant (multi-pays)
-- ============================================================
-- Permet à chaque tenant (entreprise) de définir son fuseau horaire local.
-- Toutes les opérations server-side (dateActivation, dateTransfert,
-- dateMouvement, datePaiement par défaut) seront calculées dans ce fuseau.
--
-- Format : nom IANA (ex: 'Africa/Dakar', 'Europe/Paris', 'Africa/Casablanca')
-- Défaut  : 'Africa/Dakar' (Sénégal, UTC+0) — couvre 100% des tenants actuels.
-- ============================================================

ALTER TABLE tenants
ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Africa/Dakar'
COMMENT 'Fuseau horaire IANA du tenant (ex: Africa/Dakar). Utilisé pour toutes les dates métier.';
