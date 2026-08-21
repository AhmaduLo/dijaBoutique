package com.example.dijasaliou.dto;

import com.example.dijasaliou.entity.NotificationType;

/**
 * État d'une préférence de notification pour affichage frontend.
 */
public record NotificationPreferenceDto(
        String type,           // ex "PAIEMENT_RECU"
        String libelle,
        String description,
        String categorie,
        boolean enabled,
        boolean defautActif
) {
    public static NotificationPreferenceDto from(NotificationType t, boolean enabled) {
        return new NotificationPreferenceDto(
                t.name(),
                t.getLibelle(),
                t.getDescription(),
                t.getCategorie().name(),
                enabled,
                t.isDefautActif()
        );
    }
}
