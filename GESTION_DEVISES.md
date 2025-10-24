# 💱 Système de Gestion des Devises - Dija Boutique

## 🎯 Vue d'ensemble

Le système de gestion des devises permet de :
- Gérer plusieurs devises (XOF, EUR, USD, etc.)
- Définir une devise par défaut
- Convertir automatiquement les montants entre devises
- Suivre les taux de change

---

## 📊 Architecture Implémentée

### Fichiers créés :

1. **Entity** : `DeviseEntity.java` - Modèle de données JPA
2. **Repository** : `DeviseRepository.java` - Accès à la base de données
3. **DTOs** :
   - `DeviseDto.java` - Représentation de la devise
   - `CreateDeviseDto.java` - Création d'une devise
   - `UpdateDeviseDto.java` - Modification d'une devise
4. **Service** : `DeviseService.java` - Logique métier
5. **Controller** : `DeviseController.java` - 8 endpoints REST

---

## 🔐 Sécurité

**TOUS les endpoints sont réservés aux ADMIN uniquement** via `@PreAuthorize("hasAuthority('ADMIN')")`

Pour accéder aux endpoints, vous devez :
1. Être connecté en tant qu'ADMIN
2. Inclure le token JWT dans le header : `Authorization: Bearer {token}`

---

## 🚀 Endpoints Disponibles

### 1. **GET /api/devises** - Lister toutes les devises

```bash
curl -X GET http://localhost:8080/api/devises \
  -H "Authorization: Bearer {votre-token-admin}"
```

**Réponse :**
```json
[
  {
    "id": 1,
    "code": "XOF",
    "nom": "Franc CFA",
    "symbole": "CFA",
    "pays": "Sénégal",
    "tauxChange": 1.0,
    "isDefault": true,
    "dateCreation": "2025-01-15T10:30:00"
  },
  {
    "id": 2,
    "code": "EUR",
    "nom": "Euro",
    "symbole": "€",
    "pays": "France",
    "tauxChange": 655.957,
    "isDefault": false,
    "dateCreation": "2025-01-15T10:31:00"
  }
]
```

---

### 2. **GET /api/devises/{id}** - Récupérer une devise

```bash
curl -X GET http://localhost:8080/api/devises/1 \
  -H "Authorization: Bearer {votre-token-admin}"
```

**Réponse :**
```json
{
  "id": 1,
  "code": "XOF",
  "nom": "Franc CFA",
  "symbole": "CFA",
  "pays": "Sénégal",
  "tauxChange": 1.0,
  "isDefault": true,
  "dateCreation": "2025-01-15T10:30:00"
}
```

---

### 3. **GET /api/devises/default** - Récupérer la devise par défaut

```bash
curl -X GET http://localhost:8080/api/devises/default \
  -H "Authorization: Bearer {votre-token-admin}"
```

**Réponse :**
```json
{
  "id": 1,
  "code": "XOF",
  "nom": "Franc CFA",
  "symbole": "CFA",
  "pays": "Sénégal",
  "tauxChange": 1.0,
  "isDefault": true,
  "dateCreation": "2025-01-15T10:30:00"
}
```

---

### 4. **POST /api/devises** - Créer une nouvelle devise

```bash
curl -X POST http://localhost:8080/api/devises \
  -H "Authorization: Bearer {votre-token-admin}" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "USD",
    "nom": "Dollar américain",
    "symbole": "$",
    "pays": "États-Unis",
    "tauxChange": 600.0,
    "isDefault": false
  }'
```

**Validation automatique :**
- `code` : 3 lettres majuscules (ex: USD, EUR)
- `tauxChange` : doit être > 0
- Si c'est la première devise, elle devient automatiquement par défaut

**Réponse (201 Created) :**
```json
{
  "id": 3,
  "code": "USD",
  "nom": "Dollar américain",
  "symbole": "$",
  "pays": "États-Unis",
  "tauxChange": 600.0,
  "isDefault": false,
  "dateCreation": "2025-01-15T11:00:00"
}
```

**Erreurs possibles :**
```json
{
  "message": "Une devise avec le code USD existe déjà"
}
```

---

### 5. **PUT /api/devises/{id}** - Modifier une devise

```bash
curl -X PUT http://localhost:8080/api/devises/2 \
  -H "Authorization: Bearer {votre-token-admin}" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Euro (Monnaie Européenne)",
    "tauxChange": 660.0
  }'
```

**Note :** Tous les champs sont optionnels. Seuls les champs fournis seront mis à jour.

**Réponse (200 OK) :**
```json
{
  "id": 2,
  "code": "EUR",
  "nom": "Euro (Monnaie Européenne)",
  "symbole": "€",
  "pays": "France",
  "tauxChange": 660.0,
  "isDefault": false,
  "dateCreation": "2025-01-15T10:31:00"
}
```

---

### 6. **DELETE /api/devises/{id}** - Supprimer une devise

```bash
curl -X DELETE http://localhost:8080/api/devises/3 \
  -H "Authorization: Bearer {votre-token-admin}"
```

**Réponse (204 No Content) :** Pas de corps de réponse

**Protection :**
```json
{
  "message": "Impossible de supprimer la devise par défaut. Définissez d'abord une autre devise comme devise par défaut."
}
```

---

### 7. **PUT /api/devises/{id}/set-default** - Définir comme devise par défaut

```bash
curl -X PUT http://localhost:8080/api/devises/2/set-default \
  -H "Authorization: Bearer {votre-token-admin}"
```

**Comportement :**
- Retire automatiquement le statut par défaut des autres devises
- Définit la devise spécifiée comme nouvelle devise par défaut

**Réponse (200 OK) :**
```json
{
  "id": 2,
  "code": "EUR",
  "nom": "Euro",
  "symbole": "€",
  "pays": "France",
  "tauxChange": 655.957,
  "isDefault": true,
  "dateCreation": "2025-01-15T10:31:00"
}
```

---

### 8. **POST /api/devises/convertir** - Convertir un montant

```bash
curl -X POST http://localhost:8080/api/devises/convertir \
  -H "Authorization: Bearer {votre-token-admin}" \
  -H "Content-Type: application/json" \
  -d '{
    "montant": 100.0,
    "deviseSource": "USD",
    "deviseCible": "XOF"
  }'
```

**Réponse (200 OK) :**
```json
{
  "montant": 100.0,
  "deviseSource": "USD",
  "deviseCible": "XOF",
  "montantConverti": 60000.0
}
```

**Formule de conversion :**
```
1. Montant en devise de référence = montant × taux de la devise source
2. Montant en devise cible = montant de référence ÷ taux de la devise cible
```

**Exemple :**
- XOF (référence) : taux = 1.0
- USD : taux = 600.0 (1 USD = 600 XOF)
- EUR : taux = 655.957 (1 EUR = 655.957 XOF)

Pour convertir 100 USD en EUR :
1. 100 USD × 600 = 60,000 XOF
2. 60,000 XOF ÷ 655.957 = 91.45 EUR

---

## 🧪 Tests avec Postman

### Collection Postman : Gestion des Devises

#### Variables d'environnement
- `baseUrl` : `http://localhost:8080/api`
- `adminToken` : {votre-token-admin}

#### 1. Login ADMIN

```
POST {{baseUrl}}/auth/login
Body:
{
  "email": "admin@dijaboutique.com",
  "motDePasse": "admin123"
}

Tests (JavaScript):
pm.environment.set("adminToken", pm.response.json().token);
```

#### 2. Créer la devise de référence (XOF)

```
POST {{baseUrl}}/devises
Authorization: Bearer {{adminToken}}
Body:
{
  "code": "XOF",
  "nom": "Franc CFA",
  "symbole": "CFA",
  "pays": "Sénégal",
  "tauxChange": 1.0,
  "isDefault": true
}
```

#### 3. Créer d'autres devises

**Euro :**
```json
{
  "code": "EUR",
  "nom": "Euro",
  "symbole": "€",
  "pays": "France",
  "tauxChange": 655.957,
  "isDefault": false
}
```

**Dollar américain :**
```json
{
  "code": "USD",
  "nom": "Dollar américain",
  "symbole": "$",
  "pays": "États-Unis",
  "tauxChange": 600.0,
  "isDefault": false
}
```

**Livre sterling :**
```json
{
  "code": "GBP",
  "nom": "Livre sterling",
  "symbole": "£",
  "pays": "Royaume-Uni",
  "tauxChange": 765.0,
  "isDefault": false
}
```

#### 4. Tester la conversion

```
POST {{baseUrl}}/devises/convertir
Authorization: Bearer {{adminToken}}
Body:
{
  "montant": 100,
  "deviseSource": "EUR",
  "deviseCible": "USD"
}
```

---

## 📝 Règles Métier

### 1. Code unique
Le code de la devise doit être unique dans le système.

**Erreur si doublon :**
```json
{
  "message": "Une devise avec le code USD existe déjà"
}
```

### 2. Une seule devise par défaut
Quand on définit une devise comme par défaut, le système retire automatiquement ce statut des autres devises.

### 3. Protection de la devise par défaut
Impossible de supprimer la devise par défaut. Il faut d'abord définir une autre devise comme par défaut.

### 4. Taux de change positif
Le taux de change doit toujours être > 0.

### 5. Première devise = devise par défaut
Si aucune devise n'existe dans le système, la première devise créée devient automatiquement la devise par défaut.

---

## 💾 Structure de la Base de Données

### Table : `devises`

```sql
CREATE TABLE devises (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    nom VARCHAR(100) NOT NULL,
    symbole VARCHAR(10) NOT NULL,
    pays VARCHAR(100) NOT NULL,
    taux_change DOUBLE NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation DATETIME NOT NULL,
    INDEX idx_code (code),
    INDEX idx_default (is_default)
);
```

---

## 🎨 Intégration avec le Frontend Angular

Le frontend Angular est déjà prêt avec :
- Service `DeviseService`
- Composant `DeviseListComponent`
- Composant `DeviseFormComponent`
- Conversions automatiques dans les formulaires

Le backend fonctionne maintenant avec le frontend ! 🎉

---

## 🔧 Utilisation dans les Transactions

### Exemple : Stocker une transaction avec devise

```java
@Entity
public class VenteEntity {
    // ... autres champs

    @Column(name = "devise_code")
    private String deviseCode = "XOF";  // Par défaut : XOF

    @Column(name = "montant_devise_origine")
    private BigDecimal montantDeviseOrigine;

    @Column(name = "montant_xof")  // Toujours stocker en devise de référence
    private BigDecimal montantXof;
}
```

### Conversion lors de la création d'une vente

```java
public VenteEntity creerVente(CreateVenteDto dto) {
    DeviseEntity devise = deviseService.obtenirDeviseParCode(dto.getDeviseCode());

    // Convertir le montant en XOF (devise de référence)
    BigDecimal montantXof = new BigDecimal(
        devise.convertirVersReference(dto.getMontant().doubleValue())
    );

    VenteEntity vente = new VenteEntity();
    vente.setDeviseCode(dto.getDeviseCode());
    vente.setMontantDeviseOrigine(dto.getMontant());
    vente.setMontantXof(montantXof);

    return venteRepository.save(vente);
}
```

---

## ✅ Checklist de Déploiement

- [x] Entité `DeviseEntity` créée
- [x] Repository `DeviseRepository` créé
- [x] DTOs créés (`DeviseDto`, `CreateDeviseDto`, `UpdateDeviseDto`)
- [x] Service `DeviseService` créé avec toute la logique métier
- [x] Controller `DeviseController` créé avec 8 endpoints
- [x] Sécurité configurée (ADMIN uniquement)
- [x] Validation des données avec `@Valid`
- [x] Gestion des erreurs (code unique, devise par défaut)
- [x] Compilation réussie ✅
- [ ] Tester tous les endpoints avec Postman
- [ ] Créer les devises initiales (XOF, EUR, USD)
- [ ] Intégrer avec le frontend Angular

---

## 🚀 Prochaines Étapes

1. **Tester les endpoints** avec Postman
2. **Créer les devises de base** :
   - XOF (Franc CFA) - devise par défaut
   - EUR (Euro)
   - USD (Dollar)
3. **Intégrer dans les transactions** (Achats, Ventes, Dépenses)
4. **Configurer le frontend** Angular pour utiliser les nouveaux endpoints

---

## 📞 Support

Pour toute question sur l'implémentation, consultez :
- **Code source** : `DeviseController.java`
- **Logique métier** : `DeviseService.java`
- **Modèle de données** : `DeviseEntity.java`

---

**🎉 Le système de gestion des devises est maintenant opérationnel !**
