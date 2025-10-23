# Tests API - Boutique DijaSaliou

## 🚀 Démarrage du serveur

```bash
# Option 1 : Avec Maven Wrapper
./mvnw spring-boot:run

# Option 2 : Avec Maven installé
mvn spring-boot:run
```

**URL de base** : `http://localhost:8081/api`

---

## 🔐 1. Authentication

### S'inscrire (Register)
```bash
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "nom": "Saliou",
  "prenom": "Dija",
  "email": "dija@boutique.com",
  "motDePasse": "password123",
  "role": "ADMIN"
}
```

**Réponse** :
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "nom": "Saliou",
    "prenom": "Dija",
    "email": "dija@boutique.com",
    "role": "ADMIN",
    "dateCreation": "2025-10-23T17:00:00"
  }
}
```

### Se connecter (Login)
```bash
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "email": "dija@boutique.com",
  "motDePasse": "password123"
}
```

**Réponse** : Même format que le register

---

## 🛒 2. Achats

### Obtenir tous les achats
```bash
GET http://localhost:8081/api/achats
Authorization: Bearer {votre-token-jwt}
```

**Réponse** :
```json
[
  {
    "id": 1,
    "quantite": 10,
    "nomProduit": "Collier en or",
    "prixUnitaire": 15000.00,
    "prixTotal": 150000.00,
    "dateAchat": "2025-10-20",
    "fournisseur": "Fournisseur ABC",
    "utilisateur": {
      "id": 1,
      "nom": "Saliou",
      "prenom": "Dija",
      "email": "dija@boutique.com",
      "role": "ADMIN"
    },
    "estRecent": true,
    "mois": 10,
    "annee": 2025
  }
]
```

### Créer un achat
```bash
POST http://localhost:8081/api/achats?utilisateurId=1
Authorization: Bearer {votre-token-jwt}
Content-Type: application/json

{
  "quantite": 10,
  "nomProduit": "Collier en or",
  "prixUnitaire": 15000.00,
  "dateAchat": "2025-10-20",
  "fournisseur": "Fournisseur ABC"
}
```

### Obtenir un achat par ID
```bash
GET http://localhost:8081/api/achats/1
Authorization: Bearer {votre-token-jwt}
```

### Obtenir les achats d'un utilisateur
```bash
GET http://localhost:8081/api/achats/utilisateur/1
Authorization: Bearer {votre-token-jwt}
```

### Modifier un achat
```bash
PUT http://localhost:8081/api/achats/1
Authorization: Bearer {votre-token-jwt}
Content-Type: application/json

{
  "quantite": 15,
  "nomProduit": "Collier en or (modifié)",
  "prixUnitaire": 16000.00,
  "dateAchat": "2025-10-20",
  "fournisseur": "Fournisseur ABC"
}
```

### Supprimer un achat
```bash
DELETE http://localhost:8081/api/achats/1
Authorization: Bearer {votre-token-jwt}
```

### Statistiques des achats
```bash
GET http://localhost:8081/api/achats/statistiques?debut=2025-10-01&fin=2025-10-31
Authorization: Bearer {votre-token-jwt}
```

**Réponse** :
```json
{
  "dateDebut": "2025-10-01",
  "dateFin": "2025-10-31",
  "nombreAchats": 5,
  "montantTotal": 750000.00,
  "achats": [...]
}
```

---

## 💰 3. Ventes

### Obtenir toutes les ventes
```bash
GET http://localhost:8081/api/ventes
Authorization: Bearer {votre-token-jwt}
```

**Réponse** :
```json
[
  {
    "id": 1,
    "quantite": 1,
    "nomProduit": "Collier en or",
    "prixUnitaire": 25000.00,
    "prixTotal": 25000.00,
    "dateVente": "2025-10-23",
    "client": "Marie Dupont",
    "utilisateur": {
      "id": 1,
      "nom": "Saliou",
      "prenom": "Dija",
      "email": "dija@boutique.com",
      "role": "ADMIN"
    },
    "estRecente": true,
    "mois": 10,
    "annee": 2025,
    "aUnClient": true
  }
]
```

### Créer une vente
```bash
POST http://localhost:8081/api/ventes?utilisateurId=1
Authorization: Bearer {votre-token-jwt}
Content-Type: application/json

{
  "quantite": 1,
  "nomProduit": "Collier en or",
  "prixUnitaire": 25000.00,
  "dateVente": "2025-10-23",
  "client": "Marie Dupont"
}
```

### Obtenir une vente par ID
```bash
GET http://localhost:8081/api/ventes/1
Authorization: Bearer {votre-token-jwt}
```

### Obtenir les ventes d'un utilisateur
```bash
GET http://localhost:8081/api/ventes/utilisateur/1
Authorization: Bearer {votre-token-jwt}
```

### Modifier une vente
```bash
PUT http://localhost:8081/api/ventes/1
Authorization: Bearer {votre-token-jwt}
Content-Type: application/json

{
  "quantite": 2,
  "nomProduit": "Collier en or",
  "prixUnitaire": 25000.00,
  "dateVente": "2025-10-23",
  "client": "Marie Dupont"
}
```

### Supprimer une vente
```bash
DELETE http://localhost:8081/api/ventes/1
Authorization: Bearer {votre-token-jwt}
```

### Calculer le chiffre d'affaires
```bash
GET http://localhost:8081/api/ventes/chiffre-affaires?debut=2025-10-01&fin=2025-10-31
Authorization: Bearer {votre-token-jwt}
```

**Réponse** : `375000.00`

### Statistiques des ventes
```bash
GET http://localhost:8081/api/ventes/statistiques?debut=2025-10-01&fin=2025-10-31
Authorization: Bearer {votre-token-jwt}
```

**Réponse** :
```json
{
  "dateDebut": "2025-10-01",
  "dateFin": "2025-10-31",
  "nombreVentes": 15,
  "chiffreAffaires": 375000.00,
  "ventes": [...]
}
```

---

## 💸 4. Dépenses

### Obtenir toutes les dépenses
```bash
GET http://localhost:8081/api/depenses
Authorization: Bearer {votre-token-jwt}
```

**Réponse** :
```json
[
  {
    "id": 1,
    "libelle": "Loyer octobre 2025",
    "montant": 50000.00,
    "dateDepense": "2025-10-01",
    "categorie": "LOYER",
    "notes": "Loyer du local principal",
    "estRecurrente": true,
    "utilisateur": {
      "id": 1,
      "nom": "Saliou",
      "prenom": "Dija",
      "email": "dija@boutique.com",
      "role": "ADMIN"
    },
    "estRecente": true,
    "mois": 10,
    "annee": 2025,
    "libelleCategorie": "Loyer",
    "aDesNotes": true
  }
]
```

### Créer une dépense
```bash
POST http://localhost:8081/api/depenses?utilisateurId=1
Authorization: Bearer {votre-token-jwt}
Content-Type: application/json

{
  "libelle": "Loyer octobre 2025",
  "montant": 50000.00,
  "dateDepense": "2025-10-01",
  "categorie": "LOYER",
  "notes": "Loyer du local principal",
  "estRecurrente": true
}
```

### Catégories disponibles
```
LOYER, ELECTRICITE, EAU, INTERNET, TRANSPORT, MARKETING,
FOURNITURES, MAINTENANCE, SALAIRES, ASSURANCE, TAXES,
FORMATION, EQUIPEMENT, AUTRE
```

### Obtenir une dépense par ID
```bash
GET http://localhost:8081/api/depenses/1
Authorization: Bearer {votre-token-jwt}
```

### Obtenir les dépenses d'un utilisateur
```bash
GET http://localhost:8081/api/depenses/utilisateur/1
Authorization: Bearer {votre-token-jwt}
```

### Obtenir les dépenses par catégorie
```bash
GET http://localhost:8081/api/depenses/categorie/LOYER
Authorization: Bearer {votre-token-jwt}
```

### Modifier une dépense
```bash
PUT http://localhost:8081/api/depenses/1
Authorization: Bearer {votre-token-jwt}
Content-Type: application/json

{
  "libelle": "Loyer octobre 2025 (modifié)",
  "montant": 55000.00,
  "dateDepense": "2025-10-01",
  "categorie": "LOYER",
  "notes": "Augmentation du loyer",
  "estRecurrente": true
}
```

### Supprimer une dépense
```bash
DELETE http://localhost:8081/api/depenses/1
Authorization: Bearer {votre-token-jwt}
```

### Calculer le total des dépenses
```bash
GET http://localhost:8081/api/depenses/total?debut=2025-10-01&fin=2025-10-31
Authorization: Bearer {votre-token-jwt}
```

**Réponse** : `150000.00`

### Statistiques des dépenses
```bash
GET http://localhost:8081/api/depenses/statistiques?debut=2025-10-01&fin=2025-10-31
Authorization: Bearer {votre-token-jwt}
```

**Réponse** :
```json
{
  "dateDebut": "2025-10-01",
  "dateFin": "2025-10-31",
  "nombreDepenses": 8,
  "montantTotal": 150000.00,
  "depenses": [...]
}
```

---

## 🧪 Test avec cURL

### 1. S'inscrire
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Saliou",
    "prenom": "Dija",
    "email": "dija@boutique.com",
    "motDePasse": "password123",
    "role": "ADMIN"
  }'
```

### 2. Récupérer le token et l'utiliser
```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl http://localhost:8081/api/achats \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Créer un achat
```bash
curl -X POST "http://localhost:8081/api/achats?utilisateurId=1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "quantite": 10,
    "nomProduit": "Collier en or",
    "prixUnitaire": 15000.00,
    "dateAchat": "2025-10-20",
    "fournisseur": "Fournisseur ABC"
  }'
```

---

## 🧪 Test avec Postman

### Configuration de base

1. **Créer une nouvelle collection** : "Boutique DijaSaliou"

2. **Ajouter des variables d'environnement** :
   - `base_url` : `http://localhost:8081/api`
   - `token` : (sera rempli après login)
   - `userId` : (sera rempli après login)

3. **S'inscrire** :
   - Method: POST
   - URL: `{{base_url}}/auth/register`
   - Body (JSON):
     ```json
     {
       "nom": "Saliou",
       "prenom": "Dija",
       "email": "dija@boutique.com",
       "motDePasse": "password123",
       "role": "ADMIN"
     }
     ```
   - Script "Tests" pour sauvegarder le token automatiquement :
     ```javascript
     const response = pm.response.json();
     pm.environment.set("token", response.token);
     pm.environment.set("userId", response.user.id);
     ```

4. **Créer un achat** :
   - Method: POST
   - URL: `{{base_url}}/achats?utilisateurId={{userId}}`
   - Headers: `Authorization: Bearer {{token}}`
   - Body (JSON):
     ```json
     {
       "quantite": 10,
       "nomProduit": "Collier en or",
       "prixUnitaire": 15000.00,
       "dateAchat": "2025-10-20",
       "fournisseur": "Fournisseur ABC"
     }
     ```

5. **Obtenir tous les achats** :
   - Method: GET
   - URL: `{{base_url}}/achats`
   - Headers: `Authorization: Bearer {{token}}`

---

## ✅ Vérifications

### Vérifier que l'utilisateur est inclus
```bash
# Dans la réponse JSON, vous devriez voir :
{
  "id": 1,
  "nomProduit": "Collier en or",
  ...
  "utilisateur": {
    "id": 1,
    "nom": "Saliou",
    "prenom": "Dija",
    "email": "dija@boutique.com",
    "role": "ADMIN"
  }
}
```

### Vérifier qu'il n'y a pas de boucle
- Le JSON doit se charger correctement
- Pas d'erreur "Infinite recursion"
- Le mot de passe n'apparaît jamais

### Vérifier les champs calculés
```json
{
  "estRecent": true,
  "mois": 10,
  "annee": 2025,
  "aDesNotes": true  // pour les dépenses
}
```

---

## 🐛 Debugging

### Problème : 401 Unauthorized
- Vérifiez que le token JWT est valide
- Vérifiez que le header Authorization est bien `Bearer {token}`

### Problème : 404 Not Found
- Vérifiez l'URL de base : `http://localhost:8081/api`
- Vérifiez que le serveur est démarré

### Problème : 500 Internal Server Error
- Vérifiez les logs du serveur
- Vérifiez que MySQL est démarré
- Vérifiez que la base de données `dijaSaliou` existe

### Problème : Boucle de sérialisation
- Assurez-vous que tous les contrôleurs utilisent les DTOs
- Vérifiez que `@JsonBackReference` et `@JsonManagedReference` sont corrects dans les entités

---

## 📝 Notes

- **Port** : 8081 (changé de 8080 pour éviter les conflits)
- **Context path** : `/api`
- **Base de données** : MySQL sur localhost:3306
- **Nom de la base** : dijaSaliou
- **Tous les utilisateurs authentifiés** peuvent voir toutes les données
