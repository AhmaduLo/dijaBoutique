-- Lien d'action optionnel sur une notification in-app (super admin -> tenant).
-- Permet d'afficher un bouton cliquable (ex: "/notifications") en plus du
-- texte, au lieu d'un simple message informatif sans action possible.
--
-- Ecrit de maniere idempotente (verifie l'etat courant avant d'agir) : du
-- DDL MySQL n'est pas transactionnel, une premiere execution partiellement
-- echouee (ou une colonne deja ajoutee via Hibernate ddl-auto en dev) ne
-- doit pas bloquer un second passage.
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'lien_action'
);
SET @add_col_sql = IF(@col_exists = 0,
    'ALTER TABLE notifications ADD COLUMN lien_action VARCHAR(200) NULL',
    'DO 0');
PREPARE stmt FROM @add_col_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
