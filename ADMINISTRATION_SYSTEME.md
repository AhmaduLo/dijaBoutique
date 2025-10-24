# 🔐 Système d'Administration - Dija Boutique

## 📋 Table des Matières

1. [Vue d'ensemble](#vue-densemble)
2. [Règles de sécurité](#règles-de-sécurité)
3. [Création du compte ADMIN initial](#création-du-compte-admin-initial)
4. [Endpoints ADMIN](#endpoints-admin)
5. [Scénarios d'utilisation](#scénarios-dutilisation)
6. [Tests avec Postman](#tests-avec-postman)
7. [Gestion des erreurs](#gestion-des-erreurs)

---

## 🎯 Vue d'ensemble

### Principe

Dija Boutique utilise un système d'administration centralisé :

- **Un seul compte ADMIN** créé au départ
- **Seul l'ADMIN** peut créer d'autres comptes (employés)
- **L'ADMIN** a tous les droits : création, modification, suppression
- **Les employés (USER)** peuvent accéder à l'application mais **ne peuvent pas** :
  - Modifier leurs informations personnelles
  - Supprimer leur compte
  - Créer d'autres comptes

### Architecture

```
┌─────────────────────────────────────────┐
│         ADMIN (1 seul compte)           │
│  - Créé manuellement (script SQL)       │
│  - Tous les droits                      │
└──────────────────┬──────────────────────┘
                   │ Crée
                   ↓
┌─────────────────────────────────────────┐
│    EMPLOYÉS (USER) - Plusieurs          │
│  - Créés par l'ADMIN                    │
│  - Accès en lecture/écriture aux données│
│  - Pas de modification de compte        │
└─────────────────────────────────────────┘
```

---

## 🔒 Règles de sécurité

### ✅ Ce que l'ADMIN peut faire

| Action | Endpoint | Description |
|--------|----------|-------------|
| Se connecter | `POST /api/auth/login` | Login avec email/password |
| Voir tous les comptes | `GET /api/admin/utilisateurs` | Liste complète des utilisateurs |
| Voir un compte | `GET /api/admin/utilisateurs/{id}` | Détails d'un utilisateur |
| Créer un compte | `POST /api/admin/utilisateurs` | Nouveau compte employé |
| Modifier un compte | `PUT /api/admin/utilisateurs/{id}` | Modifier n'importe quel compte |
| Supprimer un compte | `DELETE /api/admin/utilisateurs/{id}` | Supprimer (sauf le sien) |
| Changer un rôle | `PUT /api/admin/utilisateurs/{id}/role` | Promouvoir USER → ADMIN |
| Voir les stats | `GET /api/admin/statistiques` | Statistiques utilisateurs |

### ✅ Ce que les USER (employés) peuvent faire

| Action | Endpoint | Description |
|--------|----------|-------------|
| Se connecter | `POST /api/auth/login` | Login avec email/password |
| Voir leur profil | `GET /api/utilisateurs/moi` | Leurs propres infos |
| Voir tous les utilisateurs | `GET /api/utilisateurs` | Liste (lecture seule) |
| Utiliser l'application | `/api/achats`, `/api/ventes`, etc. | Toutes les fonctionnalités métier |

### ❌ Ce que les USER NE peuvent PAS faire

- ❌ Créer des comptes
- ❌ Modifier leurs informations (nom, email, password)
- ❌ Supprimer leur compte
- ❌ Accéder aux routes `/api/admin/**`

### ❌ Ce que PERSONNE ne peut faire

- ❌ S'inscrire publiquement (`POST /api/auth/register` est désactivé)
- ❌ L'ADMIN ne peut pas se supprimer lui-même

---

## 🚀 Création du compte ADMIN initial

### Étape 1 : Exécuter le script SQL

Le fichier `create_admin.sql` contient le script pour créer le premier compte ADMIN.

**Depuis MySQL Workbench :**

```sql
-- Ouvrir le fichier create_admin.sql
-- Exécuter le script (⚡ Execute)
```

**Depuis la ligne de commande :**

```bash
mysql -u root -p dija_boutique < create_admin.sql
```

### Étape 2 : Vérifier la création

```sql
SELECT id, nom, prenom, email, role, date_creation
FROM utilisateurs
WHERE email = 'admin@dijaboutique.com';
```

Résultat attendu :

```
+----+-------+-------------+-------------------------+-------+---------------------+
| id | nom   | prenom      | email                   | role  | date_creation       |
+----+-------+-------------+-------------------------+-------+---------------------+
|  1 | Admin | Dija Saliou | admin@dijaboutique.com  | ADMIN | 2025-01-15 10:30:00 |
+----+-------+-------------+-------------------------+-------+---------------------+
```

### Étape 3 : Se connecter

**Endpoint :** `POST http://localhost:8080/api/auth/login`

**Body JSON :**

```json
{
  "email": "admin@dijaboutique.com",
  "motDePasse": "admin123"
}
```

**Réponse :**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbkBkaWphYm91dGlxdWUuY29tIiwiaWF0IjoxNzA1MzIwNjAwLCJleHAiOjE3MDU0MDcwMDB9.abc123...",
  "user": {
    "id": 1,
    "nom": "Admin",
    "prenom": "Dija Saliou",
    "email": "admin@dijaboutique.com",
    "role": "ADMIN",
    "dateCreation": "2025-01-15T10:30:00"
  }
}
```

⚠️ **IMPORTANT : Sauvegardez le token !** Il sera utilisé dans toutes les requêtes suivantes.

### Étape 4 : Changer le mot de passe (RECOMMANDÉ)

**Endpoint :** `PUT http://localhost:8080/api/admin/utilisateurs/1`

**Header :**
```
Authorization: Bearer {votre-token}
```

**Body JSON :**

```json
{
  "motDePasse": "VotreNouveauMotDePasseSecurise2025!"
}
```

---

## 🛠️ Endpoints ADMIN

### 1. Voir tous les utilisateurs

**GET** `/api/admin/utilisateurs`

**Header :**
```
Authorization: Bearer {token-admin}
```

**Réponse :**

```json
[
  {
    "id": 1,
    "nom": "Admin",
    "prenom": "Dija Saliou",
    "email": "admin@dijaboutique.com",
    "role": "ADMIN",
    "dateCreation": "2025-01-15T10:30:00"
  },
  {
    "id": 2,
    "nom": "Diop",
    "prenom": "Mamadou",
    "email": "mamadou@dijaboutique.com",
    "role": "USER",
    "dateCreation": "2025-01-16T14:20:00"
  }
]
```

---

### 2. Créer un compte employé

**POST** `/api/admin/utilisateurs`

**Header :**
```
Authorization: Bearer {token-admin}
```

**Body JSON :**

```json
{
  "nom": "Diop",
  "prenom": "Mamadou",
  "email": "mamadou@dijaboutique.com",
  "motDePasse": "password123",
  "role": "USER"
}
```

**Réponse :**

```json
{
  "id": 2,
  "nom": "Diop",
  "prenom": "Mamadou",
  "email": "mamadou@dijaboutique.com",
  "role": "USER",
  "dateCreation": "2025-01-16T14:20:00"
}
```

**Notes :**
- Si `role` n'est pas spécifié, le rôle par défaut est `USER`
- Le mot de passe sera automatiquement hashé avec BCrypt
- Le champ `createdBy` sera automatiquement rempli avec l'ID de l'ADMIN connecté

---

### 3. Modifier un utilisateur

**PUT** `/api/admin/utilisateurs/{id}`

**Header :**
```
Authorization: Bearer {token-admin}
```

**Body JSON (tous les champs sont optionnels) :**

```json
{
  "nom": "Nouveau Nom",
  "prenom": "Nouveau Prénom",
  "email": "nouveau.email@dijaboutique.com",
  "motDePasse": "nouveau-password"
}
```

**Exemple : Changer uniquement le mot de passe**

```json
{
  "motDePasse": "nouveau-mot-de-passe-123"
}
```

**Réponse :**

```json
{
  "id": 2,
  "nom": "Nouveau Nom",
  "prenom": "Nouveau Prénom",
  "email": "nouveau.email@dijaboutique.com",
  "role": "USER",
  "dateCreation": "2025-01-16T14:20:00"
}
```

---

### 4. Supprimer un utilisateur

**DELETE** `/api/admin/utilisateurs/{id}`

**Header :**
```
Authorization: Bearer {token-admin}
```

**Réponse :**

```json
{
  "message": "Utilisateur supprimé avec succès"
}
```

**⚠️ Restrictions :**
- L'ADMIN ne peut pas supprimer son propre compte
- Réponse d'erreur si tentative :

```json
{
  "message": "Vous ne pouvez pas supprimer votre propre compte"
}
```

---

### 5. Changer le rôle d'un utilisateur

**PUT** `/api/admin/utilisateurs/{id}/role`

**Header :**
```
Authorization: Bearer {token-admin}
```

**Body JSON :**

```json
{
  "role": "ADMIN"
}
```

**Valeurs acceptées :**
- `"USER"` : Utilisateur standard
- `"ADMIN"` : Administrateur

**Réponse :**

```json
{
  "id": 2,
  "nom": "Diop",
  "prenom": "Mamadou",
  "email": "mamadou@dijaboutique.com",
  "role": "ADMIN",
  "dateCreation": "2025-01-16T14:20:00"
}
```

---

### 6. Voir les statistiques

**GET** `/api/admin/statistiques`

**Header :**
```
Authorization: Bearer {token-admin}
```

**Réponse :**

```json
{
  "nombreTotal": 5,
  "nombreAdmins": 1,
  "nombreUsers": 4,
  "nouveauxUtilisateurs7Jours": 2
}
```

---

## 📝 Scénarios d'utilisation

### Scénario 1 : Premier démarrage

1. Exécuter `create_admin.sql` pour créer le compte ADMIN
2. Se connecter avec `admin@dijaboutique.com` / `admin123`
3. Changer le mot de passe ADMIN
4. Créer les comptes employés

### Scénario 2 : Embauche d'un nouvel employé

**Étape 1 : L'ADMIN crée le compte**

```bash
POST /api/admin/utilisateurs
Authorization: Bearer {token-admin}

{
  "nom": "Ndiaye",
  "prenom": "Fatou",
  "email": "fatou@dijaboutique.com",
  "motDePasse": "MotDePasseTemporaire123",
  "role": "USER"
}
```

**Étape 2 : Donner les identifiants à l'employé**

- Email : `fatou@dijaboutique.com`
- Mot de passe temporaire : `MotDePasseTemporaire123`

**Étape 3 : L'employé se connecte**

```bash
POST /api/auth/login

{
  "email": "fatou@dijaboutique.com",
  "motDePasse": "MotDePasseTemporaire123"
}
```

**Note :** L'employé **ne pourra pas** changer son mot de passe lui-même. Seul l'ADMIN peut le faire via :

```bash
PUT /api/admin/utilisateurs/{id}
{
  "motDePasse": "NouveauMotDePasse"
}
```

### Scénario 3 : Départ d'un employé

**L'ADMIN supprime le compte**

```bash
DELETE /api/admin/utilisateurs/5
Authorization: Bearer {token-admin}
```

Le compte est définitivement supprimé. L'employé ne pourra plus se connecter.

### Scénario 4 : Promouvoir un employé en ADMIN

**Changer le rôle de USER à ADMIN**

```bash
PUT /api/admin/utilisateurs/3/role
Authorization: Bearer {token-admin}

{
  "role": "ADMIN"
}
```

L'utilisateur ID 3 devient ADMIN et aura accès aux routes `/api/admin/**`.

---

## 🧪 Tests avec Postman

### Collection Postman : Administration

Créez une collection Postman avec les requêtes suivantes :

#### 1. Login ADMIN

```
POST http://localhost:8080/api/auth/login
Body (JSON):
{
  "email": "admin@dijaboutique.com",
  "motDePasse": "admin123"
}
```

**Variables d'environnement :**
- Créer une variable `adminToken`
- Dans Tests (onglet Scripts) :

```javascript
pm.test("Login réussi", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.environment.set("adminToken", jsonData.token);
});
```

#### 2. Voir tous les utilisateurs

```
GET http://localhost:8080/api/admin/utilisateurs
Headers:
Authorization: Bearer {{adminToken}}
```

#### 3. Créer un utilisateur

```
POST http://localhost:8080/api/admin/utilisateurs
Headers:
Authorization: Bearer {{adminToken}}
Body (JSON):
{
  "nom": "Test",
  "prenom": "Utilisateur",
  "email": "test@dijaboutique.com",
  "motDePasse": "password123",
  "role": "USER"
}
```

#### 4. Modifier un utilisateur

```
PUT http://localhost:8080/api/admin/utilisateurs/2
Headers:
Authorization: Bearer {{adminToken}}
Body (JSON):
{
  "nom": "Nouveau Nom"
}
```

#### 5. Supprimer un utilisateur

```
DELETE http://localhost:8080/api/admin/utilisateurs/2
Headers:
Authorization: Bearer {{adminToken}}
```

#### 6. Tester qu'un USER ne peut pas accéder aux routes ADMIN

**Étape 1 : Se connecter en tant que USER**

```
POST http://localhost:8080/api/auth/login
Body (JSON):
{
  "email": "user@dijaboutique.com",
  "motDePasse": "password123"
}
```

Sauvegarder le token dans une variable `userToken`.

**Étape 2 : Essayer d'accéder à une route ADMIN**

```
GET http://localhost:8080/api/admin/utilisateurs
Headers:
Authorization: Bearer {{userToken}}
```

**Résultat attendu : 403 Forbidden**

```json
{
  "timestamp": "2025-01-16T10:30:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/admin/utilisateurs"
}
```

---

## ⚠️ Gestion des erreurs

### Erreurs courantes

#### 1. Tentative d'inscription publique

**Requête :**
```
POST /api/auth/register
```

**Réponse : 400 Bad Request**
```json
{
  "message": "L'inscription publique est désactivée. Seul l'administrateur peut créer des comptes. Contactez votre administrateur pour obtenir un compte."
}
```

#### 2. USER tente de créer un compte

**Requête :**
```
POST /api/utilisateurs
Authorization: Bearer {token-user}
```

**Réponse : 403 Forbidden**
```json
{
  "message": "La création de compte est réservée aux administrateurs",
  "endpoint": "Utilisez /api/admin/utilisateurs (ADMIN uniquement)"
}
```

#### 3. USER tente de modifier son compte

**Requête :**
```
PUT /api/utilisateurs/2
Authorization: Bearer {token-user}
```

**Réponse : 403 Forbidden**
```json
{
  "message": "La modification de compte est réservée aux administrateurs",
  "endpoint": "Utilisez /api/admin/utilisateurs/{id} (ADMIN uniquement)"
}
```

#### 4. ADMIN tente de se supprimer lui-même

**Requête :**
```
DELETE /api/admin/utilisateurs/1
Authorization: Bearer {token-admin}
```

**Réponse : 400 Bad Request**
```json
{
  "message": "Vous ne pouvez pas supprimer votre propre compte"
}
```

#### 5. Email déjà existant

**Requête :**
```
POST /api/admin/utilisateurs
{
  "email": "admin@dijaboutique.com",
  ...
}
```

**Réponse : 400 Bad Request**
```json
{
  "message": "Un utilisateur avec cet email existe déjà"
}
```

#### 6. Token expiré ou invalide

**Réponse : 401 Unauthorized**
```json
{
  "timestamp": "2025-01-16T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/admin/utilisateurs"
}
```

**Solution :** Se reconnecter pour obtenir un nouveau token.

#### 7. Rôle invalide

**Requête :**
```
PUT /api/admin/utilisateurs/2/role
{
  "role": "SUPER_ADMIN"
}
```

**Réponse : 400 Bad Request**
```json
{
  "message": "Rôle invalide : SUPER_ADMIN. Valeurs acceptées : ADMIN, USER"
}
```

---

## 🔑 Bonnes pratiques de sécurité

### 1. Gestion des mots de passe

✅ **À FAIRE :**
- Utiliser des mots de passe forts (min 12 caractères, mixte)
- Changer le mot de passe ADMIN par défaut immédiatement
- Utiliser des mots de passe temporaires pour les nouveaux employés
- Changer le mot de passe d'un employé s'il le demande

❌ **À ÉVITER :**
- Utiliser le même mot de passe pour plusieurs comptes
- Partager le mot de passe ADMIN
- Laisser le mot de passe par défaut `admin123`

### 2. Gestion des tokens JWT

✅ **À FAIRE :**
- Stocker le token de manière sécurisée (localStorage avec HTTPS)
- Expiration du token : 24 heures (configurable dans `application.properties`)
- Se reconnecter si le token expire

❌ **À ÉVITER :**
- Partager le token ADMIN
- Stocker le token en clair dans le code source

### 3. Gestion des comptes

✅ **À FAIRE :**
- Supprimer immédiatement les comptes des employés partis
- Limiter le nombre d'ADMIN (1 ou 2 maximum)
- Auditer régulièrement les comptes avec `GET /api/admin/statistiques`

❌ **À ÉVITER :**
- Laisser des comptes inactifs
- Promouvoir des USER en ADMIN sans raison

---

## 📊 Récapitulatif

| Fonctionnalité | Endpoint | ADMIN | USER |
|----------------|----------|-------|------|
| Login | `POST /api/auth/login` | ✅ | ✅ |
| Register (public) | `POST /api/auth/register` | ❌ Désactivé | ❌ Désactivé |
| Voir son profil | `GET /api/utilisateurs/moi` | ✅ | ✅ |
| Voir tous les utilisateurs | `GET /api/utilisateurs` | ✅ | ✅ (lecture seule) |
| Créer un compte | `POST /api/admin/utilisateurs` | ✅ | ❌ |
| Modifier un compte | `PUT /api/admin/utilisateurs/{id}` | ✅ | ❌ |
| Supprimer un compte | `DELETE /api/admin/utilisateurs/{id}` | ✅ (pas le sien) | ❌ |
| Changer un rôle | `PUT /api/admin/utilisateurs/{id}/role` | ✅ | ❌ |
| Voir les stats | `GET /api/admin/statistiques` | ✅ | ❌ |
| Utiliser l'application | `/api/achats`, `/api/ventes`, etc. | ✅ | ✅ |

---

## 🚀 Prochaines étapes

1. ✅ Exécuter `create_admin.sql`
2. ✅ Se connecter en tant qu'ADMIN
3. ✅ Changer le mot de passe ADMIN
4. ✅ Créer les comptes employés
5. ✅ Tester les permissions
6. ✅ Déployer en production

---

**🎉 Votre système d'administration est maintenant prêt !**
