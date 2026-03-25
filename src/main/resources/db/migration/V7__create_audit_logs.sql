CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT,
    auteur VARCHAR(150),
    date_action DATETIME NOT NULL DEFAULT NOW(),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);
