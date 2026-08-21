package com.example.dijasaliou.dto;

import java.time.DayOfWeek;

/**
 * Config utilisateur pour RESUME_HEBDO.
 *
 * Schéma JSON : { "jour": "MONDAY", "heure": 8 }
 *
 * Le résumé est envoyé chaque semaine à ce jour + cette heure locales du tenant.
 * Défaut : lundi 8h. Le résumé porte sur la semaine écoulée (7 derniers jours
 * précédant l'envoi).
 */
public record ResumeHebdoConfig(DayOfWeek jour, int heure) {

    private static final DayOfWeek DEFAULT_JOUR = DayOfWeek.MONDAY;
    private static final int DEFAULT_HEURE = 8;

    public ResumeHebdoConfig {
        if (jour == null) jour = DEFAULT_JOUR;
        if (heure < 0 || heure > 23) {
            throw new IllegalArgumentException("L'heure doit être entre 0 et 23 (reçu : " + heure + ")");
        }
    }

    public static ResumeHebdoConfig defaults() {
        return new ResumeHebdoConfig(DEFAULT_JOUR, DEFAULT_HEURE);
    }
}
