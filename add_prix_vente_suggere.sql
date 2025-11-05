-- Script SQL pour ajouter la colonne prix_vente_suggere si elle n'existe pas
-- Exécutez ce script dans votre client MySQL (MySQL Workbench, phpMyAdmin, etc.)

USE dijaSaliou;

-- Vérifier si la colonne existe déjà
SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'dijaSaliou'
  AND TABLE_NAME = 'achats'
  AND COLUMN_NAME = 'prix_vente_suggere';

-- Si la colonne n'existe pas, décommentez et exécutez la ligne suivante :
-- ALTER TABLE achats ADD COLUMN prix_vente_suggere DECIMAL(10,2) NULL;

-- Vérifier la structure de la table
DESCRIBE achats;
