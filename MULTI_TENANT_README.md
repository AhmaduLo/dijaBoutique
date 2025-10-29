# 🏢 SYSTÈME MULTI-TENANT - GUIDE COMPLET

## 📋 Table des matières
1. [Vue d'ensemble](#vue-densemble)
2. [Architecture](#architecture)
3. [Sécurité](#sécurité)
4. [Migration](#migration)
5. [Utilisation](#utilisation)
6. [Tests](#tests)
7. [Dépannage](#dépannage)

---

## 🎯 Vue d'ensemble

Votre application est maintenant un **SaaS multi-tenant** où :
- ✅ **Chaque entreprise qui s'inscrit = 1 TENANT isolé**
- ✅ **Isolation totale** : Une entreprise ne peut JAMAIS voir les données d'une autre
- ✅ **Automatique** : Le filtrage des données est transparent pour les développeurs
- ✅ **Sécurisé** : Protection à plusieurs niveaux (JWT, JPA Filters, Entity Listeners)

---

## 🏗️ Architecture

### Composants principaux

#### 1. **TenantEntity** (`entity/TenantEntity.java`)
- Représente une entreprise cliente
- Génère automatiquement un UUID unique
- Gère les plans d'abonnement (GRATUIT, BASIC, PREMIUM, ENTREPRISE)

#### 2. **TenantContext** (`tenant/TenantContext.java`)
- Stocke le tenant_id de l'utilisateur connecté (ThreadLocal)
- Garantit l'isolation entre les requêtes HTTP

#### 3. **JwtService** (`jwt/JwtService.java`)
- Génère des tokens JWT contenant `tenant_id`
- Extrait le `tenant_id` lors de l'authentification

#### 4. **JwtAuthenticationFilter** (`jwt/JwtAuthenticationFilter.java`)
- Intercepte chaque requête HTTP
- Extrait le `tenant_id` du JWT
- Stocke le `tenant_id` dans TenantContext

#### 5. **TenantJpaConfig** (`config/TenantJpaConfig.java`)
- Active automatiquement le filtre Hibernate avant chaque requête
- Ajoute `WHERE tenant_id = :tenantId` à toutes les requêtes SQL

#### 6. **TenantEntityListener** (`tenant/TenantEntityListener.java`)
- Assigne automatiquement le `tenant_id` lors de la création d'entités
- Évite d'avoir à définir manuellement le tenant

---

## 🔒 Sécurité

### Protection multi-niveaux

1. **Niveau JWT** : Le tenant_id est dans le token (impossible de le modifier sans invalider le token)
2. **Niveau JPA** : Filtrage automatique des requêtes SQL
3. **Niveau Entity** : Assignation automatique du tenant lors de la création
4. **Niveau Repository** : Isolation transparente

### Principes appliqués

✅ **Zero Trust** : Chaque couche vérifie le tenant_id
✅ **Defense in Depth** : Plusieurs couches de sécurité
✅ **Fail Secure** : En cas d'erreur, l'accès est refusé (pas de tenant = pas de données)
✅ **Principle of Least Privilege** : Accès uniquement aux données de son tenant

---

## 🔄 Migration

### Étape 1 : Prérequis

```bash
# Vérifier que vous avez une sauvegarde de la base de données
mysqldump -u root -p dijasaliou > backup_avant_migration.sql
```

### Étape 2 : Exécuter la migration

La migration s'exécutera automatiquement au démarrage si vous utilisez Flyway/Liquibase.

**OU** exécuter manuellement :

```bash
mysql -u root -p dijasaliou < src/main/resources/db/migration/V1__create_multi_tenant_schema.sql
```

### Étape 3 : Vérification

```sql
-- Vérifier que la table tenants existe
SHOW TABLES LIKE 'tenants';

-- Vérifier que les colonnes tenant_id existent
DESCRIBE utilisateurs;
DESCRIBE achats;
DESCRIBE ventes;
DESCRIBE depenses;

-- Vérifier qu'un tenant par défaut a été créé
SELECT * FROM tenants;

-- Vérifier que tous les utilisateurs ont un tenant_id
SELECT COUNT(*) FROM utilisateurs WHERE tenant_id IS NULL;
-- Résultat attendu : 0
```

### Étape 4 : Migration des données existantes

Si vous avez des données existantes :

1. **Option A** : Tout assigner à un seul tenant (déjà fait par le script)
```sql
-- Un tenant "Entreprise Par Défaut" a été créé
-- Toutes les données existantes lui ont été assignées
```

2. **Option B** : Séparer les données par entreprise (à faire manuellement)
```sql
-- Créer un tenant par entreprise
INSERT INTO tenants (tenant_uuid, nom_entreprise, numero_telephone)
VALUES (UUID(), 'Entreprise A', '+221771234567');

-- Assigner les utilisateurs au bon tenant
UPDATE utilisateurs
SET tenant_id = (SELECT id FROM tenants WHERE nom_entreprise = 'Entreprise A')
WHERE nom_entreprise = 'Entreprise A';

-- Les achats/ventes/dépenses suivront automatiquement via les triggers
```

---

## 🚀 Utilisation

### 1. Inscription (Création d'un nouveau tenant)

```http
POST /api/auth/register
Content-Type: application/json

{
  "nom": "Saliou",
  "prenom": "Dija",
  "email": "dija@boutique.com",
  "motDePasse": "password123",
  "nomEntreprise": "Boutique DijaSaliou",
  "numeroTelephone": "+221771234567"
}
```

**Résultat** :
- ✅ Création d'un nouveau **TENANT** (entreprise)
- ✅ Création d'un utilisateur **ADMIN** pour ce tenant
- ✅ Génération d'un **JWT** contenant le `tenant_id`

### 2. Connexion

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "dija@boutique.com",
  "motDePasse": "password123"
}
```

**Résultat** :
- ✅ JWT contenant le `tenant_id` de l'entreprise

### 3. Utilisation des API (Exemple : Créer un achat)

```http
POST /api/achats
Authorization: Bearer <votre_jwt_avec_tenant_id>
Content-Type: application/json

{
  "nomProduit": "Ordinateur",
  "quantite": 5,
  "prixUnitaire": 500000,
  "dateAchat": "2025-10-29",
  "fournisseur": "Fournisseur A"
}
```

**Magie** ✨ :
- Le `tenant_id` est **automatiquement** assigné depuis le JWT
- Vous n'avez **rien à faire** manuellement
- L'achat est **automatiquement** lié au tenant de l'utilisateur connecté

### 4. Liste des achats

```http
GET /api/achats
Authorization: Bearer <votre_jwt_avec_tenant_id>
```

**Résultat** :
- ✅ Seuls les achats de **VOTRE** entreprise sont retournés
- ✅ Impossible de voir les achats des autres entreprises
- ✅ Filtrage automatique et transparent

---

## 🧪 Tests

### Test 1 : Isolation des données

```bash
# 1. Créer deux entreprises
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "User1",
    "prenom": "Entreprise1",
    "email": "user1@entreprise1.com",
    "motDePasse": "password123",
    "nomEntreprise": "Entreprise A",
    "numeroTelephone": "+221771111111"
  }'

curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "User2",
    "prenom": "Entreprise2",
    "email": "user2@entreprise2.com",
    "motDePasse": "password123",
    "nomEntreprise": "Entreprise B",
    "numeroTelephone": "+221772222222"
  }'

# 2. Se connecter avec Entreprise A
TOKEN_A=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user1@entreprise1.com", "motDePasse": "password123"}' \
  | jq -r '.token')

# 3. Se connecter avec Entreprise B
TOKEN_B=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user2@entreprise2.com", "motDePasse": "password123"}' \
  | jq -r '.token')

# 4. Créer un achat pour Entreprise A
curl -X POST http://localhost:8080/api/achats \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{
    "nomProduit": "Produit A",
    "quantite": 10,
    "prixUnitaire": 1000,
    "dateAchat": "2025-10-29"
  }'

# 5. Lister les achats de l'Entreprise A
curl -X GET http://localhost:8080/api/achats \
  -H "Authorization: Bearer $TOKEN_A"
# Résultat attendu : 1 achat (Produit A)

# 6. Lister les achats de l'Entreprise B
curl -X GET http://localhost:8080/api/achats \
  -H "Authorization: Bearer $TOKEN_B"
# Résultat attendu : 0 achat (isolation OK ✅)
```

### Test 2 : Vérifier le tenant_id dans le JWT

```bash
# Décoder le JWT (utiliser jwt.io ou jq)
echo $TOKEN_A | cut -d'.' -f2 | base64 -d | jq .

# Résultat attendu :
# {
#   "sub": "user1@entreprise1.com",
#   "tenant_id": "12345678-1234-1234-1234-123456789abc",
#   "iat": 1234567890,
#   "exp": 1234654290
# }
```

### Test 3 : Vérifier la base de données

```sql
-- Vérifier que chaque tenant a ses propres données
SELECT t.nom_entreprise, COUNT(a.id) AS nb_achats
FROM tenants t
LEFT JOIN achats a ON t.id = a.tenant_id
GROUP BY t.id, t.nom_entreprise;

-- Résultat attendu :
-- +------------------+------------+
-- | nom_entreprise   | nb_achats  |
-- +------------------+------------+
-- | Entreprise A     | 1          |
-- | Entreprise B     | 0          |
-- +------------------+------------+
```

---

## 🛠️ Dépannage

### Problème 1 : "Aucun tenant défini dans le contexte"

**Cause** : Le JWT ne contient pas de `tenant_id`

**Solution** :
1. Se reconnecter pour obtenir un nouveau JWT
2. Vérifier que l'utilisateur a bien un `tenant_id` en base

```sql
SELECT id, email, tenant_id FROM utilisateurs WHERE email = 'votre@email.com';
```

### Problème 2 : "Tenant introuvable pour l'UUID"

**Cause** : Le tenant a été supprimé ou est inactif

**Solution** :
```sql
-- Vérifier le statut du tenant
SELECT * FROM tenants WHERE tenant_uuid = 'votre-uuid';

-- Réactiver si nécessaire
UPDATE tenants SET actif = TRUE WHERE tenant_uuid = 'votre-uuid';
```

### Problème 3 : Voir les données d'autres tenants

**Cause** : Le filtre JPA n'est pas activé

**Solution** :
1. Vérifier les logs : `grep "Filtre tenant activé" logs/application.log`
2. Vérifier que `TenantJpaConfig` est bien chargé
3. Redémarrer l'application

### Problème 4 : Les nouvelles entités n'ont pas de tenant_id

**Cause** : `TenantEntityListener` n'est pas déclenché

**Solution** :
1. Vérifier que `@EntityListeners(TenantEntityListener.class)` est présent sur l'entité
2. Vérifier que l'utilisateur est authentifié
3. Vérifier les logs d'erreur

---

## 📊 Monitoring

### Métriques à surveiller

1. **Nombre de tenants actifs**
```sql
SELECT COUNT(*) FROM tenants WHERE actif = TRUE;
```

2. **Distribution des données par tenant**
```sql
SELECT t.nom_entreprise,
       COUNT(DISTINCT u.id) AS nb_users,
       COUNT(DISTINCT a.id) AS nb_achats,
       COUNT(DISTINCT v.id) AS nb_ventes
FROM tenants t
LEFT JOIN utilisateurs u ON t.id = u.tenant_id
LEFT JOIN achats a ON t.id = a.tenant_id
LEFT JOIN ventes v ON t.id = v.tenant_id
GROUP BY t.id, t.nom_entreprise;
```

3. **Tenants sans activité**
```sql
SELECT t.*
FROM tenants t
LEFT JOIN achats a ON t.id = a.tenant_id
LEFT JOIN ventes v ON t.id = v.tenant_id
WHERE a.id IS NULL AND v.id IS NULL;
```

---

## 🎓 Bonnes pratiques

### ✅ À FAIRE

1. **Toujours** se connecter avant d'accéder aux données
2. **Toujours** vérifier que le token est valide
3. **Toujours** utiliser les Repository standards (le filtrage est automatique)
4. **Toujours** logger les tentatives d'accès suspects

### ❌ À NE PAS FAIRE

1. **JAMAIS** désactiver le filtre tenant sans raison valide
2. **JAMAIS** passer le tenant_id manuellement dans les requêtes
3. **JAMAIS** partager les tokens JWT entre utilisateurs
4. **JAMAIS** utiliser des requêtes SQL natives sans filtre tenant

---

## 📞 Support

En cas de problème :
1. Consulter les logs : `logs/application.log`
2. Vérifier la base de données (requêtes SQL ci-dessus)
3. Tester l'isolation avec les scripts de test
4. Contacter l'équipe de développement

---

## 🔐 Sécurité en production

### Checklist avant mise en production

- [ ] Changer `jwt.secret` dans `application.properties`
- [ ] Activer HTTPS uniquement
- [ ] Configurer les CORS correctement
- [ ] Activer les logs d'audit
- [ ] Mettre en place un monitoring
- [ ] Tester l'isolation avec plusieurs tenants
- [ ] Documenter la procédure de backup
- [ ] Former les administrateurs

---

## 📝 Changelog

### Version 1.0.0 (2025-10-29)
- ✅ Transformation en SaaS multi-tenant
- ✅ Isolation totale des données par tenant
- ✅ Filtrage automatique JPA
- ✅ Assignation automatique du tenant
- ✅ Migration des données existantes
- ✅ Tests d'isolation

---

**🎉 Félicitations ! Votre application est maintenant un SaaS multi-tenant sécurisé !**
