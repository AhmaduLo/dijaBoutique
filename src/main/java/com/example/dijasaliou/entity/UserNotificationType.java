package com.example.dijasaliou.entity;

/**
 * Types de notifications utilisateur (admin d'un tenant).
 *
 * Distinct de {@link NotificationType} qui est réservé aux super admins de la plateforme.
 *
 * IMPORTANT : ne jamais renommer un nom d'enum (utilisé comme clé en BDD).
 * On peut ajouter de nouveaux types ; pour en retirer un, vider la table
 * de préférences correspondante d'abord.
 *
 * NOTE Phase 1 : seuls STOCK_BAS et RUPTURE sont branchés à un émetteur.
 * Les autres types sont exposés dans l'UI de préférences mais n'ont pas
 * encore de déclencheur — ils seront branchés en Phase 2.
 */
public enum UserNotificationType {

    // ─── 📦 STOCK ────────────────────────────────────────────────────────
    STOCK_BAS(
            "📉 Stock faible",
            "Un produit atteint un seuil de stock bas (15, 10 ou 5 unités).",
            Categorie.STOCK, true),

    RUPTURE(
            "🛑 Rupture de stock",
            "Un produit tombe à 0 unité — plus vendable.",
            Categorie.STOCK, true),

    // ─── 💵 VENTES ───────────────────────────────────────────────────────
    VENTE_EMPLOYE(
            "🧑‍💼 Vente par un employé",
            "Un vendeur ou gérant vient d'enregistrer une vente.",
            Categorie.VENTES, false),

    VENTE_A_PERTE(
            "⚠️ Vente à perte détectée",
            "Une vente vient d'être enregistrée à un prix inférieur au coût d'achat moyen.",
            Categorie.VENTES, true),

    // ─── 🏦 CAISSE ───────────────────────────────────────────────────────
    SORTIE_CAISSE_IMPORTANTE(
            "💸 Sortie de caisse importante",
            "Un retrait ou une dépense en caisse supérieur à ton seuil.",
            Categorie.CAISSE, false),

    TRANSACTION_MANUELLE(
            "🔁 Transaction manuelle en caisse",
            "Dépôt, retrait ou transfert manuel entre comptes de caisse.",
            Categorie.CAISSE, false),

    // ─── 💳 CRÉDITS ──────────────────────────────────────────────────────
    NOUVEAU_CREDIT(
            "🆕 Nouveau crédit accordé",
            "Un crédit vient d'être accordé à un client.",
            Categorie.CREDITS, false),

    CREDIT_REMBOURSE(
            "✅ Crédit remboursé",
            "Un client vient de solder son crédit.",
            Categorie.CREDITS, false),

    CREDIT_EN_RETARD(
            "⏰ Crédit en retard",
            "Un crédit client a dépassé sa date d'échéance.",
            Categorie.CREDITS, true),

    // ─── 💼 DÉPENSES ─────────────────────────────────────────────────────
    DEPENSE_EMPLOYE(
            "🧾 Dépense saisie par un employé",
            "Un gérant ou vendeur a enregistré une dépense.",
            Categorie.DEPENSES, false),

    // ─── 📊 RÉSUMÉS ──────────────────────────────────────────────────────
    RESUME_QUOTIDIEN(
            "📊 Résumé quotidien",
            "Synthèse de la journée : CA, ventes, bénéfice.",
            Categorie.RESUMES, true),

    RESUME_HEBDO(
            "📈 Résumé hebdomadaire",
            "Synthèse de la semaine écoulée avec comparaison à la précédente.",
            Categorie.RESUMES, false),

    RESUME_MENSUEL(
            "📅 Résumé mensuel",
            "Bilan du mois avec top produits et top dépenses.",
            Categorie.RESUMES, false),

    // ─── 💎 ABONNEMENT ───────────────────────────────────────────────────
    ABONNEMENT_EXPIRE_7J(
            "⏳ Abonnement expire dans 7 jours",
            "Rappel préventif avant l'expiration.",
            Categorie.ABONNEMENT, true),

    ABONNEMENT_EXPIRE_3J(
            "⚠️ Abonnement expire dans 3 jours",
            "Rappel urgent avant l'expiration.",
            Categorie.ABONNEMENT, true),

    ABONNEMENT_EXPIRE_1J(
            "🚨 Abonnement expire demain",
            "Dernier rappel avant blocage.",
            Categorie.ABONNEMENT, true),

    // ─── 🎯 OBJECTIFS ────────────────────────────────────────────────────
    OBJECTIF_JOURNALIER(
            "🎯 Objectif journalier atteint",
            "Ton objectif de chiffre d'affaires du jour vient d'être atteint.",
            Categorie.OBJECTIFS, false);

    public enum Categorie {
        STOCK, VENTES, CAISSE, CREDITS, DEPENSES, RESUMES, ABONNEMENT, OBJECTIFS
    }

    private final String libelle;
    private final String description;
    private final Categorie categorie;
    private final boolean defautActif;

    UserNotificationType(String libelle, String description, Categorie categorie, boolean defautActif) {
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
