# 🔧 Configuration Locale - Développement

Ce fichier explique comment configurer l'application pour le développement local.

## ⚠️ IMPORTANT : Variables d'Environnement Obligatoires

Depuis que les secrets ont été retirés du code pour la sécurité, vous devez configurer les variables d'environnement localement.

---

## 🚀 Configuration IntelliJ IDEA (Recommandée)

### 1. Ouvrir la Configuration

1. Menu **Run** → **Edit Configurations...**
2. Sélectionner votre configuration Spring Boot (DijaSaliouApplication)
3. Onglet **Environment variables**

### 2. Ajouter les Variables

Cliquer sur l'icône 📁 à côté de "Environment variables" et ajouter :

```
STRIPE_PUBLIC_KEY=<votre_cle_publique_stripe_ici>
STRIPE_SECRET_KEY=<votre_cle_secrete_stripe_ici>
STRIPE_WEBHOOK_SECRET=<votre_webhook_secret_stripe_ici>
```

**Note** : Utilisez vos vraies clés Stripe que vous avez reçues. Ne les partagez jamais publiquement.

### 3. Appliquer et Redémarrer

- Cliquer sur **Apply** puis **OK**
- Redémarrer l'application

---

## 📄 Alternative : Fichier .env (Non Committé)

### 1. Créer le Fichier .env

À la racine du projet :

```bash
touch .env
```

### 2. Ajouter les Variables

Copier `.env.example` en `.env` et remplir avec vos vraies clés :

```bash
cp .env.example .env
# Puis éditer .env avec vos vraies clés Stripe
```

### 3. Vérifier que .env est Ignoré

```bash
git status
# .env ne doit PAS apparaître (il est dans .gitignore)
```

### 4. Charger les Variables (avec plugin)

Installer le plugin **EnvFile** dans IntelliJ :
1. **Settings** → **Plugins**
2. Chercher "EnvFile"
3. Installer et redémarrer
4. **Run** → **Edit Configurations** → **EnvFile** → Ajouter `.env`

---

## 🎯 Variables Disponibles

| Variable | Description | Requis? |
|----------|-------------|---------|
| `STRIPE_PUBLIC_KEY` | Clé publique Stripe (TEST) | ✅ Oui |
| `STRIPE_SECRET_KEY` | Clé secrète Stripe (TEST) | ✅ Oui |
| `STRIPE_WEBHOOK_SECRET` | Secret webhook Stripe | ✅ Oui |
| `WAVE_API_KEY` | Clé API Wave (optionnel) | ❌ Non (vide = simulation) |
| `WAVE_API_SECRET` | Secret API Wave (optionnel) | ❌ Non (vide = simulation) |

**Note** : Obtenez vos clés Stripe sur https://dashboard.stripe.com/apikeys

---

## ✅ Vérification

Après configuration, au démarrage de l'application, vous devez voir :

```
✅ Stripe initialisé avec succès
```

Si vous voyez une erreur, c'est que les variables ne sont pas chargées correctement.

---

## 🔐 Sécurité

- ✅ `.env` est dans `.gitignore` → Ne sera jamais commité
- ✅ Utiliser des clés de TEST en développement (pk_test_... et sk_test_...)
- ❌ Ne JAMAIS committer de clés (ni TEST ni LIVE) dans le code
- ❌ Ne JAMAIS utiliser de clés LIVE (pk_live_... ou sk_live_...) en développement

---

## 📞 Support

En cas de problème :
1. Vérifier que les variables sont bien configurées dans IntelliJ
2. Redémarrer l'application
3. Vérifier les logs au démarrage

Email : ahmadubambagayelo@gmail.com
