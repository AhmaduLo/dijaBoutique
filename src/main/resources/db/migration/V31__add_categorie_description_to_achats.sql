-- ============================================================
-- V31 : Ajout des champs categorie + description aux achats
-- ============================================================
-- categorie : auparavant @Transient (non stockée), désormais stockée
--             pour pouvoir filtrer/regrouper les achats par catégorie.
-- description : nouveau champ libre (1 ligne, max 200 chars) pour
--               noter des détails sur l'achat (lot, conditions, etc.).
-- Les 2 champs sont optionnels.

ALTER TABLE achats
    ADD COLUMN categorie   VARCHAR(100) NULL,
    ADD COLUMN description VARCHAR(200) NULL;
