-- CORRECTIF (suite de l'incident corrige par V45 ; initialement prevu en V48,
-- renumerote V51 car jamais deploye et V49/V50 deja passees) : bug d'affichage en
-- "centimes" pour toute boutique dont la devise n'est PAS le XOF.
--
-- V45 a restaure taux_change_applique = 0 -> 1.000000 en supposant
-- devise_code = 'XOF' pour TOUTES les lignes anterieures au deploiement du
-- multi-devises (achats, ventes, depenses, credits_clients, paiements_credit,
-- mouvement_caisse_manuel, transfert_caisse). C'est correct pour les boutiques
-- dont la devise preferee (tenants.devise_preferee, V15 - existe bien avant le
-- multi-devises) est XOF, mais FAUX pour toute boutique qui operait deja dans
-- une autre devise (ex: EUR) : pour elle, le montant brut ("10", "15"...)
-- representait deja des unites de SA devise (10 EUR), pas des XOF.
--
-- Consequence du mauvais tag : tout affichage qui reconvertit le montant vers
-- la devise de la boutique (montant / taux_change_de_sa_devise, cf.
-- DeviseEntity#convertirDepuisReference) divise un montant deja correct par
-- ~656 (taux EUR/XOF) -> une vente/un achat de 10 EUR ou 15 EUR d'origine
-- s'affiche comme quelques centimes (0,01 - 0,04 EUR).
--
-- Ce correctif cible PRECISEMENT les lignes touchees par V45 : devise_code =
-- 'XOF' ET taux_change_applique = 1.000000 ET created_date anterieure a
-- l'execution de V45 (horodatage recupere depuis flyway_schema_history, donc
-- exact quel que soit l'environnement) ET dont la boutique proprietaire a une
-- devise_preferee differente de XOF. Une transaction multi-devises reelle et
-- volontairement en XOF, creee APRES V45, n'est jamais touchee grace au
-- filtre sur created_date.
--
-- La devise/le taux appliques sont resolus avec la meme priorite que
-- DeviseService/DeviseRepository#findByCodeVisiblePourTenant : la devise
-- personnalisee de la boutique pour ce code si elle en a fork une (jointure
-- d_custom), sinon la devise systeme du meme code (jointure d_sys). Si aucune
-- des deux n'existe (devise_preferee invalide), la ligne est laissee
-- inchangee plutot que de risquer un taux errone.
--
-- paiements_credit n'a pas de tenant_id direct : la boutique se retrouve via
-- son credit (credits_clients.tenant_id).

SET @v45_installed_on = (
    SELECT installed_on FROM flyway_schema_history WHERE version = '45' LIMIT 1
);

UPDATE achats a
JOIN tenants t ON t.id = a.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = t.devise_preferee AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = t.devise_preferee AND d_sys.tenant_id IS NULL
SET a.devise_code = t.devise_preferee,
    a.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE a.devise_code = 'XOF'
  AND a.taux_change_applique = 1.000000
  AND a.created_date < @v45_installed_on
  AND t.devise_preferee IS NOT NULL AND t.devise_preferee <> 'XOF'
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL;

UPDATE ventes v
JOIN tenants t ON t.id = v.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = t.devise_preferee AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = t.devise_preferee AND d_sys.tenant_id IS NULL
SET v.devise_code = t.devise_preferee,
    v.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE v.devise_code = 'XOF'
  AND v.taux_change_applique = 1.000000
  AND v.created_date < @v45_installed_on
  AND t.devise_preferee IS NOT NULL AND t.devise_preferee <> 'XOF'
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL;

UPDATE depenses dep
JOIN tenants t ON t.id = dep.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = t.devise_preferee AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = t.devise_preferee AND d_sys.tenant_id IS NULL
SET dep.devise_code = t.devise_preferee,
    dep.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE dep.devise_code = 'XOF'
  AND dep.taux_change_applique = 1.000000
  AND dep.created_date < @v45_installed_on
  AND t.devise_preferee IS NOT NULL AND t.devise_preferee <> 'XOF'
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL;

UPDATE credits_clients c
JOIN tenants t ON t.id = c.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = t.devise_preferee AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = t.devise_preferee AND d_sys.tenant_id IS NULL
SET c.devise_code = t.devise_preferee,
    c.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE c.devise_code = 'XOF'
  AND c.taux_change_applique = 1.000000
  AND c.created_date < @v45_installed_on
  AND t.devise_preferee IS NOT NULL AND t.devise_preferee <> 'XOF'
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL;

UPDATE paiements_credit p
JOIN credits_clients c ON c.id = p.credit_id
JOIN tenants t ON t.id = c.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = t.devise_preferee AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = t.devise_preferee AND d_sys.tenant_id IS NULL
SET p.devise_code = t.devise_preferee,
    p.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE p.devise_code = 'XOF'
  AND p.taux_change_applique = 1.000000
  AND p.created_date < @v45_installed_on
  AND t.devise_preferee IS NOT NULL AND t.devise_preferee <> 'XOF'
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL;

UPDATE mouvement_caisse_manuel m
JOIN tenants t ON t.id = m.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = t.devise_preferee AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = t.devise_preferee AND d_sys.tenant_id IS NULL
SET m.devise_code = t.devise_preferee,
    m.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE m.devise_code = 'XOF'
  AND m.taux_change_applique = 1.000000
  AND m.created_date < @v45_installed_on
  AND t.devise_preferee IS NOT NULL AND t.devise_preferee <> 'XOF'
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL;

UPDATE transfert_caisse tc
JOIN tenants t ON t.id = tc.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = t.devise_preferee AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = t.devise_preferee AND d_sys.tenant_id IS NULL
SET tc.devise_code = t.devise_preferee,
    tc.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE tc.devise_code = 'XOF'
  AND tc.taux_change_applique = 1.000000
  AND tc.created_date < @v45_installed_on
  AND t.devise_preferee IS NOT NULL AND t.devise_preferee <> 'XOF'
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL;
