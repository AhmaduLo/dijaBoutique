-- ============================================================
-- V28 : Préférences de notifications super admin
-- ============================================================
-- Permet à chaque super admin de cocher / décocher les types de notifications
-- qu'il souhaite recevoir (sur son téléphone via Web Push).
--
-- Si aucun enregistrement n'existe pour un (user, type) donné → on applique
-- la valeur par défaut définie côté Java dans NotificationType.

CREATE TABLE IF NOT EXISTS super_admin_notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type_notification VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_notif_pref_user_type UNIQUE (user_id, type_notification),
    CONSTRAINT fk_notif_pref_user FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    INDEX idx_notif_pref_user (user_id)
);
