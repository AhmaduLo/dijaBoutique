-- ============================================================
-- V32 : Ajout des colonnes pour les sorties hors vente
-- ============================================================
-- Une "sortie hors vente" est un mouvement de stock qui n'est PAS
-- une vente commerciale : perte, vol, casse, don, créance impayée…
--
-- type_sortie  : NULL = vraie vente, sinon = motif de la sortie
-- motif_sortie : description libre optionnelle (ex: "Offert au cousin Mamadou")
--
-- Comportement :
--  - Le stock FIFO sort normalement (la marchandise quitte la boutique)
--  - Le CA et le bénéfice commercial IGNORENT ces lignes
--  - Le coût FIFO est comptabilisé séparément comme "pertes" dans les stats

ALTER TABLE ventes
    ADD COLUMN type_sortie  VARCHAR(30)  NULL,
    ADD COLUMN motif_sortie VARCHAR(255) NULL;

-- Index pour exclure rapidement les sorties des stats globales
CREATE INDEX idx_vente_type_sortie ON ventes(type_sortie);
