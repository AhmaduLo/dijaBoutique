-- Chaque boutique (tenant) stocke sa propre devise préférée
-- La table devises reste un catalogue global — seule la préférence est par boutique
ALTER TABLE tenants ADD COLUMN devise_preferee VARCHAR(10) DEFAULT 'XOF';
