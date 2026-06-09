-- Étendre les essais BUSINESS en cours de 14 jours à 30 jours.
-- Concerne uniquement les tenants encore en période d'essai (essai_utilise = false)
-- et non supprimés. La nouvelle date_expiration est calculée à partir de date_debut_essai
-- pour garantir la cohérence avec essaiGratuitValide() qui calcule désormais
-- date_debut_essai + 30 jours côté code (constante TenantEntity.DUREE_ESSAI_JOURS).

UPDATE tenants
SET date_expiration = DATE_ADD(date_debut_essai, INTERVAL 30 DAY)
WHERE essai_utilise = FALSE
  AND deleted = FALSE
  AND date_debut_essai IS NOT NULL
  AND plan = 'BUSINESS'
  AND date_expiration > NOW();
