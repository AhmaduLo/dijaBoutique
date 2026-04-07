-- Configuration globale de la plateforme HeasyStock
-- Table non-tenant : partagée par toutes les boutiques

CREATE TABLE platform_config (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    cle        VARCHAR(50)  NOT NULL UNIQUE,
    valeur     VARCHAR(255),
    updated_at DATETIME(6)
);

-- Valeur par défaut : numéro WhatsApp support
INSERT INTO platform_config (cle, valeur, updated_at)
VALUES ('whatsapp_support', '+33751130937', NOW());
