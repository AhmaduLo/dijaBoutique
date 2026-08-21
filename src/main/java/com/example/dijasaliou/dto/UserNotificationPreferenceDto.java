package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.UserNotificationType;

/**
 * État d'une préférence de notification (côté utilisateur admin d'un tenant)
 * pour affichage frontend.
 *
 * Miroir de {@link NotificationPreferenceDto} qui est réservé aux super admins.
 */
public record UserNotificationPreferenceDto(
        String type,           // ex "STOCK_BAS"
        String libelle,
        String description,
        String categorie,
        boolean enabled,
        boolean defautActif,
        String config          // JSON sérialisé, peut être null
) {
    public static UserNotificationPreferenceDto from(UserNotificationType t, boolean enabled, String config) {
        return new UserNotificationPreferenceDto(
                t.name(),
                t.getLibelle(),
                t.getDescription(),
                t.getCategorie().name(),
                enabled,
                t.isDefautActif(),
                config
        );
    }
}
