package com.example.dijasaliou.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Converter JPA pour {@link UserNotificationType}.
 *
 * Contrairement à {@code @Enumerated(EnumType.STRING)} qui plante avec
 * IllegalArgumentException quand la base contient une valeur qui n'existe
 * plus dans l'enum Java (typiquement après suppression d'un type), ce
 * converter retourne {@code null} et laisse la couche service filtrer.
 *
 * Impact : plus jamais de crash au chargement d'une préférence orpheline
 * (le service doit filtrer les {@code type == null} avant utilisation).
 * Les rangées orphelines restent en base — c'est le rôle d'une migration
 * de les nettoyer proprement.
 */
@Converter(autoApply = false)
@Slf4j
public class UserNotificationTypeConverter implements AttributeConverter<UserNotificationType, String> {

    @Override
    public String convertToDatabaseColumn(UserNotificationType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public UserNotificationType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return UserNotificationType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.warn("[USER_NOTIF] Type inconnu en base ignoré : '{}' (probablement supprimé de l'enum)", dbData);
            return null;
        }
    }
}
