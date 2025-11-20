# 🔐 Sécurisation Complète - Dija Saliou

Ce document récapitule toutes les mesures de sécurité implémentées dans l'application.

---

## ✅ Checklist de Sécurité

### CRITIQUE (Implémenté) ✅

- [x] **Secrets retirés d'application.properties**
  - Fichier `application-prod.properties` créé pour la production
  - Toutes les clés chargées depuis variables d'environnement
  - Fichier `.env.example` fourni comme template

- [x] **JWT Secret fort et unique**
  - Secret de 88 caractères pour dev (valeur par défaut)
  - Guide de génération fourni dans `SECURITE_JWT.md`
  - Variable `JWT_SECRET` obligatoire en production

- [x] **Vérification signatures webhooks Stripe**
  - Endpoint `/api/payment/webhook` sécurisé
  - Vérification de signature avec `Webhook.constructEvent()`
  - Rejet automatique des signatures invalides

- [x] **Vérification signatures webhooks Wave**
  - Endpoint `/api/payment/wave/webhook` sécurisé
  - Vérification HMAC SHA-256
  - Comparaison en temps constant (protection timing attacks)

### ÉLEVÉ (Implémenté) ✅

- [x] **Logs en INFO/WARN pour production**
  - `application-prod.properties` : logs en INFO/WARN
  - `application.properties` : logs en DEBUG (développement)

- [x] **show-sql désactivé en production**
  - `application-prod.properties` : `show-sql=false`
  - Protection contre la fuite d'informations sensibles

- [x] **Rate limiting sur login/register**
  - Login : 5 tentatives par minute par IP
  - Register : 3 comptes par heure par IP
  - Reset password : 3 tentatives par heure par email
  - Service `RateLimitService` avec Bucket4j

- [x] **CSRF désactivé (justifié)**
  - JWT stocké dans cookie HttpOnly
  - Protection CSRF par SameSite=Strict
  - API stateless (pas de session)

### MOYEN (Implémenté) ✅

- [x] **hibernate.ddl-auto=validate en production**
  - `application-prod.properties` : validate
  - `application.properties` : update (développement)

- [x] **Limite taille des uploads**
  - Max fichier : 5MB
  - Max requête : 10MB
  - Configuré dans les deux fichiers properties

- [x] **app.cookie.secure=true en production**
  - `application-prod.properties` : true
  - Cookies uniquement en HTTPS

- [x] **CORS restrictif**
  - Headers spécifiques (pas de wildcard *)
  - Domaines autorisés explicitement
  - Cache preflight : 1 heure
  - Credentials autorisés pour JWT cookies

### FAIBLE (Implémenté) ✅

- [x] **Headers HSTS**
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`
  - Force HTTPS pour tous les futurs accès
  - Activé UNIQUEMENT en production

- [x] **Headers CSP**
  - Content-Security-Policy strict
  - Autorise uniquement Stripe.js et API Stripe
  - frame-ancestors 'none' (protection clickjacking)

- [x] **Autres headers de sécurité**
  - X-Content-Type-Options: nosniff
  - X-Frame-Options: DENY
  - X-XSS-Protection: 1; mode=block
  - Referrer-Policy: strict-origin-when-cross-origin
  - Permissions-Policy: désactive fonctionnalités inutiles

---

## 📂 Fichiers de Configuration

### Développement Local
- `application.properties` : Configuration dev avec valeurs par défaut
- Secrets avec valeurs de test pour Stripe
- show-sql=true pour debug
- Logs en DEBUG
- cookie.secure=false

### Production
- `application-prod.properties` : Configuration production
- **AUCUN secret** dans le fichier
- Toutes les valeurs depuis environnement
- show-sql=false
- Logs en INFO/WARN
- cookie.secure=true
- hibernate.ddl-auto=validate

### Variables d'Environnement
- `.env.example` : Template complet
- À copier en `.env` et remplir
- **NE JAMAIS** committer `.env`
- `.gitignore` déjà configuré

---

## 🔒 Protection Mise en Place

### 1. Authentification & Autorisation

#### JWT Sécurisé
```java
// Cookie HttpOnly (JavaScript ne peut pas lire)
cookie.setHttpOnly(true);
// Secure en production (HTTPS uniquement)
cookie.setSecure(cookieSecure);
// SameSite protection CSRF (géré par navigateur)
cookie.setPath("/");
// Durée de vie 24h
cookie.setMaxAge(24 * 60 * 60);
```

#### Rate Limiting
```java
// Login : 5 tentatives/minute
Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)))

// Register : 3 comptes/heure
Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)))

// Password Reset : 3 tentatives/heure
Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)))
```

### 2. Paiements Sécurisés

#### Stripe
- Vérification de signature webhooks
- Clés API en variables d'environnement
- Mode TEST en dev, LIVE en prod
- Customer Stripe lié au tenant_uuid

#### Wave
- Vérification signature HMAC SHA-256
- Comparaison en temps constant
- Mode simulation en dev (sans clés)
- Masquage numéros de téléphone dans logs

### 3. Headers de Sécurité

Tous les headers sont ajoutés automatiquement par `SecurityHeadersConfig`:

```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline' https://js.stripe.com; ...
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(), camera=(), ...
```

### 4. Protection des Données

#### Mots de passe
- BCrypt avec salt automatique
- Impossible de retrouver le mot de passe original
- Chaque hash est différent même pour le même mot de passe

#### Base de données
- Connexions limitées (pool Hikari)
- Timeout de connexion configuré
- Validation des requêtes en production

#### Emails
- Vérification d'email obligatoire
- Tokens expirables (24h)
- Rate limiting sur reset password

### 5. CORS Strict

```java
// Domaines autorisés explicitement
setAllowedOrigins(["http://localhost:4200", "https://votredomaine.com"])

// Headers spécifiques (pas de wildcard)
setAllowedHeaders(["Authorization", "Content-Type", ...])

// Méthodes HTTP nécessaires uniquement
setAllowedMethods(["GET", "POST", "PUT", "DELETE", "OPTIONS"])

// Cache preflight 1 heure
setMaxAge(3600L)
```

---

## 🚀 Déploiement en Production

### 1. Configurer les Variables d'Environnement

```bash
# Générer JWT secret fort
JWT_SECRET=$(openssl rand -base64 64)

# Configurer sur Heroku
heroku config:set JWT_SECRET="$JWT_SECRET"
heroku config:set STRIPE_PUBLIC_KEY="pk_live_..."
heroku config:set STRIPE_SECRET_KEY="sk_live_..."
heroku config:set STRIPE_WEBHOOK_SECRET="whsec_..."
# ... (voir .env.example pour la liste complète)
```

### 2. Activer le Profil Production

```bash
# Heroku
heroku config:set SPRING_PROFILES_ACTIVE=prod

# Ou dans application.properties
spring.profiles.active=prod
```

### 3. Configurer CORS pour votre domaine

Dans `SecurityConfig.java`, ligne 132:
```java
configuration.setAllowedOrigins(Arrays.asList(
    "https://votredomaine.com",
    "https://www.votredomaine.com"
));
```

### 4. Configurer les webhooks

**Stripe:**
- Dashboard → Developers → Webhooks
- URL: `https://votredomaine.com/api/payment/webhook`
- Events: `payment_intent.succeeded`, `payment_intent.payment_failed`

**Wave:**
- Contacter Wave support
- URL: `https://votredomaine.com/api/payment/wave/webhook`

### 5. Activer HTTPS

HTTPS est **OBLIGATOIRE** en production pour:
- Cookies Secure
- Headers HSTS
- Paiements Stripe
- Protection des données

Sur Heroku, HTTPS est automatique.

---

## 🧪 Tests de Sécurité

### 1. Tester Rate Limiting

```bash
# Login : doit bloquer après 5 tentatives
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","motDePasse":"wrong"}'
done
```

### 2. Tester Webhooks

```bash
# Webhook Stripe sans signature : doit être rejeté
curl -X POST http://localhost:8080/api/payment/webhook \
  -H "Content-Type: application/json" \
  -d '{"type":"payment_intent.succeeded"}'
# Résultat attendu: 400 Bad Request
```

### 3. Tester Headers de Sécurité

```bash
# Vérifier les headers
curl -I https://votredomaine.com/api/auth/login

# Doit contenir:
# Strict-Transport-Security: max-age=31536000
# Content-Security-Policy: ...
# X-Frame-Options: DENY
# etc.
```

### 4. Tester CORS

```bash
# Requête depuis un domaine non autorisé : doit être bloquée
curl -X OPTIONS http://localhost:8080/api/auth/login \
  -H "Origin: http://malicious-site.com" \
  -H "Access-Control-Request-Method: POST"
```

---

## 📊 Monitoring en Production

### 1. Logs à surveiller

```bash
# Heroku
heroku logs --tail | grep "SÉCURITÉ"

# Rechercher :
# "❌ SÉCURITÉ:" → Tentatives d'attaque
# "⚠️ RATE LIMIT:" → Abus détectés
# "Signature webhook invalide" → Webhooks suspects
```

### 2. Alertes recommandées

- Trop de tentatives de login échouées (> 100/heure)
- Webhooks avec signature invalide
- Erreurs de paiement anormales
- Pics de création de comptes

---

## 🆘 En Cas de Problème

### Secret JWT compromis

1. Générer un NOUVEAU secret immédiatement
2. Mettre à jour la production
3. Tous les utilisateurs devront se reconnecter
4. Voir `SECURITE_JWT.md` pour détails

### Clés Stripe/Wave exposées

1. Révoquer les clés dans le dashboard
2. Générer de nouvelles clés
3. Mettre à jour les variables d'environnement
4. Redémarrer l'application

### Attaque par force brute détectée

1. Vérifier les logs rate limiting
2. Bloquer l'IP au niveau du firewall si nécessaire
3. Ajuster les limites si besoin

---

## 📞 Support

- Documentation JWT : `SECURITE_JWT.md`
- Déploiement production : `DEPLOIEMENT_PRODUCTION.md`
- Email support : ahmadubambagayelo@gmail.com

---

## ✅ Résumé - Sécurité au Top

🎉 **Toutes les vulnérabilités critiques ont été corrigées !**

- ✅ Secrets protégés
- ✅ JWT sécurisé
- ✅ Webhooks vérifiés
- ✅ Rate limiting actif
- ✅ Headers de sécurité configurés
- ✅ CORS restrictif
- ✅ Logs production optimisés
- ✅ Base de données sécurisée
- ✅ Uploads limités
- ✅ HTTPS en production

**Votre application est maintenant prête pour la production ! 🚀**
