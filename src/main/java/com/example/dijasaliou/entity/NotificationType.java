package com.example.dijasaliou.entity;

/**
 * Types de notifications super admin.
 * Chaque type est associé à un libellé + une catégorie pour l'affichage UI.
 *
 * IMPORTANT : ne jamais renommer un nom d'enum (utilisé comme clé en BDD).
 * On peut ajouter de nouveaux types ; pour en retirer un, vider la table
 * de préférences correspondante d'abord.
 */
public enum NotificationType {

    // ─── 💰 REVENUS ──────────────────────────────────────────────────────
    PAIEMENT_RECU(
            "💰 Paiement reçu",
            "Quand un paiement (manuel ou en ligne) est validé",
            Categorie.REVENUS, true),

    CONVERSION_ESSAI(
            "⭐ Conversion essai → payant",
            "Un compte en essai gratuit a souscrit à un plan payant",
            Categorie.REVENUS, true),

    UPGRADE_PLAN(
            "⬆️ Upgrade de plan",
            "Un client passe à un plan supérieur (STARTER → PRO, PRO → BUSINESS)",
            Categorie.REVENUS, true),

    PAIEMENT_ANNUEL(
            "💎 Paiement annuel reçu",
            "Un client paie son abonnement à l'année (gros revenu)",
            Categorie.REVENUS, true),

    // ─── ⚠️ RISQUES ──────────────────────────────────────────────────────
    ABONNEMENT_EXPIRE_3J(
            "⚠️ Abonnement expire dans 3 jours",
            "Alerte préventive avant expiration",
            Categorie.RISQUES, true),

    // ─── 🆕 CROISSANCE (optionnel, désactivé par défaut) ─────────────────
    NOUVEAU_CLIENT(
            "🆕 Nouveau client inscrit",
            "Un nouveau tenant vient de créer un compte",
            Categorie.CROISSANCE, false),

    // ─── 🛠️ TECHNIQUE ────────────────────────────────────────────────────
    ERREUR_WEBHOOK_PAIEMENT(
            "🚨 Erreur webhook paiement",
            "Wave ou PayDunya a échoué à confirmer un paiement (argent qui n'arrive pas)",
            Categorie.TECHNIQUE, true),

    STOCKAGE_BDD_PLEIN(
            "💾 Stockage BDD > 80%",
            "La base de données Railway approche de la limite",
            Categorie.TECHNIQUE, true),

    PIC_ERREURS_SERVEUR(
            "🔥 Pic d'erreurs serveur",
            "Plus de 10 erreurs serveur dans la dernière heure",
            Categorie.TECHNIQUE, true);

    public enum Categorie {
        REVENUS, RISQUES, CROISSANCE, TECHNIQUE
    }

    private final String libelle;
    private final String description;
    private final Categorie categorie;
    private final boolean defautActif;

    NotificationType(String libelle, String description, Categorie categorie, boolean defautActif) {
        this.libelle = libelle;
        this.description = description;
        this.categorie = categorie;
        this.defautActif = defautActif;
    }

    public String getLibelle() { return libelle; }
    public String getDescription() { return description; }
    public Categorie getCategorie() { return categorie; }
    public boolean isDefautActif() { return defautActif; }
}
