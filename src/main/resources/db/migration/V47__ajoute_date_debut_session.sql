-- Duree de session pour le Super Admin ("connecte depuis X min").
--
-- date_debut_session est distinct de derniere_connexion : ce dernier est
-- ecrase a chaque requete authentifiee (ActivityTrackingFilter), donc
-- l'instant de debut de connexion etait perdu des la premiere activite.
-- date_debut_session n'est remis a zero que lorsque l'ecart depuis
-- derniere_connexion depasse le seuil d'inactivite (30 min) - il represente
-- donc le debut de la session CONTINUE en cours, pas le premier login jamais
-- effectue.
--
-- Ecrit de maniere idempotente (verifie l'etat courant avant d'agir) : du
-- DDL MySQL n'est pas transactionnel, une premiere execution partiellement
-- echouee (ou une colonne deja ajoutee via Hibernate ddl-auto en dev) ne
-- doit pas bloquer un second passage.
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'utilisateurs' AND COLUMN_NAME = 'date_debut_session'
);
SET @add_col_sql = IF(@col_exists = 0,
    'ALTER TABLE utilisateurs ADD COLUMN date_debut_session DATETIME(6) NULL',
    'DO 0');
PREPARE stmt FROM @add_col_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
