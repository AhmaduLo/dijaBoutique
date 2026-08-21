-- ============================================================
-- V33 : Crédits passés en perte (créances irrécouvrables)
-- ============================================================
-- Quand un client ne paiera plus son crédit, le commerçant clique
-- "Passer en perte". Le reste dû devient une perte FIFO de catégorie
-- CREDIT_IMPAYE dans les stats bénéfice — sans toucher aux paiements
-- déjà reçus (qui restent dans le CA/caisse).
--
-- - Ajoute la valeur PERTE à l'enum statut (déjà géré côté Hibernate via @Enumerated STRING)
-- - Ajoute date_passage_en_perte : pour attribuer la perte à la bonne période
-- ============================================================

ALTER TABLE credits_clients
    ADD COLUMN date_passage_en_perte DATE NULL;

-- Index pour accélérer les agrégats par période sur les stats bénéfice
CREATE INDEX idx_credit_date_perte ON credits_clients(date_passage_en_perte);
