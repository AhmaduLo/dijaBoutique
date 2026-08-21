-- Produits archivés (en rupture de stock depuis 30+ jours)

CREATE TABLE IF NOT EXISTS produits_archives (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    nom_produit VARCHAR(100) NOT NULL,
    date_rupture DATETIME NOT NULL,
    date_archivage DATETIME NULL,
    CONSTRAINT fk_archive_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uk_archive_tenant_produit UNIQUE (tenant_id, nom_produit)
);

CREATE INDEX idx_archive_tenant ON produits_archives(tenant_id);
CREATE INDEX idx_archive_nom ON produits_archives(nom_produit);
