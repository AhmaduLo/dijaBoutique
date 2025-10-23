# 📋 PROMPT pour le développement Frontend - Gestion de Stock

> **Copiez ce prompt et donnez-le à Claude/ChatGPT pour générer le code Angular du système de gestion de stock**

---

## 🎯 OBJECTIF

Créer une interface Angular complète pour gérer le stock des produits d'une boutique de bijoux.

Le backend est **déjà prêt** et fournit une API REST complète pour consulter le stock en temps réel.

---

## 🏗️ ARCHITECTURE BACKEND (Déjà implémentée)

### API de base : `http://localhost:8080/api`

### Endpoints disponibles

#### 1. **GET /api/stock**
Obtenir le stock de tous les produits

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
  }
]
```

#### 2. **GET /api/stock/produit/{nomProduit}**
Obtenir le stock d'un produit spécifique

Exemple : `GET /api/stock/produit/Collier en or`

#### 3. **GET /api/stock/rupture**
Liste des produits en rupture de stock (stock = 0)

#### 4. **GET /api/stock/stock-bas**
Liste des produits avec stock faible (1-9 unités)

#### 5. **GET /api/stock/alertes**
Toutes les alertes (ruptures + stocks bas)

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

#### 6. **GET /api/stock/verifier?nomProduit=xxx&quantite=10**
Vérifier si un produit a un stock suffisant pour une vente

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

#### 7. **GET /api/stock/valeur-totale**
Valeur totale du stock (somme de tous les produits)

**Réponse** :
```json
{
  "valeurTotale": 1500000.00,
  "nombreProduits": 15,
  "details": [...]
}
```

#### 8. **GET /api/stock/resume**
Résumé général du stock

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

## 📊 TYPES/INTERFACES TypeScript nécessaires

### StockDto
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
  statut: StatutStock;
}

export enum StatutStock {
  EN_STOCK = 'EN_STOCK',
  STOCK_BAS = 'STOCK_BAS',
  RUPTURE = 'RUPTURE',
  NEGATIF = 'NEGATIF'
}
```

### AlertesStock
```typescript
export interface AlertesStock {
  ruptures: StockDto[];
  nombreRuptures: number;
  stocksBas: StockDto[];
  nombreStocksBas: number;
  nombreTotalAlertes: number;
}
```

### ResumeStock
```typescript
export interface ResumeStock {
  nombreTotalProduits: number;
  produitsEnStock: number;
  produitsEnRupture: number;
  produitsStockBas: number;
  valeurTotaleStock: number;
}
```

---

## 🎨 COMPOSANTS À CRÉER

### 1. **StockService** (Service Angular)

**Fichier** : `src/app/services/stock.service.ts`

**Fonctionnalités requises** :
- Méthode pour obtenir tous les stocks
- Méthode pour obtenir le stock d'un produit
- Méthode pour obtenir les alertes
- Méthode pour obtenir le résumé
- Méthode pour vérifier la disponibilité d'un produit
- Gestion de l'authentification JWT (Bearer token)

**Exemple de structure** :
```typescript
@Injectable({ providedIn: 'root' })
export class StockService {
  private apiUrl = 'http://localhost:8080/api/stock';

  constructor(private http: HttpClient) {}

  getTousLesStocks(): Observable<StockDto[]> {
    return this.http.get<StockDto[]>(this.apiUrl);
  }

  // ... autres méthodes
}
```

---

### 2. **StockDashboardComponent** (Composant principal)

**Fichier** : `src/app/components/stock-dashboard/stock-dashboard.component.ts`

**Fonctionnalités requises** :
- Afficher le résumé du stock en **cartes/cards** :
  - Nombre total de produits
  - Produits en stock
  - Produits en rupture
  - Produits à stock bas
  - Valeur totale du stock
- Afficher les alertes en haut de page (badges colorés)
- Tableau des stocks avec :
  - Nom du produit
  - Quantité achetée
  - Quantité vendue
  - Stock disponible (avec couleur selon statut)
  - Prix moyen d'achat
  - Prix moyen de vente
  - Valeur du stock
  - Marge unitaire
  - Statut (badge coloré)
- Barre de recherche pour filtrer les produits
- Bouton pour actualiser les données

**Design attendu** :
```
┌─────────────────────────────────────────────────────────┐
│ 📊 GESTION DU STOCK                                     │
├─────────────────────────────────────────────────────────┤
│ ⚠️ 3 produits en rupture | ⚠️ 5 produits à stock bas   │
├─────────────────────────────────────────────────────────┤
│ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐    │
│ │  25   │ │  20   │ │   3   │ │   5   │ │ 1.5M  │    │
│ │Produits│ │En stock│ │Rupture│ │Stock  │ │Valeur │    │
│ └───────┘ └───────┘ └───────┘ │  bas  │ └───────┘    │
│                                └───────┘                │
├─────────────────────────────────────────────────────────┤
│ 🔍 Rechercher un produit: [____________] [🔄 Actualiser]│
├─────────────────────────────────────────────────────────┤
│ TABLEAU DES STOCKS                                      │
│ ┌────────────────────────────────────────────────────┐ │
│ │Produit│Acheté│Vendu│Stock│Prix Achat│Valeur│Statut││ │
│ ├────────────────────────────────────────────────────┤ │
│ │Collier│  50  │ 30  │ 20  │ 15000 XOF│300K  │✅    ││ │
│ │Bracelet│ 20  │ 18  │  2  │  5000 XOF│ 10K  │⚠️    ││ │
│ │Bague  │ 10  │ 10  │  0  │  8000 XOF│  0   │🔴    ││ │
│ └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

**Couleurs des statuts** :
- `EN_STOCK` : Vert (✅)
- `STOCK_BAS` : Orange (⚠️)
- `RUPTURE` : Rouge (🔴)
- `NEGATIF` : Rouge foncé (⚠️⚠️)

---

### 3. **StockAlertsComponent** (Composant d'alertes - Optionnel)

**Fichier** : `src/app/components/stock-alerts/stock-alerts.component.ts`

**Fonctionnalités** :
- Afficher uniquement les produits en rupture et à stock bas
- Liste visuelle avec icônes et couleurs
- Bouton "Réapprovisionner" pour chaque produit (redirection vers achat)

---

### 4. **StockDetailComponent** (Détail produit - Optionnel)

**Fichier** : `src/app/components/stock-detail/stock-detail.component.ts`

**Fonctionnalités** :
- Afficher les détails complets d'un produit
- Graphique de l'évolution du stock
- Historique des achats et ventes
- Actions possibles (réapprovisionner, etc.)

---

## 🎨 DESIGN & STYLE

### Framework CSS recommandé
- **Bootstrap 5** OU **Angular Material** OU **Tailwind CSS**

### Palette de couleurs suggérée
```css
/* Statuts */
--success-color: #28a745;  /* EN_STOCK */
--warning-color: #ffc107;  /* STOCK_BAS */
--danger-color: #dc3545;   /* RUPTURE */
--dark-danger: #721c24;    /* NEGATIF */

/* Thème */
--primary-color: #007bff;
--secondary-color: #6c757d;
--background: #f8f9fa;
```

### Composants visuels à utiliser
- **Cards/Cartes** : Pour les statistiques du résumé
- **Badges** : Pour les statuts et alertes
- **Tableau responsive** : Avec tri et pagination
- **Progress bars** : Pour visualiser le pourcentage de stock
- **Tooltips** : Pour afficher les détails au survol

---

## 🔄 FONCTIONNALITÉS AVANCÉES (Optionnelles)

### 1. Recherche et filtrage
- Barre de recherche en temps réel
- Filtres par statut (EN_STOCK, STOCK_BAS, RUPTURE)
- Tri par colonne (nom, stock, valeur, etc.)

### 2. Pagination
- 10, 25, 50 produits par page
- Navigation page précédente/suivante

### 3. Actualisation automatique
- Timer pour rafraîchir les données toutes les 30 secondes
- Indicateur visuel de chargement

### 4. Export
- Bouton "Exporter en Excel"
- Bouton "Imprimer"

### 5. Graphiques
- Graphique en camembert : Répartition des statuts
- Graphique en barres : Top 10 produits par valeur de stock
- Graphique linéaire : Évolution du stock dans le temps

### 6. Notifications
- Toast/Snackbar pour les nouvelles alertes
- Badge de notification dans la navbar

---

## 📱 RESPONSIVE DESIGN

Le dashboard doit être **responsive** :
- **Desktop** : Tableau complet avec toutes les colonnes
- **Tablette** : Colonnes simplifiées, cartes empilées
- **Mobile** : Vue en cartes, une par produit

---

## 🔐 SÉCURITÉ & AUTHENTIFICATION

### Headers HTTP requis
Tous les appels API doivent inclure le **token JWT** :

```typescript
const headers = new HttpHeaders({
  'Authorization': `Bearer ${localStorage.getItem('token')}`
});

this.http.get(url, { headers });
```

### Gestion des erreurs
- Intercepteur HTTP pour gérer les erreurs 401 (non autorisé)
- Messages d'erreur conviviaux pour l'utilisateur
- Redirection vers login si token expiré

---

## 🧪 TESTS & VALIDATION

### Scénarios à tester

1. **Chargement initial** :
   - Afficher le résumé correctement
   - Afficher la liste des stocks

2. **Recherche** :
   - Filtrer les produits par nom
   - Afficher "Aucun résultat" si vide

3. **Alertes** :
   - Afficher les badges d'alerte si présents
   - Cacher si aucune alerte

4. **Détail produit** :
   - Naviguer vers les détails
   - Afficher toutes les informations

5. **Actualisation** :
   - Bouton refresh fonctionne
   - Indicateur de chargement visible

---

## 📂 STRUCTURE DES FICHIERS

```
src/app/
├── models/
│   ├── stock-dto.model.ts
│   ├── alertes-stock.model.ts
│   └── resume-stock.model.ts
├── services/
│   └── stock.service.ts
├── components/
│   ├── stock-dashboard/
│   │   ├── stock-dashboard.component.ts
│   │   ├── stock-dashboard.component.html
│   │   ├── stock-dashboard.component.css
│   │   └── stock-dashboard.component.spec.ts
│   ├── stock-alerts/
│   │   ├── stock-alerts.component.ts
│   │   ├── stock-alerts.component.html
│   │   └── stock-alerts.component.css
│   └── stock-detail/
│       ├── stock-detail.component.ts
│       ├── stock-detail.component.html
│       └── stock-detail.component.css
└── app-routing.module.ts (ajouter routes)
```

---

## 🚀 ROUTES À AJOUTER

```typescript
const routes: Routes = [
  { path: 'stock', component: StockDashboardComponent },
  { path: 'stock/alertes', component: StockAlertsComponent },
  { path: 'stock/:nomProduit', component: StockDetailComponent }
];
```

---

## 💡 EXEMPLES DE CODE

### Appel API simple
```typescript
chargerStocks() {
  this.stockService.getTousLesStocks().subscribe({
    next: (stocks) => {
      this.stocks = stocks;
      this.stocksFiltres = stocks;
    },
    error: (error) => {
      console.error('Erreur chargement stocks:', error);
      this.afficherErreur('Impossible de charger les stocks');
    }
  });
}
```

### Filtrage en temps réel
```typescript
filtrerProduits(terme: string) {
  this.stocksFiltres = this.stocks.filter(stock =>
    stock.nomProduit.toLowerCase().includes(terme.toLowerCase())
  );
}
```

### Couleur selon statut
```typescript
getColorByStatus(statut: StatutStock): string {
  switch (statut) {
    case StatutStock.EN_STOCK: return 'success';
    case StatutStock.STOCK_BAS: return 'warning';
    case StatutStock.RUPTURE: return 'danger';
    case StatutStock.NEGATIF: return 'danger';
    default: return 'secondary';
  }
}
```

### Badge HTML
```html
<span class="badge bg-{{ getColorByStatus(stock.statut) }}">
  {{ stock.statut }}
</span>
```

---

## ✅ CHECKLIST DE DÉVELOPPEMENT

### Phase 1 : Setup
- [ ] Créer les interfaces TypeScript
- [ ] Créer le StockService
- [ ] Tester les appels API avec Postman/curl

### Phase 2 : Dashboard
- [ ] Créer StockDashboardComponent
- [ ] Afficher le résumé (5 cartes)
- [ ] Afficher les alertes
- [ ] Afficher le tableau des stocks
- [ ] Implémenter la recherche

### Phase 3 : Styles
- [ ] Appliquer les couleurs selon statut
- [ ] Rendre le tableau responsive
- [ ] Ajouter les badges et icônes
- [ ] Tester sur mobile

### Phase 4 : Fonctionnalités avancées
- [ ] Pagination
- [ ] Tri des colonnes
- [ ] Actualisation automatique
- [ ] Export Excel (optionnel)

### Phase 5 : Tests
- [ ] Tester avec données réelles
- [ ] Tester les cas d'erreur
- [ ] Tester la recherche
- [ ] Tester le responsive

---

## 📚 RESSOURCES UTILES

### Documentation Angular
- HttpClient : https://angular.io/guide/http
- Services : https://angular.io/guide/architecture-services
- Routing : https://angular.io/guide/router

### Bibliothèques recommandées
- **ng-bootstrap** : `npm install @ng-bootstrap/ng-bootstrap`
- **Chart.js** (graphiques) : `npm install chart.js ng2-charts`
- **ngx-pagination** : `npm install ngx-pagination`
- **xlsx** (export Excel) : `npm install xlsx`

---

## 🎯 RÉSULTAT ATTENDU

À la fin, vous devez avoir :
1. ✅ Un dashboard complet et visuel du stock
2. ✅ Vue en temps réel des stocks disponibles
3. ✅ Alertes visuelles pour ruptures et stocks bas
4. ✅ Recherche et filtrage des produits
5. ✅ Interface responsive (desktop, tablette, mobile)
6. ✅ Code propre et maintenable

---

## 🚀 COMMANDE À EXÉCUTER

```bash
# Générer les fichiers
ng generate service services/stock
ng generate component components/stock-dashboard
ng generate interface models/stock-dto
ng generate interface models/alertes-stock
ng generate interface models/resume-stock
```

---

## 💬 PROMPT À COPIER-COLLER

**Voici le prompt complet à donner à Claude/ChatGPT** :

---

```
Je développe une application Angular pour une boutique de bijoux.

Le backend est déjà prêt et fournit une API REST pour gérer le stock des produits.

OBJECTIF : Créer une interface complète de gestion de stock avec :
- Dashboard avec résumé (cartes statistiques)
- Tableau des stocks avec recherche et tri
- Alertes visuelles (ruptures, stocks bas)
- Design moderne et responsive

API DISPONIBLE :
- GET /api/stock → Tous les stocks
- GET /api/stock/produit/{nom} → Stock d'un produit
- GET /api/stock/alertes → Alertes (ruptures + stocks bas)
- GET /api/stock/resume → Résumé global

FORMAT DES DONNÉES :
StockDto : {
  nomProduit: string,
  quantiteAchetee: number,
  quantiteVendue: number,
  stockDisponible: number,
  prixMoyenAchat: number,
  prixMoyenVente: number,
  valeurStock: number,
  margeUnitaire: number,
  statut: 'EN_STOCK' | 'STOCK_BAS' | 'RUPTURE' | 'NEGATIF'
}

BESOIN :
1. Créer StockService avec toutes les méthodes d'API
2. Créer StockDashboardComponent avec :
   - 5 cartes de résumé (total produits, en stock, rupture, stock bas, valeur)
   - Badges d'alerte en haut
   - Tableau avec toutes les colonnes du StockDto
   - Barre de recherche
   - Couleurs selon statut (vert, orange, rouge)
3. Design responsive avec Bootstrap ou Angular Material
4. Gestion du token JWT dans les headers

Framework : Angular 18+
Style : Bootstrap 5 ou Angular Material
API : http://localhost:8080/api

Génère tout le code complet (service, component.ts, component.html, component.css, interfaces).
```

---

## 🎉 BON DÉVELOPPEMENT !

Avec ce prompt, Claude ou ChatGPT pourra générer **tout le code nécessaire** pour votre frontend de gestion de stock.

N'hésitez pas à ajuster le prompt selon vos besoins spécifiques (framework CSS préféré, fonctionnalités additionnelles, etc.).
