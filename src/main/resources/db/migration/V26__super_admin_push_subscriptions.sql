-- ============================================================
-- V26 : Table des subscriptions Push notifications (super admin)
-- ============================================================
-- Stocke les endpoints push web des super admins (un par appareil).
-- Permet d'envoyer des notifications natives au téléphone :
--   - nouveau tenant inscrit
--   - abonnement d'un tenant qui expire bientôt

CREATE TABLE IF NOT EXISTS super_admin_push_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth_key VARCHAR(255) NOT NULL,
    user_agent VARCHAR(255),
    date_creation DATETIME NOT NULL,
    derniere_utilisation DATETIME,
    CONSTRAINT uk_super_admin_push_endpoint UNIQUE (endpoint(255)),
    CONSTRAINT fk_super_admin_push_user FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    INDEX idx_super_admin_push_user (user_id)
);
