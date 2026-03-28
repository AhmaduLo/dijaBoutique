-- V11 : Ajout des index manquants pour améliorer les performances des requêtes fréquentes
-- Identifiés lors de l'audit de scalabilité du 2026-03-28

-- audit_logs : aucun index — chaque requête par tenant ou date fait un full scan
ALTER TABLE audit_logs
    ADD INDEX idx_auditlog_tenant (tenant_id),
    ADD INDEX idx_auditlog_date (date_action);

-- ventes : index composite tenant+date pour les rapports par période
ALTER TABLE ventes
    ADD INDEX idx_vente_tenant_date (tenant_id, date_vente);

-- achats : idem
ALTER TABLE achats
    ADD INDEX idx_achat_tenant_date (tenant_id, date_achat);

-- paiements_credit : index sur la date pour les rapports de remboursements
ALTER TABLE paiements_credit
    ADD INDEX idx_paiement_date (date_paiement);

-- credits_clients : index composite tenant+statut pour les filtres fréquents
ALTER TABLE credits_clients
    ADD INDEX idx_credit_tenant_statut (tenant_id, statut);
