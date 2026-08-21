-- Notifications envoyées par le super admin

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    objet VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    tenant_id BIGINT NULL,
    filtre_plan VARCHAR(20),
    canal_email BOOLEAN NOT NULL DEFAULT FALSE,
    canal_app BOOLEAN NOT NULL DEFAULT TRUE,
    canal_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    nb_destinataires INTEGER NOT NULL DEFAULT 0,
    date_envoi TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    envoye_par VARCHAR(100),
    CONSTRAINT fk_notif_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_notif_tenant ON notifications(tenant_id);
CREATE INDEX idx_notif_date ON notifications(date_envoi);

-- Trace de lecture des notifications par tenant

CREATE TABLE IF NOT EXISTS notifications_lues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    date_lecture TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_lue_notif FOREIGN KEY (notification_id) REFERENCES notifications(id),
    CONSTRAINT fk_notif_lue_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uk_notif_tenant UNIQUE (notification_id, tenant_id)
);

CREATE INDEX idx_notif_lue_tenant ON notifications_lues(tenant_id);
CREATE INDEX idx_notif_lue_notif ON notifications_lues(notification_id);
