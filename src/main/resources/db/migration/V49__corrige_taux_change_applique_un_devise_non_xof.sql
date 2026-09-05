-- CORRECTIF (second bug distinct de V45/V48) : ventes/achats/depenses avec
-- devise_code correct (ex: EUR) mais taux_change_applique fige a 1.000000 au
-- lieu du vrai taux de la devise (ex: 655.957 pour EUR).
--
-- taux_change_applique est un INSTANTANE du taux de change au moment de la
-- transaction (cf. VenteService#creerVente : vente.setTauxChangeApplique(
-- devise.getTauxChange())) — volontairement decouple de la ligne `devises`
-- courante pour ne jamais reecrire l'histoire si le taux change plus tard.
-- Consequence : si la ligne `devises` d'une boutique a ete initialement mal
-- reglee (taux = 1 au lieu du vrai taux) puis corrigee ensuite, les
-- transactions creees AVANT la correction restent figees sur l'ancien taux 1,
-- meme si `devises.taux_change` affiche desormais la bonne valeur.
--
-- Tout calcul qui pivote par la devise de reference (rapports, historique de
-- caisse, benefice FIFO — cf. VenteService, ligne ~980) multiplie alors le
-- montant par 1 au lieu du vrai taux, puis divise par le vrai taux pour
-- l'affichage -> un montant de 20 EUR redescend a 20 / 655.957 = 0,03 EUR.
--
-- Un taux_change_applique = 1.000000 sur une ligne dont devise_code N'EST
-- PAS 'XOF' n'est jamais une valeur legitime (aucune devise du systeme n'est
-- pegee 1:1 au XOF, cf. DeviseEntity : EUR = 655.957, USD = 600.0...) : ce
-- WHERE cible donc precisement les lignes corrompues, sans risque de toucher
-- une transaction reelle et volontairement a taux 1.
--
-- Le taux de reference utilise pour corriger est celui de LA DEVISE DE LA
-- LIGNE elle-meme (v.devise_code), pas necessairement celle du tenant
-- aujourd'hui (qui a pu changer depuis) — resolu avec la meme priorite que
-- DeviseService : devise personnalisee du tenant pour ce code en premier,
-- sinon devise systeme du meme code.
--
-- Portee (constatee sur prod) : credits_clients / paiements_credit /
-- mouvement_caisse_manuel / transfert_caisse ne sont pas concernes par ce
-- bug-ci (aucune ligne ne matche) — inclus quand meme par precaution/
-- symetrie avec V45, sans effet si aucune ligne ne correspond.

UPDATE achats a
JOIN tenants t ON t.id = a.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = a.devise_code AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = a.devise_code AND d_sys.tenant_id IS NULL
SET a.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE a.devise_code <> 'XOF'
  AND a.taux_change_applique = 1.000000
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) <> 1.000000;

UPDATE ventes v
JOIN tenants t ON t.id = v.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = v.devise_code AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = v.devise_code AND d_sys.tenant_id IS NULL
SET v.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE v.devise_code <> 'XOF'
  AND v.taux_change_applique = 1.000000
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) <> 1.000000;

UPDATE depenses dep
JOIN tenants t ON t.id = dep.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = dep.devise_code AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = dep.devise_code AND d_sys.tenant_id IS NULL
SET dep.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE dep.devise_code <> 'XOF'
  AND dep.taux_change_applique = 1.000000
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) <> 1.000000;

UPDATE credits_clients c
JOIN tenants t ON t.id = c.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = c.devise_code AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = c.devise_code AND d_sys.tenant_id IS NULL
SET c.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE c.devise_code <> 'XOF'
  AND c.taux_change_applique = 1.000000
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) <> 1.000000;

UPDATE paiements_credit p
JOIN credits_clients c ON c.id = p.credit_id
JOIN tenants t ON t.id = c.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = p.devise_code AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = p.devise_code AND d_sys.tenant_id IS NULL
SET p.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE p.devise_code <> 'XOF'
  AND p.taux_change_applique = 1.000000
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) <> 1.000000;

UPDATE mouvement_caisse_manuel m
JOIN tenants t ON t.id = m.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = m.devise_code AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = m.devise_code AND d_sys.tenant_id IS NULL
SET m.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE m.devise_code <> 'XOF'
  AND m.taux_change_applique = 1.000000
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) <> 1.000000;

UPDATE transfert_caisse tc
JOIN tenants t ON t.id = tc.tenant_id
LEFT JOIN devises d_custom ON d_custom.code = tc.devise_code AND d_custom.tenant_id = t.id
LEFT JOIN devises d_sys ON d_sys.code = tc.devise_code AND d_sys.tenant_id IS NULL
SET tc.taux_change_applique = COALESCE(d_custom.taux_change, d_sys.taux_change)
WHERE tc.devise_code <> 'XOF'
  AND tc.taux_change_applique = 1.000000
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) IS NOT NULL
  AND COALESCE(d_custom.taux_change, d_sys.taux_change) <> 1.000000;
