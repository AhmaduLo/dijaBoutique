-- ============================================================
-- V37 : Retrait du type OBJECTIF_JOURNALIER
-- ============================================================
-- Le type OBJECTIF_JOURNALIER a été supprimé de l'enum UserNotificationType
-- (cf. commit de suppression). On nettoie les preferences deja persistees
-- pour eviter les IllegalArgumentException lors du mapping enum a la lecture.

DELETE FROM user_notification_preferences
WHERE type_notification = 'OBJECTIF_JOURNALIER';
