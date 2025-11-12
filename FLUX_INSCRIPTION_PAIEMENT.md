# Flux d'Inscription et Paiement

## Principe : Inscription D'ABORD → Paiement ENSUITE

Ce document décrit le flux complet d'inscription et de paiement de l'application.

---

## 📋 Vue d'Ensemble

```
┌─────────────────┐
│   1. INSCRIPTION│  User remplit formulaire → Plan GRATUIT (expiré)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  2. CONNEXION   │  User reçoit JWT et est connecté
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  3. BLOCAGE     │  SubscriptionExpirationFilter bloque l'accès
└────────┬────────┘  (sauf routes /payment/**)
         │
         ▼
┌─────────────────┐
│  4. PAIEMENT    │  User choisit un plan et paie avec Stripe
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  5. ACTIVATION  │  Abonnement activé pour 30 jours
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  6. ACCÈS OK    │  User accède à l'application ✅
└─────────────────┘
```

---

## 🔄 Flux Détaillé

### Étape 1 : Inscription

**Frontend** → `POST /api/auth/register`

```json
{
  "nom": "Gaye",
  "prenom": "Bamba",
  "email": "bamba@example.com",
  "motDePasse": "password123",
  "nomEntreprise": "Boutique Bamba",
  "numeroTelephone": "+221771234567",
  "adresseEntreprise": "Dakar, Sénégal",
  "nineaSiret": "123456789",
  "acceptationCGU": true,
  "acceptationPolitiqueConfidentialite": true
}
```

**Backend** → [AuthService.register()](src/main/java/com/example/dijasaliou/service/AuthService.java)
1. Vérifie que l'email n'existe pas
2. Vérifie l'acceptation des CGU
3. Crée un **TENANT** avec :
   - `plan = GRATUIT`
   - `dateExpiration = now` (expiré immédiatement)
   - `actif = true`
4. Crée un **USER** ADMIN lié au tenant
5. Génère un **JWT** avec `tenant_uuid`
6. Retourne le token

**Résultat :** User a un compte mais ne peut pas accéder à l'application.

---

### Étape 2 : Connexion Automatique

Le frontend reçoit le JWT et authentifie automatiquement l'utilisateur.

**Token JWT contient :**
```json
{
  "email": "bamba@example.com",
  "tenant_uuid": "7d2ec4ac-ea4f-41f1-90ac-ff008945455c",
  "role": "ADMIN"
}
```

---

### Étape 3 : Blocage par le Filtre

**Chaque requête passe par :**
1. **JwtAuthenticationFilter** → Authentifie l'utilisateur
2. **SubscriptionExpirationFilter** → Vérifie l'abonnement

Le `SubscriptionExpirationFilter` vérifie si :
- `plan == GRATUIT` OU
- `dateExpiration < now`

**Si OUI** → Bloque l'accès avec `403 Forbidden` :
```json
{
  "error": "Paiement requis",
  "message": "Veuillez souscrire à un abonnement pour accéder à l'application. Choisissez un plan et effectuez votre paiement.",
  "code": "PAYMENT_REQUIRED"
}
```

**EXCEPTION :** Les routes `/payment/**` sont autorisées pour permettre le paiement.

---

### Étape 4 : Sélection du Plan

**Frontend** → `GET /api/payment/plans`

**Réponse :**
```json
{
  "BASIC": {
    "libelle": "Plan Basic",
    "description": "Gestion complète boutique - 3 utilisateurs",
    "prixEuro": 9.99,
    "prixCFA": 6555,
    "maxUtilisateurs": 3
  },
  "PREMIUM": {
    "libelle": "Plan Premium",
    "description": "Pour moyennes entreprises",
    "prixEuro": 15.24,
    "prixCFA": 10000,
    "maxUtilisateurs": 10
  },
  "ENTREPRISE": {
    "libelle": "Plan Entreprise",
    "description": "Pour grandes entreprises",
    "prixEuro": 22.87,
    "prixCFA": 15000,
    "maxUtilisateurs": 2147483647
  }
}
```

---

### Étape 5 : Création du PaymentIntent

**Frontend** → `POST /api/payment/create-intent` (AVEC JWT)

```json
{
  "plan": "BASIC",
  "devise": "EUR"
}
```

**Backend** → [PaymentController.createPaymentIntent()](src/main/java/com/example/dijasaliou/controller/PaymentController.java)
1. Vérifie que l'utilisateur est **ADMIN** (`@PreAuthorize`)
2. Récupère le **tenant** depuis le contexte JWT
3. Appelle `StripeService.createPaymentIntent(request, tenant)`

**Backend** → [StripeService.createPaymentIntent()](src/main/java/com/example/dijasaliou/service/StripeService.java)
1. Calcule le montant (9.99€ → 999 centimes)
2. **Crée ou récupère un Stripe Customer** lié au tenant :
   - `metadata.tenant_uuid = "7d2ec4ac..."`
   - `metadata.tenant_nom = "Boutique Bamba"`
3. Crée un **PaymentIntent** lié au Customer :
   - `customer = "cus_xxx"`
   - `metadata.tenant_uuid = "7d2ec4ac..."`
   - `metadata.plan = "BASIC"`
4. Retourne le `clientSecret`

**Réponse :**
```json
{
  "clientSecret": "pi_3SRvt2R04vCoCXhR1QhKkzSb_secret_xxx",
  "montant": 999,
  "devise": "eur",
  "plan": "BASIC",
  "message": "PaymentIntent créé avec succès"
}
```

**✅ Avantages de cette approche :**
- **Traçabilité parfaite** : Chaque PaymentIntent est lié à un tenant spécifique
- **Stripe Customer** : Tous les paiements d'un tenant sont regroupés
- **Sécurité** : Seul le propriétaire du compte (ADMIN) peut créer un paiement
- **Historique** : On peut voir tous les paiements d'un tenant dans Stripe Dashboard

---

### Étape 6 : Confirmation du Paiement (Frontend)

Le frontend utilise **Stripe.js** pour afficher le formulaire de paiement et confirmer le paiement côté client.

```javascript
const stripe = Stripe('pk_test_xxx');
const {error} = await stripe.confirmCardPayment(clientSecret, {
  payment_method: {
    card: cardElement,
    billing_details: {name: 'Bamba Gaye'}
  }
});
```

---

### Étape 7 : Activation de l'Abonnement

**Frontend** → `POST /api/payment/success` (AVEC JWT)

```json
{
  "paymentIntentId": "pi_3SRvt2R04vCoCXhR1QhKkzSb",
  "plan": "BASIC"
}
```

**Backend** → [PaymentController.confirmPayment()](src/main/java/com/example/dijasaliou/controller/PaymentController.java)
1. Vérifie que l'utilisateur est **ADMIN**
2. Vérifie le paiement avec Stripe : `stripeService.verifyPaymentIntent(paymentIntentId)`
3. Si le paiement est valide (`status == "succeeded"`) :
   - `tenant.setPlan(BASIC)`
   - `tenant.setDateExpiration(now + 30 jours)`
   - `tenant.setActif(true)`
4. Sauvegarde le tenant

**Réponse :**
```json
{
  "message": "Paiement confirmé ! Votre abonnement Plan Basic est maintenant actif.",
  "plan": "BASIC",
  "dateExpiration": "2025-12-11T18:00:00"
}
```

---

### Étape 8 : Accès à l'Application ✅

L'utilisateur peut maintenant accéder à toutes les fonctionnalités de l'application car :
- `plan = BASIC` (plus GRATUIT)
- `dateExpiration > now`

Le `SubscriptionExpirationFilter` laisse passer toutes les requêtes.

---

## 🔐 Sécurité

### 1. Isolation Multi-Tenant
- Chaque PaymentIntent contient le `tenant_uuid` dans les metadata
- Impossible de payer pour un autre tenant

### 2. Authentification JWT
- Seul un utilisateur **connecté** peut créer un PaymentIntent
- Le JWT contient le `tenant_uuid` → Pas de fraude possible

### 3. Vérification Stripe
- Le backend vérifie le statut du PaymentIntent avec Stripe API
- Impossible de tricher en envoyant un faux `paymentIntentId`

### 4. Stripe Customer
- Un Customer Stripe est créé par tenant
- Tous les paiements sont tracés
- Facilite les renouvellements futurs

---

## 📊 Traçabilité

### Dans la Base de Données
```sql
SELECT
  t.tenant_uuid,
  t.nom_entreprise,
  t.plan,
  t.date_expiration,
  u.email,
  u.nom,
  u.prenom
FROM tenants t
JOIN utilisateurs u ON u.tenant_id = t.id
WHERE t.plan != 'GRATUIT';
```

### Dans Stripe Dashboard
1. Aller sur https://dashboard.stripe.com/customers
2. Chercher par `metadata.tenant_uuid`
3. Voir tous les paiements d'un tenant

---

## 🔄 Renouvellement

Quand l'abonnement expire (`dateExpiration < now`) :

1. Le `SubscriptionExpirationFilter` bloque l'accès
2. L'utilisateur voit un message : "Abonnement expiré"
3. Il retourne sur `/payment` et choisit un plan
4. Il paie → L'abonnement est renouvelé pour 30 jours supplémentaires

**Avantage du Stripe Customer :**
- La carte peut être enregistrée
- Paiements futurs plus rapides
- Possibilité de mettre en place des abonnements récurrents automatiques

---

## 📝 Résumé des Endpoints

| Endpoint | Méthode | Auth | Description |
|----------|---------|------|-------------|
| `/auth/register` | POST | ❌ Non | Créer un compte (plan GRATUIT) |
| `/auth/login` | POST | ❌ Non | Se connecter |
| `/payment/plans` | GET | ❌ Non | Lister les plans disponibles |
| `/payment/config` | GET | ❌ Non | Récupérer la clé publique Stripe |
| `/payment/subscription` | GET | ✅ Oui | Statut de l'abonnement actuel |
| `/payment/create-intent` | POST | ✅ ADMIN | Créer un PaymentIntent |
| `/payment/success` | POST | ✅ ADMIN | Confirmer le paiement et activer |

---

## ✅ Pourquoi Cette Approche Est Meilleure

| Critère | Payer PUIS S'inscrire | **S'inscrire PUIS Payer** |
|---------|----------------------|---------------------------|
| **Traçabilité** | ❌ Impossible | ✅ Parfaite |
| **Qui a payé ?** | ❌ Inconnu | ✅ tenant_uuid dans metadata |
| **Sécurité** | ❌ Partage possible | ✅ JWT obligatoire |
| **Stripe Customer** | ❌ Non | ✅ Oui (un par tenant) |
| **Historique** | ❌ Difficile | ✅ Facile (Stripe Dashboard) |
| **Renouvellement** | ❌ Compliqué | ✅ Simple |
| **Comptabilité** | ❌ Problématique | ✅ Claire |
| **Standard SaaS** | ❌ Non standard | ✅ Standard (Stripe, Shopify, etc.) |

---

## 🎯 Prochaines Étapes

1. **Webhooks Stripe** : Écouter les événements `payment_intent.succeeded` pour une validation en temps réel
2. **Abonnements récurrents** : Utiliser Stripe Subscriptions pour facturer automatiquement chaque mois
3. **Historique des paiements** : Créer une table `payments` pour stocker l'historique
4. **Factures PDF** : Générer des factures automatiquement après chaque paiement
5. **Notifications email** : Envoyer un email de confirmation après paiement réussi
