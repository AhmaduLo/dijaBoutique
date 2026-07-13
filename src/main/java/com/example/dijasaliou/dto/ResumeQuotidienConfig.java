package com.example.dijasaliou.dto;

/**
 * Configuration utilisateur pour le type de notification RESUME_QUOTIDIEN.
 *
 * Schéma JSON persisté dans UserNotificationPreference.config :
 *   { "heure": 20 }
 *
 * L'heure est interprétée dans le fuseau horaire du tenant (défaut Africa/Dakar).
 * Valeurs valides : 0 à 23. Par défaut : 20h.
 */
public record ResumeQuotidienConfig(int heure) {

    private static final int DEFAULT_HEURE = 20;

    public ResumeQuotidienConfig {
        if (heure < 0 || heure > 23) {
            throw new IllegalArgumentException("L'heure doit être entre 0 et 23 (reçu : " + heure + ")");
        }
    }

    public static ResumeQuotidienConfig defaults() {
        return new ResumeQuotidienConfig(DEFAULT_HEURE);
    }
}
