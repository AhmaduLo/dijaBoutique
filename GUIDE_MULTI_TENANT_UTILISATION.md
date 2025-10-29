# Guide d'Utilisation Multi-Tenant

## Problème Rencontré

Quand vous créez un **Compte A**, puis faites des achats/ventes, ensuite créez un **Compte B**, vous restez connecté au Compte A et voyez les données du Compte A.

## Solution : Se Déconnecter et Se Reconnecter

### Flux Correct

```
1. Créer Compte A (Entreprise A)
   └─> Vous recevez un JWT avec tenant_id de l'Entreprise A

2. Se connecter avec Compte A
   └─> Vous recevez un JWT avec tenant_id de l'Entreprise A
   └─> Vous pouvez créer des achats/ventes pour l'Entreprise A

3. Créer Compte B (Entreprise B)
   └─> Un nouveau tenant est créé pour l'Entreprise B

4. ⚠️ IMPORTANT : Se DÉCONNECTER du Compte A

5. Se connecter avec Compte B
   └─> Vous recevez un NOUVEAU JWT avec tenant_id de l'Entreprise B
   └─> Maintenant vous voyez UNIQUEMENT les données de l'Entreprise B
```

---

## Étapes Détaillées avec Postman

### Étape 1 : Créer Compte A (Entreprise A)

**Requête** :
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "nom": "DIALLO",
  "prenom": "Amadou",
  "email": "amadou@entrepriseA.com",
  "motDePasse": "password123",
  "nomEntreprise": "Entreprise A",
  "numeroTelephone": "771234567"
}
```

**Réponse** :
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbWFkb3VAZe50cmVwcmlzZUEuY29tIiwiaWF0IjoxNzQxNjM5MjMwLCJleHAiOjE3NDE2NDI4MzAsInRlbmFudF9pZCI6IjEyMzQ1Njc4LWFiY2QtZWZnaC1pams...",
  "user": {
    "id": 1,
    "nom": "DIALLO",
    "prenom": "Amadou",
    "email": "amadou@entrepriseA.com",
    "role": "ADMIN",
    "nomEntreprise": "Entreprise A"
  }
}
```

**Copiez le token** dans Postman Authorization > Bearer Token.

---

### Étape 2 : Créer des Achats/Ventes pour Entreprise A

**Créer un Achat** :
```http
POST http://localhost:8080/api/achats
Authorization: Bearer <TOKEN_COMPTE_A>
Content-Type: application/json

{
  "nomProduit": "Ordinateur HP",
  "quantite": 10,
  "prixUnitaire": 500000,
  "dateAchat": "2025-10-29",
  "fournisseur": "Fournisseur A"
}
```

**Créer une Vente** :
```http
POST http://localhost:8080/api/ventes
Authorization: Bearer <TOKEN_COMPTE_A>
Content-Type: application/json

{
  "nomProduit": "Ordinateur HP",
  "quantite": 2,
  "prixUnitaire": 650000,
  "dateVente": "2025-10-29",
  "client": "Client A"
}
```

✅ Ces données appartiennent maintenant à **Entreprise A**.

---

### Étape 3 : Créer Compte B (Entreprise B)

**Requête** :
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "nom": "SOW",
  "prenom": "Fatou",
  "email": "fatou@entrepriseB.com",
  "motDePasse": "password456",
  "nomEntreprise": "Entreprise B",
  "numeroTelephone": "779876543"
}
```

**Réponse** :
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYXRvdUBlbnRyZXByaXNlQi5jb20iLCJpYXQiOjE3NDE2MzkyMzAsImV4cCI6MTc0MTY0MjgzMCwidGVuYW50X2lkIjoiOTg3NjU0MzItd3h5ei1hYmNkLWVmZ2guLi4=",
  "user": {
    "id": 2,
    "nom": "SOW",
    "prenom": "Fatou",
    "email": "fatou@entrepriseB.com",
    "role": "ADMIN",
    "nomEntreprise": "Entreprise B"
  }
}
```

⚠️ **ATTENTION** : Ne gardez PAS l'ancien token du Compte A !

---

### Étape 4 : Se DÉCONNECTER du Compte A

Dans Postman :

1. Allez dans **Authorization** de votre requête
2. **Supprimez** l'ancien token du Compte A
3. **Collez** le NOUVEAU token du Compte B

**OU** dans votre frontend :

```javascript
// Se déconnecter
localStorage.removeItem('token');
localStorage.removeItem('user');

// Se connecter avec Compte B
localStorage.setItem('token', nouveauTokenCompteB);
```

---

### Étape 5 : Vérifier l'Isolation des Données

**Avec le token du Compte B**, essayez de lister les achats :

```http
GET http://localhost:8080/api/achats
Authorization: Bearer <TOKEN_COMPTE_B>
```

**Résultat attendu** :
```json
[]
```

✅ La liste est **VIDE** car Entreprise B n'a encore fait aucun achat !

Les achats de l'Entreprise A sont **INVISIBLES** pour l'Entreprise B.

---

### Étape 6 : Créer des Données pour Entreprise B

**Créer un Achat** :
```http
POST http://localhost:8080/api/achats
Authorization: Bearer <TOKEN_COMPTE_B>
Content-Type: application/json

{
  "nomProduit": "iPhone 15",
  "quantite": 5,
  "prixUnitaire": 800000,
  "dateAchat": "2025-10-29",
  "fournisseur": "Fournisseur B"
}
```

**Lister les Achats** :
```http
GET http://localhost:8080/api/achats
Authorization: Bearer <TOKEN_COMPTE_B>
```

**Résultat** :
```json
[
  {
    "id": 2,
    "nomProduit": "iPhone 15",
    "quantite": 5,
    "prixUnitaire": 800000,
    "fournisseur": "Fournisseur B"
  }
]
```

✅ Vous voyez UNIQUEMENT les achats de l'Entreprise B !

---

### Étape 7 : Se Reconnecter au Compte A

**Requête de connexion** :
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "amadou@entrepriseA.com",
  "motDePasse": "password123"
}
```

**Réponse** :
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...<TOKEN_AVEC_TENANT_A>...",
  "user": {
    "id": 1,
    "nom": "DIALLO",
    "prenom": "Amadou",
    "email": "amadou@entrepriseA.com",
    "nomEntreprise": "Entreprise A"
  }
}
```

**Utilisez ce nouveau token** dans Postman.

**Lister les Achats** :
```http
GET http://localhost:8080/api/achats
Authorization: Bearer <TOKEN_COMPTE_A>
```

**Résultat** :
```json
[
  {
    "id": 1,
    "nomProduit": "Ordinateur HP",
    "quantite": 10,
    "prixUnitaire": 500000,
    "fournisseur": "Fournisseur A"
  }
]
```

✅ Vous voyez UNIQUEMENT les achats de l'Entreprise A !

L'achat "iPhone 15" de l'Entreprise B est **INVISIBLE**.

---

## Règles Importantes

### 1. Le JWT Token Contient le Tenant

Chaque JWT token contient le `tenant_id` de l'entreprise :

```json
{
  "sub": "amadou@entrepriseA.com",
  "tenant_id": "12345678-abcd-efgh-ijkl-mnopqrstuvwx",
  "iat": 1741639230,
  "exp": 1741642830
}
```

### 2. Un Token = Une Entreprise

- Token du Compte A → Données de l'Entreprise A
- Token du Compte B → Données de l'Entreprise B

### 3. Changer de Compte = Changer de Token

Pour voir les données d'une autre entreprise, vous DEVEZ :

1. **Se déconnecter** (supprimer l'ancien token)
2. **Se reconnecter** avec les identifiants de l'autre entreprise
3. **Utiliser le nouveau token**

---

## Vérification en Base de Données

Vous pouvez vérifier l'isolation en base de données :

```sql
-- Voir tous les tenants
SELECT * FROM tenants;

-- Résultat attendu :
+----+--------------------------------------+---------------+-----------------+--------+
| id | tenant_uuid                          | nom_entreprise| numero_telephone| plan   |
+----+--------------------------------------+---------------+-----------------+--------+
| 1  | 12345678-abcd-efgh-ijkl-mnopqrstuvwx | Entreprise A  | 771234567       | GRATUIT|
| 2  | 98765432-wxyz-abcd-efgh-ijklmnopqrst | Entreprise B  | 779876543       | GRATUIT|
+----+--------------------------------------+---------------+-----------------+--------+

-- Voir les achats avec leur tenant
SELECT id, nom_produit, quantite, fournisseur, tenant_id FROM achats;

-- Résultat attendu :
+----+---------------+----------+---------------+-----------+
| id | nom_produit   | quantite | fournisseur   | tenant_id |
+----+---------------+----------+---------------+-----------+
| 1  | Ordinateur HP | 10       | Fournisseur A | 1         |
| 2  | iPhone 15     | 5        | Fournisseur B | 2         |
+----+---------------+----------+---------------+-----------+
```

Chaque achat a un `tenant_id` différent → **Isolation complète** !

---

## Résumé : Comment Changer de Compte

| Action | Dans Postman | Dans Frontend |
|--------|-------------|---------------|
| **Se déconnecter** | Supprimer l'ancien token de Authorization | `localStorage.removeItem('token')` |
| **Se connecter au Compte B** | Faire POST /api/auth/login avec email/password du Compte B | `localStorage.setItem('token', newToken)` |
| **Utiliser le nouveau token** | Coller le nouveau token dans Authorization > Bearer | Utiliser le token dans les headers HTTP |

---

## Questions Fréquentes

### Q1 : Pourquoi je vois toujours les données du Compte A ?

**R** : Vous utilisez toujours l'ancien token du Compte A. Vous devez vous **reconnecter** avec le Compte B pour obtenir un nouveau token.

### Q2 : Puis-je utiliser un seul token pour plusieurs comptes ?

**R** : Non ! Chaque token est lié à UNE entreprise. Impossible de voir les données d'une autre entreprise avec le même token.

### Q3 : Comment savoir quel tenant je suis connecté ?

**R** : Décodez votre JWT token sur https://jwt.io et regardez le champ `tenant_id`.

### Q4 : Est-ce que l'admin peut voir toutes les entreprises ?

**R** : Non ! Même l'admin d'une entreprise ne voit QUE les données de son entreprise. C'est la sécurité multi-tenant.

---

## Félicitations ! 🎉

Votre application est maintenant **100% multi-tenant sécurisée** avec isolation complète des données entre les entreprises !
