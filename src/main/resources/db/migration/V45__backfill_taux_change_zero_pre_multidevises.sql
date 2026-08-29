-- CORRECTIF URGENT (incident prod) : les colonnes devise_code/taux_change_applique
-- ont ete ajoutees a achats/ventes/depenses/credits_clients/paiements_credit/
-- mouvement_caisse_manuel/transfert_caisse par Hibernate ddl-auto=update lors du
-- deploiement du multi-devises, plutot que par les migrations V16/V17/V41/V42/V43
-- ci-dessus (Flyway n'avait jamais tourne avant l'activation de ce meme
-- deploiement — voir V44). Consequence : sur les lignes DEJA existantes avant ce
-- deploiement, MySQL a applique son defaut implicite (0 / chaine vide) au lieu du
-- DEFAULT 'XOF' / 1.000000 prevu par ces migrations. Tout calcul qui multiplie un
-- montant par taux_change_applique (historique de caisse, solde, rapports,
-- benefice FIFO) retombait donc a zero pour toute donnee anterieure a ce
-- deploiement.
--
-- taux_change_applique = 0 n'est jamais une valeur legitime (aucun taux de change
-- reel n'est nul) : ce WHERE cible donc precisement les lignes corrompues, sans
-- risque de toucher une vraie transaction multi-devises creee depuis. La colonne
-- montant elle-meme n'a jamais ete touchee par le bug — cette migration ne fait
-- que restaurer le multiplicateur, aucune donnee n'est inventee ni perdue.
--
-- Verifie sur une copie de donnees reelles (corruption reproduite puis corrigee) :
-- restauration exacte des valeurs d'origine.

UPDATE achats
SET devise_code = 'XOF', taux_change_applique = 1.000000
WHERE taux_change_applique = 0;

UPDATE ventes
SET devise_code = 'XOF', taux_change_applique = 1.000000
WHERE taux_change_applique = 0;

UPDATE depenses
SET devise_code = 'XOF', taux_change_applique = 1.000000
WHERE taux_change_applique = 0;

UPDATE credits_clients
SET devise_code = 'XOF', taux_change_applique = 1.000000
WHERE taux_change_applique = 0;

UPDATE paiements_credit
SET devise_code = 'XOF', taux_change_applique = 1.000000
WHERE taux_change_applique = 0;

UPDATE mouvement_caisse_manuel
SET devise_code = 'XOF', taux_change_applique = 1.000000
WHERE taux_change_applique = 0;

UPDATE transfert_caisse
SET devise_code = 'XOF', taux_change_applique = 1.000000
WHERE taux_change_applique = 0;
