package com.example.dijasaliou.dto;

/**
 * Config utilisateur pour RESUME_MENSUEL.
 *
 * Schéma JSON : { "jourDuMois": 1, "heure": 8 }
 *
 * Le résumé est envoyé chaque mois à ce jour + cette heure locales du tenant.
 * Défaut : le 1er à 8h. Le résumé porte sur le mois calendaire écoulé.
 *
 * jourDuMois est borné à 28 pour éviter les cas particuliers de février.
 */
public record ResumeMensuelConfig(int jourDuMois, int heure) {

    private static final int DEFAULT_JOUR_DU_MOIS = 1;
    private static final int DEFAULT_HEURE = 8;
    private static final int MAX_JOUR = 28;

    public ResumeMensuelConfig {
        if (jourDuMois < 1 || jourDuMois > MAX_JOUR) {
            throw new IllegalArgumentException(
                    "Le jour du mois doit être entre 1 et " + MAX_JOUR + " (reçu : " + jourDuMois + ")");
        }
        if (heure < 0 || heure > 23) {
            throw new IllegalArgumentException("L'heure doit être entre 0 et 23 (reçu : " + heure + ")");
        }
    }

    public static ResumeMensuelConfig defaults() {
        return new ResumeMensuelConfig(DEFAULT_JOUR_DU_MOIS, DEFAULT_HEURE);
    }
}
