-- ============================================================
-- V36 : Notifications utilisateur (admins des tenants)
-- ============================================================
-- Deux tables : subscriptions Web Push + préférences par type.
--
-- Les super admins ont leurs propres tables (V26, V30). Ici c'est spécifique
-- aux admins des boutiques (tenants), avec isolation multi-tenant explicite.
--
-- Si aucune préférence n'existe pour un (user, type) → on applique le défaut
-- défini côté Java dans UserNotificationType.

-- ─── Table 1 : subscriptions Web Push (un par appareil) ──────────────────
CREATE TABLE IF NOT EXISTS user_push_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth_key VARCHAR(255) NOT NULL,
    user_agent VARCHAR(255),
    date_creation DATETIME NOT NULL,
    derniere_utilisation DATETIME,
    CONSTRAINT uk_user_push_endpoint UNIQUE (endpoint(255)),
    CONSTRAINT fk_user_push_user FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_push_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    INDEX idx_user_push_user (user_id),
    INDEX idx_user_push_tenant (tenant_id)
);

-- ─── Table 2 : préférences par (user, type) ──────────────────────────────
CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    type_notification VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    config TEXT,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_user_notif_pref_user_type UNIQUE (user_id, type_notification),
    CONSTRAINT fk_user_notif_pref_user FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_notif_pref_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    INDEX idx_user_notif_pref_user (user_id),
    INDEX idx_user_notif_pref_tenant (tenant_id)
);
