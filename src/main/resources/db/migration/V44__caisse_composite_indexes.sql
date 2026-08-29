-- ============================================================
-- MIGRATION V26 : Indexes composites pour le calcul du solde caisse
-- ============================================================
-- Le calcul du solde caisse fait des SUM avec WHERE filtré sur :
--   (tenant_id, mode_paiement, date_xxx BETWEEN ...)
-- Sans index composite, MySQL fait un full table scan dès quelques
-- dizaines de milliers de lignes. Cette migration ajoute les indexes
-- qui réduisent ces queries de O(N) à O(log N).
-- ============================================================

-- ── Ventes ──
CREATE INDEX idx_vente_tenant_mode_date
ON ventes(tenant_id, mode_paiement, date_vente);

-- ── Achats ──
CREATE INDEX idx_achat_tenant_mode_date
ON achats(tenant_id, mode_paiement, date_achat);

-- ── Dépenses ──
CREATE INDEX idx_depense_tenant_mode_date
ON depenses(tenant_id, mode_paiement, date_depense);

-- ── Paiements crédit (filtre via credit.tenant) ──
-- Index sur (mode_paiement, date_paiement) suffit, le tenant est résolu
-- via le JOIN sur credits_clients
CREATE INDEX idx_paiement_credit_mode_date
ON paiements_credit(mode_paiement, date_paiement);

-- ── Transferts caisse (entrants/sortants par compte) ──
CREATE INDEX idx_transfert_tenant_source_date
ON transfert_caisse(tenant_id, compte_source, date_transfert);

CREATE INDEX idx_transfert_tenant_destination_date
ON transfert_caisse(tenant_id, compte_destination, date_transfert);

-- ── Mouvements manuels (entrée/sortie par compte) ──
CREATE INDEX idx_mouvement_tenant_compte_type_date
ON mouvement_caisse_manuel(tenant_id, compte, type_mouvement, date_mouvement);
