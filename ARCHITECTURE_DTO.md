# Architecture DTO - Boutique DijaSaliou

## 📋 Vue d'ensemble

Cette architecture utilise le pattern **DTO (Data Transfer Object)** pour :
- ✅ Éviter les boucles de sérialisation infinies
- ✅ Retourner l'utilisateur dans les réponses JSON
- ✅ Protéger les données sensibles (mot de passe)
- ✅ Contrôler exactement quelles données sont exposées
- ✅ Optimiser les performances (pas de lazy loading non désiré)

## 🏗️ Structure des DTOs

### 1. UserDto
**Localisation** : `src/main/java/com/example/dijasaliou/dto/UserDto.java`

**Contenu** :
```java
{
  "id": 1,
  "nom": "Saliou",
  "prenom": "Dija",
  "email": "dija@boutique.com",
  "role": "ADMIN",
  "dateCreation": "2025-10-23T10:00:00"
}
```

**Méthodes utiles** :
- `UserDto.fromEntity(UserEntity user)` : Conversion complète
- `UserDto.fromEntityMinimal(UserEntity user)` : Version sans date (pour listes imbriquées)

---

### 2. AchatDto
**Localisation** : `src/main/java/com/example/dijasaliou/dto/AchatDto.java`

**Contenu** :
```java
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
```

**Méthodes** :
- `AchatDto.fromEntity(AchatEntity achat)` : Avec utilisateur
- `AchatDto.fromEntityWithoutUser(AchatEntity achat)` : Sans utilisateur

---

### 3. VenteDto
**Localisation** : `src/main/java/com/example/dijasaliou/dto/VenteDto.java`

**Contenu** :
```java
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
```

**Méthodes** :
- `VenteDto.fromEntity(VenteEntity vente)` : Avec utilisateur
- `VenteDto.fromEntityWithoutUser(VenteEntity vente)` : Sans utilisateur

---

### 4. DepenseDto
**Localisation** : `src/main/java/com/example/dijasaliou/dto/DepenseDto.java`

**Contenu** :
```java
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
```

**Méthodes** :
- `DepenseDto.fromEntity(DepenseEntity depense)` : Avec utilisateur
- `DepenseDto.fromEntityWithoutUser(DepenseEntity depense)` : Sans utilisateur

---

## 🎯 Endpoints mis à jour

### Achats

| Méthode | Endpoint | Retour |
|---------|----------|--------|
| GET | `/api/achats` | `List<AchatDto>` |
| GET | `/api/achats/{id}` | `AchatDto` |
| GET | `/api/achats/utilisateur/{id}` | `List<AchatDto>` |
| POST | `/api/achats?utilisateurId=1` | `AchatDto` |
| PUT | `/api/achats/{id}` | `AchatDto` |
| GET | `/api/achats/statistiques?debut=...&fin=...` | `Map` avec `List<AchatDto>` |
| DELETE | `/api/achats/{id}` | `204 No Content` |

### Ventes

| Méthode | Endpoint | Retour |
|---------|----------|--------|
| GET | `/api/ventes` | `List<VenteDto>` |
| GET | `/api/ventes/{id}` | `VenteDto` |
| GET | `/api/ventes/utilisateur/{id}` | `List<VenteDto>` |
| POST | `/api/ventes?utilisateurId=1` | `VenteDto` |
| PUT | `/api/ventes/{id}` | `VenteDto` |
| GET | `/api/ventes/chiffre-affaires?debut=...&fin=...` | `BigDecimal` |
| GET | `/api/ventes/statistiques?debut=...&fin=...` | `Map` avec `List<VenteDto>` |
| DELETE | `/api/ventes/{id}` | `204 No Content` |

### Dépenses

| Méthode | Endpoint | Retour |
|---------|----------|--------|
| GET | `/api/depenses` | `List<DepenseDto>` |
| GET | `/api/depenses/{id}` | `DepenseDto` |
| GET | `/api/depenses/utilisateur/{id}` | `List<DepenseDto>` |
| GET | `/api/depenses/categorie/{categorie}` | `List<DepenseDto>` |
| POST | `/api/depenses?utilisateurId=1` | `DepenseDto` |
| PUT | `/api/depenses/{id}` | `DepenseDto` |
| GET | `/api/depenses/total?debut=...&fin=...` | `BigDecimal` |
| GET | `/api/depenses/statistiques?debut=...&fin=...` | `Map` avec `List<DepenseDto>` |
| DELETE | `/api/depenses/{id}` | `204 No Content` |

---

## 🔒 Sécurité

### Ce qui est exposé :
✅ Informations utilisateur (nom, prénom, email, rôle)
✅ Toutes les données d'achats/ventes/dépenses
✅ Champs calculés (estRecent, mois, annee, etc.)

### Ce qui est protégé :
🔒 Mot de passe utilisateur (jamais dans les DTOs)
🔒 Relations JPA complexes (évite lazy loading)

---

## 📊 Exemple de réponse API

### GET /api/achats

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

### GET /api/ventes/statistiques?debut=2025-10-01&fin=2025-10-31

```json
{
  "dateDebut": "2025-10-01",
  "dateFin": "2025-10-31",
  "nombreVentes": 15,
  "chiffreAffaires": 375000.00,
  "ventes": [
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
}
```

---

## 🚀 Utilisation dans Angular

### Service TypeScript

```typescript
export interface UserDto {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: string;
  dateCreation?: string;
}

export interface AchatDto {
  id: number;
  quantite: number;
  nomProduit: string;
  prixUnitaire: number;
  prixTotal: number;
  dateAchat: string;
  fournisseur?: string;
  utilisateur: UserDto;
  estRecent: boolean;
  mois: number;
  annee: number;
}

// Service
this.http.get<AchatDto[]>('http://localhost:8080/api/achats')
  .subscribe(achats => {
    console.log('Achats avec utilisateur:', achats);
    achats.forEach(achat => {
      console.log(`${achat.nomProduit} créé par ${achat.utilisateur.nom}`);
    });
  });
```

---

## ✅ Avantages de cette architecture

1. **Pas de boucles infinies** : Les DTOs coupent les relations bidirectionnelles
2. **Sécurité** : Le mot de passe n'est jamais exposé
3. **Performance** : Pas de lazy loading non désiré
4. **Flexibilité** : Facile d'ajouter des champs calculés
5. **Clarté** : Le front-end sait exactement quelles données attendre
6. **Maintenance** : Facile de modifier la structure sans toucher aux entités

---

## 🔧 Maintenance

### Ajouter un champ à un DTO :

1. Ajouter le champ dans le DTO
2. Mettre à jour la méthode `fromEntity()`
3. Recompiler : `./mvnw clean compile`

### Créer un nouveau DTO :

1. Créer la classe dans `dto/`
2. Ajouter les annotations Lombok (`@Data`, `@Builder`, etc.)
3. Créer une méthode statique `fromEntity()`
4. Mettre à jour le controller correspondant

---

## 📝 Notes importantes

- **Tous les utilisateurs authentifiés** peuvent voir toutes les données (achats, ventes, dépenses)
- Les DTOs incluent **toujours** l'utilisateur qui a créé l'élément
- Les champs calculés sont **automatiquement** inclus dans les DTOs
- Les statistiques retournent des **listes de DTOs**, pas d'entités
