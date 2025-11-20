# 🚀 Guide de Déploiement en Production - Dija Saliou

## ⚠️ IMPORTANT : Passer des paiements TEST aux paiements RÉELS

Ce guide explique comment déployer l'application avec de **vrais paiements** (Stripe et Wave).

---

## 📋 Prérequis

### 1. Compte Stripe Production
- ✅ Compte Stripe vérifié et activé
- ✅ Informations bancaires configurées pour recevoir les paiements
- ✅ Passer du mode TEST au mode LIVE

### 2. Compte Marchand Wave (pour clients Sénégalais)
- ✅ Compte Wave Business
- ✅ Contrat d'API signé avec Wave
- ✅ Accès aux clés API de production

### 3. Serveur de production
- ✅ Serveur avec Java 17+ (Heroku, AWS, DigitalOcean, etc.)
- ✅ Base de données MySQL en production
- ✅ Nom de domaine avec HTTPS (obligatoire pour les paiements)

---

## 🔑 ÉTAPE 1 : Obtenir les clés Stripe de PRODUCTION

### A. Se connecter à Stripe Dashboard
1. Allez sur https://dashboard.stripe.com/
2. **Désactivez le mode Test** (toggle en haut à droite)
3. Vous êtes maintenant en mode **LIVE** (production)

### B. Récupérer les clés API
1. Menu **Developers** → **API keys**
2. Copiez :
   - **Publishable key** (commence par `pk_live_...`)
   - **Secret key** (commence par `sk_live_...`) ⚠️ NE JAMAIS partager

### C. Configurer le webhook
1. Menu **Developers** → **Webhooks**
2. Cliquez sur **Add endpoint**
3. **Endpoint URL** : `https://votredomaine.com/api/payment/webhook`
4. **Events to send** :
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
5. Cliquez sur **Add endpoint**
6. Copiez le **Signing secret** (commence par `whsec_...`)

---

## 🇸🇳 ÉTAPE 2 : Obtenir les clés Wave de PRODUCTION

### A. Contacter Wave Sénégal
- **Site web** : https://www.wave.com/sn/
- **Email** : business@wave.com
- **Téléphone** : +221 XX XXX XX XX (à obtenir sur leur site)

### B. Demander l'accès API
Vous devez fournir :
- Informations de votre entreprise
- NINEA (numéro d'identification fiscale)
- Documents d'enregistrement de l'entreprise
- Volume de transactions estimé

### C. Recevoir les clés
Wave vous fournira :
- **API Key** (clé publique)
- **API Secret** (clé secrète)
- **Documentation API** complète

---

## 🖥️ ÉTAPE 3 : Déployer sur Heroku (Recommandé)

### A. Installer Heroku CLI
```bash
# Windows
# Télécharger depuis: https://devcenter.heroku.com/articles/heroku-cli

# macOS
brew tap heroku/brew && brew install heroku

# Linux
curl https://cli-assets.heroku.com/install.sh | sh
```

### B. Se connecter et créer l'application
```bash
# Se connecter à Heroku
heroku login

# Créer une nouvelle application
heroku create dija-saliou-prod

# Ajouter une base de données MySQL (JawsDB)
heroku addons:create jawsdb:kitefin
```

### C. Configurer les variables d'environnement
```bash
# Stripe PRODUCTION
heroku config:set STRIPE_PUBLIC_KEY=pk_live_VOTRE_CLE_PUBLIQUE
heroku config:set STRIPE_SECRET_KEY=sk_live_VOTRE_CLE_SECRETE
heroku config:set STRIPE_WEBHOOK_SECRET=whsec_VOTRE_WEBHOOK_SECRET

# Wave PRODUCTION
heroku config:set WAVE_API_KEY=VOTRE_CLE_WAVE
heroku config:set WAVE_API_SECRET=VOTRE_SECRET_WAVE
heroku config:set WAVE_API_URL=https://api.wave.com/v1
heroku config:set WAVE_WEBHOOK_URL=https://dija-saliou-prod.herokuapp.com/api/payment/wave/webhook

# Email (Gmail App Password recommandé)
heroku config:set MAIL_USERNAME=votre-email@gmail.com
heroku config:set MAIL_PASSWORD=votre-mot-de-passe-app

# URL du frontend
heroku config:set APP_FRONTEND_URL=https://votre-frontend.com
```

### D. Déployer l'application
```bash
# Ajouter Heroku remote
heroku git:remote -a dija-saliou-prod

# Déployer
git push heroku main

# Vérifier les logs
heroku logs --tail
```

---

## 🔧 ÉTAPE 4 : Configuration IntelliJ pour développement

**Pour continuer à développer en local avec les clés de TEST** :

1. Ouvrir IntelliJ IDEA
2. Menu **Run** → **Edit Configurations**
3. Sélectionner votre configuration Spring Boot
4. Onglet **Environment variables**
5. Ajouter :
   ```
   STRIPE_PUBLIC_KEY=pk_test_VOTRE_CLE_TEST
   STRIPE_SECRET_KEY=sk_test_VOTRE_CLE_TEST
   STRIPE_WEBHOOK_SECRET=whsec_VOTRE_WEBHOOK_TEST
   WAVE_API_KEY=
   WAVE_API_SECRET=
   ```

Ainsi, vous pouvez développer avec les clés TEST en local, et déployer avec les clés PRODUCTION sur le serveur.

---

## ✅ ÉTAPE 5 : Vérifier que les paiements sont RÉELS

### A. Test Stripe en production
1. Utilisez une **vraie carte bancaire** (la vôtre pour tester)
2. Effectuez un paiement de test
3. Vérifiez dans **Stripe Dashboard** → **Payments** (mode LIVE)
4. Vous devriez voir le paiement apparaître
5. ⚠️ **Remboursez le paiement de test** dans Stripe Dashboard

### B. Test Wave en production
1. Utilisez un **vrai compte Wave** (le vôtre)
2. Entrez votre numéro de téléphone Wave
3. Confirmez le paiement sur votre téléphone
4. Vérifiez que l'abonnement est activé
5. Vérifiez dans **Wave Dashboard** que le paiement est enregistré

---

## 🔒 Sécurité en Production

### ✅ Checklist de sécurité

- [ ] Clés Stripe/Wave **JAMAIS** dans le code source
- [ ] Clés chargées uniquement depuis **variables d'environnement**
- [ ] `app.cookie.secure=true` en production (cookies HTTPS uniquement)
- [ ] `spring.jpa.hibernate.ddl-auto=validate` (pas `update`)
- [ ] Logs en niveau `INFO` (pas `DEBUG`)
- [ ] HTTPS activé sur le serveur
- [ ] CORS configuré uniquement pour votre domaine
- [ ] Webhooks Stripe/Wave avec vérification de signature

### ⚠️ AVANT de push sur GitHub

```bash
# Vérifier qu'aucune clé n'est dans le code
git diff

# Si des clés apparaissent, les retirer et recommitter
git add .
git commit -m "fix: Remove API keys from code"
```

---

## 📊 Monitoring en Production

### Logs Heroku
```bash
# Voir les logs en temps réel
heroku logs --tail

# Voir les erreurs uniquement
heroku logs --tail | grep ERROR
```

### Stripe Dashboard
- **Payments** : Voir tous les paiements en temps réel
- **Disputes** : Gérer les litiges
- **Subscriptions** : Suivre les abonnements
- **Webhooks** : Vérifier que les webhooks arrivent bien

### Wave Dashboard
- **Transactions** : Voir tous les paiements Wave
- **Rapports** : Générer des rapports financiers
- **API Logs** : Vérifier les appels API

---

## 🆘 Résolution de problèmes

### Erreur : "No API key provided"
**Cause** : Variables d'environnement non configurées
**Solution** :
```bash
heroku config:set STRIPE_SECRET_KEY=sk_live_...
```

### Erreur : "Webhook signature verification failed"
**Cause** : Mauvais webhook secret
**Solution** : Vérifier que `STRIPE_WEBHOOK_SECRET` correspond au secret dans Stripe Dashboard

### Wave : "Payment failed"
**Cause** : Clés API Wave invalides ou compte non activé
**Solution** : Contacter Wave support pour vérifier l'activation du compte

### Paiements en TEST au lieu de PRODUCTION
**Cause** : Mode TEST toujours activé dans Stripe Dashboard
**Solution** : Désactiver le toggle "Test mode" en haut à droite

---

## 💰 Recevoir les paiements

### Stripe
- Les paiements sont **automatiquement** virés sur votre compte bancaire
- Délai : **7 jours** après le premier paiement (configurable)
- Puis **quotidiennement** ou **hebdomadairement**

### Wave
- Les paiements arrivent sur votre **compte Wave Business**
- Retrait possible :
  - Vers un compte bancaire sénégalais
  - Vers Orange Money / Free Money
  - En espèces dans une agence Wave

---

## 📞 Support

### Stripe Support
- Dashboard → Help → Contact Support
- https://support.stripe.com/

### Wave Support
- Email : business@wave.com
- Téléphone : Vérifier sur wave.com/sn

### Support Application
- Email : ahmadubambagayelo@gmail.com

---

## ✅ Résumé - Checklist Déploiement

- [ ] Obtenir clés Stripe LIVE
- [ ] Obtenir clés Wave Production
- [ ] Retirer toutes les clés du code source
- [ ] Créer application Heroku
- [ ] Configurer variables d'environnement
- [ ] Déployer l'application
- [ ] Configurer webhooks Stripe et Wave
- [ ] Tester avec un vrai paiement
- [ ] Vérifier réception du paiement
- [ ] Rembourser le paiement de test
- [ ] Activer monitoring et alertes

🎉 **Votre application est maintenant prête à accepter de vrais paiements !**
