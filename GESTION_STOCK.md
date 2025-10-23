# 📦 Gestion du Stock - Boutique DijaSaliou

## 🎯 Vue d'ensemble

Le système de gestion de stock calcule automatiquement le stock disponible pour chaque produit en comparant les **achats** et les **ventes**.

### Formule de calcul
```
Stock disponible = Quantité achetée - Quantité vendue
```

### Exemple
- **Collier en or** : 50 achetés, 30 vendus = **20 en stock** ✅
- **Bracelet** : 20 achetés, 25 vendus = **-5 en stock** ⚠️ (alerte !)

---

## 🏗️ Architecture

### 1. StockDto
**Localisation** : `src/main/java/com/example/dijasaliou/dto/StockDto.java`

Structure complète du stock d'un produit :

```json
{
  "nomProduit": "Collier en or",
  "quantiteAchetee": 50,
  "quantiteVendue": 30,
  "stockDisponible": 20,
  "prixMoyenAchat": 15000.00,
  "prixMoyenVente": 25000.00,
  "valeurStock": 300000.00,
  "margeUnitaire": 10000.00,
  "statut": "EN_STOCK"
}
```

#### Statuts possibles
- **EN_STOCK** : Stock disponible (≥ 10 unités)
- **STOCK_BAS** : Stock faible (1-9 unités) ⚠️
- **RUPTURE** : Stock épuisé (0 unité) 🔴
- **NEGATIF** : Plus de ventes que d'achats (< 0) ⚠️⚠️

---

### 2. StockService
**Localisation** : `src/main/java/com/example/dijasaliou/service/StockService.java`

Service qui calcule et gère le stock :

#### Méthodes principales

```java
// Obtenir tous les stocks
List<StockDto> obtenirTousLesStocks()

// Stock d'un produit spécifique
StockDto obtenirStockParNomProduit(String nomProduit)

// Produits en rupture
List<StockDto> obtenirProduitsEnRupture()

// Produits avec stock bas
List<StockDto> obtenirProduitsStockBas()

// Vérifier la disponibilité
boolean verifierStockDisponible(String nomProduit, Integer quantite)

// Valeur totale du stock
BigDecimal obtenirValeurTotaleStock()
```

---

### 3. StockController
**Localisation** : `src/main/java/com/example/dijasaliou/controller/StockController.java`

Endpoints REST pour consulter le stock.

---

## 📊 Endpoints API

### 1. Obtenir tous les stocks
```bash
GET /api/stock
Authorization: Bearer {token}
```

**Réponse** :
```json
[
  {
    "nomProduit": "collier en or",
    "quantiteAchetee": 50,
    "quantiteVendue": 30,
    "stockDisponible": 20,
    "prixMoyenAchat": 15000.00,
    "prixMoyenVente": 25000.00,
    "valeurStock": 300000.00,
    "margeUnitaire": 10000.00,
    "statut": "EN_STOCK"
  },
  {
    "nomProduit": "bracelet",
    "quantiteAchetee": 20,
    "quantiteVendue": 18,
    "stockDisponible": 2,
    "prixMoyenAchat": 5000.00,
    "prixMoyenVente": 8000.00,
    "valeurStock": 10000.00,
    "margeUnitaire": 3000.00,
    "statut": "STOCK_BAS"
  }
]
```

---

### 2. Stock d'un produit spécifique
```bash
GET /api/stock/produit/{nomProduit}
Authorization: Bearer {token}
```

**Exemple** :
```bash
GET /api/stock/produit/Collier en or
```

**Réponse** :
```json
{
  "nomProduit": "collier en or",
  "quantiteAchetee": 50,
  "quantiteVendue": 30,
  "stockDisponible": 20,
  "prixMoyenAchat": 15000.00,
  "prixMoyenVente": 25000.00,
  "valeurStock": 300000.00,
  "margeUnitaire": 10000.00,
  "statut": "EN_STOCK"
}
```

**Note** : Le nom du produit est **insensible à la casse** (majuscules/minuscules)

---

### 3. Produits en rupture de stock
```bash
GET /api/stock/rupture
Authorization: Bearer {token}
```

**Réponse** :
```json
[
  {
    "nomProduit": "bague en argent",
    "quantiteAchetee": 10,
    "quantiteVendue": 10,
    "stockDisponible": 0,
    "statut": "RUPTURE"
  }
]
```

---

### 4. Produits avec stock bas
```bash
GET /api/stock/stock-bas
Authorization: Bearer {token}
```

Retourne les produits avec **1 à 9 unités** en stock.

---

### 5. Toutes les alertes
```bash
GET /api/stock/alertes
Authorization: Bearer {token}
```

**Réponse** :
```json
{
  "ruptures": [...],
  "nombreRuptures": 3,
  "stocksBas": [...],
  "nombreStocksBas": 5,
  "nombreTotalAlertes": 8
}
```

---

### 6. Vérifier la disponibilité
```bash
GET /api/stock/verifier?nomProduit=Collier&quantite=5
Authorization: Bearer {token}
```

**Réponse** :
```json
{
  "nomProduit": "Collier",
  "quantiteDemandee": 5,
  "stockDisponible": 20,
  "disponible": true,
  "message": "Stock suffisant pour cette vente"
}
```

**Réponse (stock insuffisant)** :
```json
{
  "nomProduit": "Bracelet",
  "quantiteDemandee": 10,
  "stockDisponible": 2,
  "disponible": false,
  "message": "Stock insuffisant ! Disponible : 2"
}
```

---

### 7. Valeur totale du stock
```bash
GET /api/stock/valeur-totale
Authorization: Bearer {token}
```

**Réponse** :
```json
{
  "valeurTotale": 1500000.00,
  "nombreProduits": 15,
  "details": [...]
}
```

Calcule : **Σ (Stock disponible × Prix moyen d'achat)**

---

### 8. Résumé général
```bash
GET /api/stock/resume
Authorization: Bearer {token}
```

**Réponse** :
```json
{
  "nombreTotalProduits": 25,
  "produitsEnStock": 20,
  "produitsEnRupture": 3,
  "produitsStockBas": 5,
  "valeurTotaleStock": 1500000.00
}
```

---

## 🔒 Validation automatique des ventes

Le système **vérifie automatiquement le stock** avant chaque vente !

### Fonctionnement

Lorsque vous créez une vente :

```bash
POST /api/ventes?utilisateurId=1
{
  "nomProduit": "Collier en or",
  "quantite": 10,
  "prixUnitaire": 25000
}
```

Le système :
1. ✅ Vérifie que le produit existe dans les achats
2. ✅ Calcule le stock disponible
3. ✅ Compare avec la quantité demandée
4. ❌ **Bloque la vente** si le stock est insuffisant

### Exemple de blocage

**Scénario** :
- Stock disponible : 5 unités
- Vente demandée : 10 unités

**Réponse** :
```json
{
  "error": "Stock insuffisant pour 'Collier en or' ! Disponible : 5, Demandé : 10"
}
```

### Cas particulier

Si un produit n'a **jamais été acheté**, la vente est **autorisée** (pour permettre de vendre des produits sans achat préalable).

Pour changer ce comportement, modifiez `VenteService.java:158-161`.

---

## 🧪 Exemples d'utilisation

### Cas 1 : Consulter le stock avant une vente

```bash
# 1. Vérifier le stock
GET /api/stock/produit/Collier en or

# Réponse : stockDisponible = 20

# 2. Si suffisant, créer la vente
POST /api/ventes?utilisateurId=1
{
  "nomProduit": "Collier en or",
  "quantite": 5,
  ...
}

# 3. Revérifier le stock
GET /api/stock/produit/Collier en or

# Réponse : stockDisponible = 15 (20 - 5)
```

---

### Cas 2 : Identifier les produits à réapprovisionner

```bash
# 1. Voir les alertes
GET /api/stock/alertes

# Réponse : liste des ruptures et stocks bas

# 2. Créer des achats pour réapprovisionner
POST /api/achats?utilisateurId=1
{
  "nomProduit": "Bracelet",
  "quantite": 50,
  "prixUnitaire": 5000
}

# 3. Vérifier le nouveau stock
GET /api/stock/produit/Bracelet

# Réponse : stock mis à jour automatiquement
```

---

### Cas 3 : Dashboard de gestion

```bash
# Obtenir les statistiques globales
GET /api/stock/resume

{
  "nombreTotalProduits": 25,
  "produitsEnStock": 20,
  "produitsEnRupture": 3,
  "produitsStockBas": 5,
  "valeurTotaleStock": 1500000.00
}
```

Utilisez ces données pour créer un **dashboard visuel** dans Angular.

---

## 📈 Calculs automatiques

### 1. Quantité achetée
```
Somme de toutes les quantités dans les achats du produit
```

### 2. Quantité vendue
```
Somme de toutes les quantités dans les ventes du produit
```

### 3. Stock disponible
```
Quantité achetée - Quantité vendue
```

### 4. Prix moyen d'achat
```
Moyenne des prix unitaires d'achat
```

### 5. Prix moyen de vente
```
Moyenne des prix unitaires de vente
```

### 6. Valeur du stock
```
Stock disponible × Prix moyen d'achat
```

### 7. Marge unitaire
```
Prix moyen de vente - Prix moyen d'achat
```

---

## 🎨 Intégration Angular

### Interface TypeScript

```typescript
export interface StockDto {
  nomProduit: string;
  quantiteAchetee: number;
  quantiteVendue: number;
  stockDisponible: number;
  prixMoyenAchat: number;
  prixMoyenVente: number;
  valeurStock: number;
  margeUnitaire: number;
  statut: 'EN_STOCK' | 'STOCK_BAS' | 'RUPTURE' | 'NEGATIF';
}
```

### Service Angular

```typescript
@Injectable()
export class StockService {
  private apiUrl = 'http://localhost:8080/api/stock';

  constructor(private http: HttpClient) {}

  // Obtenir tous les stocks
  getTousLesStocks(): Observable<StockDto[]> {
    return this.http.get<StockDto[]>(this.apiUrl);
  }

  // Stock d'un produit
  getStockProduit(nomProduit: string): Observable<StockDto> {
    return this.http.get<StockDto>(`${this.apiUrl}/produit/${nomProduit}`);
  }

  // Alertes
  getAlertes(): Observable<any> {
    return this.http.get(`${this.apiUrl}/alertes`);
  }

  // Vérifier disponibilité
  verifierStock(nomProduit: string, quantite: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/verifier`, {
      params: { nomProduit, quantite: quantite.toString() }
    });
  }
}
```

### Composant Angular

```typescript
export class StockComponent implements OnInit {
  stocks: StockDto[] = [];
  alertes: any;

  constructor(private stockService: StockService) {}

  ngOnInit() {
    this.chargerStocks();
    this.chargerAlertes();
  }

  chargerStocks() {
    this.stockService.getTousLesStocks().subscribe(
      stocks => {
        this.stocks = stocks;
        console.log('Stocks:', stocks);
      }
    );
  }

  chargerAlertes() {
    this.stockService.getAlertes().subscribe(
      alertes => {
        this.alertes = alertes;
        console.log(`${alertes.nombreTotalAlertes} alertes !`);
      }
    );
  }

  getColorByStatus(statut: string): string {
    switch (statut) {
      case 'EN_STOCK': return 'green';
      case 'STOCK_BAS': return 'orange';
      case 'RUPTURE': return 'red';
      case 'NEGATIF': return 'red';
      default: return 'gray';
    }
  }
}
```

### Template HTML

```html
<div class="stock-dashboard">
  <!-- Alertes -->
  <div class="alerts" *ngIf="alertes">
    <div class="alert alert-danger" *ngIf="alertes.nombreRuptures > 0">
      ⚠️ {{ alertes.nombreRuptures }} produit(s) en rupture !
    </div>
    <div class="alert alert-warning" *ngIf="alertes.nombreStocksBas > 0">
      ⚠️ {{ alertes.nombreStocksBas }} produit(s) à stock bas !
    </div>
  </div>

  <!-- Tableau des stocks -->
  <table class="table">
    <thead>
      <tr>
        <th>Produit</th>
        <th>Acheté</th>
        <th>Vendu</th>
        <th>Stock</th>
        <th>Statut</th>
        <th>Valeur</th>
      </tr>
    </thead>
    <tbody>
      <tr *ngFor="let stock of stocks">
        <td>{{ stock.nomProduit }}</td>
        <td>{{ stock.quantiteAchetee }}</td>
        <td>{{ stock.quantiteVendue }}</td>
        <td>
          <span [style.color]="getColorByStatus(stock.statut)">
            {{ stock.stockDisponible }}
          </span>
        </td>
        <td>
          <span class="badge" [class]="'badge-' + stock.statut">
            {{ stock.statut }}
          </span>
        </td>
        <td>{{ stock.valeurStock | currency:'XOF' }}</td>
      </tr>
    </tbody>
  </table>
</div>
```

---

## ⚙️ Configuration

### Activer/Désactiver la validation de stock

**Fichier** : `VenteService.java`

#### Option 1 : Permettre les ventes sans stock (par défaut)
```java
// Ligne 158-161
if (e.getMessage().contains("Produit non trouvé")) {
    return;  // Autoriser la vente
}
```

#### Option 2 : Bloquer les ventes sans achat préalable
```java
// Commentez les lignes 158-161
// if (e.getMessage().contains("Produit non trouvé")) {
//     return;
// }
// → Lance une exception si le produit n'existe pas
```

---

## 🔍 Cas d'usage avancés

### 1. Prédiction de rupture
Identifiez les produits qui risquent une rupture :

```typescript
produitsARisque = stocks.filter(s =>
  s.stockDisponible > 0 &&
  s.stockDisponible < (s.quantiteVendue / 30 * 7) // Moins d'une semaine de stock
);
```

### 2. Analyse de la marge
Identifiez les produits les plus rentables :

```typescript
produitsRentables = stocks
  .sort((a, b) => b.margeUnitaire - a.margeUnitaire)
  .slice(0, 10); // Top 10
```

### 3. Valeur du stock par catégorie
Si vous ajoutez des catégories aux produits, calculez la valeur par catégorie.

---

## ✅ Points clés

- ✅ **Calcul automatique** : Stock = Achats - Ventes
- ✅ **Validation automatique** : Impossible de vendre sans stock
- ✅ **Alertes en temps réel** : Ruptures et stocks bas
- ✅ **Insensible à la casse** : "Collier" = "collier" = "COLLIER"
- ✅ **Prix moyens** : Calculés automatiquement
- ✅ **Valeur du stock** : Mise à jour en temps réel
- ✅ **API complète** : 8 endpoints pour tous les besoins

---

## 🐛 Dépannage

### Problème : Stock négatif
**Cause** : Plus de ventes que d'achats
**Solution** :
1. Vérifiez les données dans `/api/achats` et `/api/ventes`
2. Créez des achats pour régulariser
3. Le statut sera "NEGATIF" pour vous alerter

### Problème : Produit non trouvé
**Cause** : Le produit n'a jamais été acheté
**Solution** : Créez d'abord un achat, puis consultez le stock

### Problème : Noms de produits différents
**Cause** : "Collier" ≠ "Collier en or"
**Solution** : Utilisez des noms cohérents ou normalisez-les

---

## 📝 Résumé

Le système de gestion de stock :
- 📊 Calcule automatiquement le stock disponible
- 🔒 Valide les ventes pour éviter les ruptures
- ⚠️ Alerte sur les stocks bas et ruptures
- 💰 Calcule la valeur totale du stock
- 📈 Fournit des statistiques détaillées
- 🔄 Se met à jour en temps réel

**Prochaine étape** : Intégrer avec le dashboard Angular ! 🚀
