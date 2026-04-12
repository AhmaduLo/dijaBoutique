-- Table des catégories de produits — gérée par le super admin

CREATE TABLE IF NOT EXISTS categories_reference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    ordre INTEGER NOT NULL DEFAULT 0
);

-- Catégories par défaut adaptées au commerce sénégalais
INSERT IGNORE INTO categories_reference (nom, ordre) VALUES
    ('Alimentaire', 1),
    ('Boissons', 2),
    ('Cosmétiques & Hygiène', 3),
    ('Électronique', 4),
    ('Vêtements & Accessoires', 5),
    ('Ménager', 6),
    ('Quincaillerie', 7),
    ('Téléphonie & Recharge', 8),
    ('Papeterie & Bureau', 9),
    ('Santé', 10),
    ('Autre', 99);
