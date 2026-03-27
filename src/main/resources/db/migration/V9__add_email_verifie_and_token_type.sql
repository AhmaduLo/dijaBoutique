-- Ajouter la colonne email_verifie sur utilisateurs
ALTER TABLE utilisateurs
    ADD COLUMN IF NOT EXISTS email_verifie BIT(1) NOT NULL DEFAULT b'0';

-- Ajouter la colonne type sur password_reset_tokens
-- pour distinguer RESET_PASSWORD et EMAIL_VERIFICATION
ALTER TABLE password_reset_tokens
    ADD COLUMN IF NOT EXISTS type VARCHAR(30) NOT NULL DEFAULT 'RESET_PASSWORD';
