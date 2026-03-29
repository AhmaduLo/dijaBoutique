-- V12 : Table des paiements validés manuellement par le super admin
-- Contexte : les clients paient via WhatsApp, le super admin enregistre ici.
CREATE TABLE paiements_super_admin (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id    BIGINT         NOT NULL,
    plan         VARCHAR(20)    NOT NULL,
    montant      DECIMAL(10, 2) NOT NULL,
    date_paiement DATETIME      NOT NULL,
    mois_debut   VARCHAR(7)     NULL COMMENT 'Format YYYY-MM (ex: 2026-04)',
    mode_paiement VARCHAR(50)   NULL,
    note         TEXT           NULL,
    valide_par   BIGINT         NULL COMMENT 'ID du super admin ayant validé',
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_psa_tenant      (tenant_id),
    INDEX idx_psa_date        (date_paiement),
    INDEX idx_psa_mois        (mois_debut)
);
