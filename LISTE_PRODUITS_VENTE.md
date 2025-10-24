# 📋 Liste des Produits pour Formulaire de Vente

## 🎯 Objectif

Au lieu de **taper manuellement** le nom du produit lors d'une vente, afficher une **liste déroulante** des produits déjà achetés et disponibles en stock.

---

## 🆕 Nouveaux Endpoints

### 1. **GET /api/stock/produits-disponibles** (Recommandé)

Obtenir la liste complète des produits disponibles avec leurs détails.

**URL** : `http://localhost:8080/api/stock/produits-disponibles`

**Réponse** :
```json
[
  {
    "nomProduit": "bracelet",
    "stockDisponible": 15,
    "prixMoyenVente": 8000.00,
    "statut": "EN_STOCK"
  },
  {
    "nomProduit": "collier en or",
    "stockDisponible": 20,
    "prixMoyenVente": 25000.00,
    "statut": "EN_STOCK"
  },
  {
    "nomProduit": "bague diamant",
    "stockDisponible": 5,
    "prixMoyenVente": 50000.00,
    "statut": "STOCK_BAS"
  }
]
```

**Caractéristiques** :
- ✅ Uniquement les produits avec **stock > 0**
- ✅ Triés par **ordre alphabétique**
- ✅ Inclut le **stock disponible** pour affichage
- ✅ Inclut le **prix moyen de vente** (pour pré-remplir le formulaire)
- ✅ Inclut le **statut** (pour colorer si stock bas)

---

### 2. **GET /api/stock/noms-produits** (Version simplifiée)

Obtenir uniquement les noms des produits disponibles.

**URL** : `http://localhost:8080/api/stock/noms-produits`

**Réponse** :
```json
[
  "bracelet",
  "collier en or",
  "bague diamant"
]
```

**Caractéristiques** :
- ✅ Liste simple de noms
- ✅ Uniquement les produits avec **stock > 0**
- ✅ Triés par **ordre alphabétique**
- ✅ Parfait pour un `<select>` HTML basique

---

## 🎨 Utilisation dans Angular

### Interface TypeScript

```typescript
export interface ProduitDisponible {
  nomProduit: string;
  stockDisponible: number;
  prixMoyenVente: number;
  statut: string;
}
```

---

### Service Angular

```typescript
@Injectable({ providedIn: 'root' })
export class StockService {
  private apiUrl = 'http://localhost:8080/api/stock';

  constructor(private http: HttpClient) {}

  // Option 1 : Produits complets (recommandé)
  getProduitsDisponibles(): Observable<ProduitDisponible[]> {
    return this.http.get<ProduitDisponible[]>(
      `${this.apiUrl}/produits-disponibles`
    );
  }

  // Option 2 : Noms seulement (simple)
  getNomsProduits(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/noms-produits`);
  }
}
```

---

### Composant de Vente

```typescript
export class VenteFormComponent implements OnInit {
  produitsDisponibles: ProduitDisponible[] = [];
  venteForm: FormGroup;

  constructor(
    private stockService: StockService,
    private fb: FormBuilder
  ) {
    this.venteForm = this.fb.group({
      nomProduit: ['', Validators.required],
      quantite: [1, [Validators.required, Validators.min(1)]],
      prixUnitaire: [0, [Validators.required, Validators.min(0)]],
      client: ['']
    });
  }

  ngOnInit() {
    this.chargerProduitsDisponibles();
  }

  chargerProduitsDisponibles() {
    this.stockService.getProduitsDisponibles().subscribe({
      next: (produits) => {
        this.produitsDisponibles = produits;
      },
      error: (error) => {
        console.error('Erreur chargement produits:', error);
      }
    });
  }

  // Pré-remplir le prix quand on sélectionne un produit
  onProduitChange(nomProduit: string) {
    const produit = this.produitsDisponibles.find(
      p => p.nomProduit === nomProduit
    );

    if (produit) {
      // Pré-remplir le prix unitaire avec le prix moyen de vente
      this.venteForm.patchValue({
        prixUnitaire: produit.prixMoyenVente
      });
    }
  }

  // Vérifier le stock avant de soumettre
  onSubmit() {
    const nomProduit = this.venteForm.value.nomProduit;
    const quantite = this.venteForm.value.quantite;

    const produit = this.produitsDisponibles.find(
      p => p.nomProduit === nomProduit
    );

    if (produit && quantite > produit.stockDisponible) {
      alert(`Stock insuffisant ! Disponible : ${produit.stockDisponible}`);
      return;
    }

    // Soumettre la vente...
  }
}
```

---

### Template HTML

#### Option 1 : Select simple avec noms

```html
<form [formGroup]="venteForm" (ngSubmit)="onSubmit()">
  <!-- Liste déroulante des produits -->
  <div class="form-group">
    <label>Produit *</label>
    <select
      formControlName="nomProduit"
      class="form-control"
      (change)="onProduitChange($event.target.value)">
      <option value="">-- Sélectionnez un produit --</option>
      <option *ngFor="let produit of produitsDisponibles"
              [value]="produit.nomProduit">
        {{ produit.nomProduit }} (Stock: {{ produit.stockDisponible }})
      </option>
    </select>
  </div>

  <!-- Quantité -->
  <div class="form-group">
    <label>Quantité *</label>
    <input type="number" formControlName="quantite" class="form-control">
  </div>

  <!-- Prix unitaire (pré-rempli automatiquement) -->
  <div class="form-group">
    <label>Prix unitaire *</label>
    <input type="number" formControlName="prixUnitaire" class="form-control">
  </div>

  <!-- Client -->
  <div class="form-group">
    <label>Client</label>
    <input type="text" formControlName="client" class="form-control">
  </div>

  <button type="submit" class="btn btn-primary">Créer la vente</button>
</form>
```

---

#### Option 2 : Select avec détails visuels

```html
<form [formGroup]="venteForm" (ngSubmit)="onSubmit()">
  <div class="form-group">
    <label>Produit *</label>
    <select
      formControlName="nomProduit"
      class="form-control"
      (change)="onProduitChange($event.target.value)">
      <option value="">-- Sélectionnez un produit --</option>

      <option
        *ngFor="let produit of produitsDisponibles"
        [value]="produit.nomProduit"
        [class.text-warning]="produit.statut === 'STOCK_BAS'"
        [class.text-success]="produit.statut === 'EN_STOCK'">
        {{ produit.nomProduit }}
        ({{ produit.stockDisponible }} en stock - {{ produit.prixMoyenVente | currency:'XOF' }})
      </option>
    </select>

    <!-- Indication visuelle du stock -->
    <small class="form-text text-muted" *ngIf="venteForm.value.nomProduit">
      <ng-container *ngFor="let p of produitsDisponibles">
        <span *ngIf="p.nomProduit === venteForm.value.nomProduit">
          <span [class.text-danger]="p.statut === 'STOCK_BAS'"
                [class.text-success]="p.statut === 'EN_STOCK'">
            ● {{ p.stockDisponible }} unités disponibles
          </span>
        </span>
      </ng-container>
    </small>
  </div>

  <!-- Reste du formulaire... -->
</form>
```

---

#### Option 3 : Autocomplete (Angular Material)

```html
<form [formGroup]="venteForm" (ngSubmit)="onSubmit()">
  <mat-form-field>
    <mat-label>Produit</mat-label>
    <input
      type="text"
      matInput
      formControlName="nomProduit"
      [matAutocomplete]="auto">

    <mat-autocomplete #auto="matAutocomplete" (optionSelected)="onProduitChange($event.option.value)">
      <mat-option
        *ngFor="let produit of produitsDisponibles"
        [value]="produit.nomProduit">
        <span class="produit-name">{{ produit.nomProduit }}</span>
        <span class="produit-stock"
              [class.text-warning]="produit.statut === 'STOCK_BAS'">
          ({{ produit.stockDisponible }} en stock)
        </span>
      </mat-option>
    </mat-autocomplete>
  </mat-form-field>

  <!-- Reste du formulaire... -->
</form>
```

---

## 🎨 Améliorations UX

### 1. Pré-remplissage automatique du prix

Quand l'utilisateur sélectionne un produit, pré-remplir le prix avec le **prix moyen de vente** :

```typescript
onProduitChange(nomProduit: string) {
  const produit = this.produitsDisponibles.find(
    p => p.nomProduit === nomProduit
  );

  if (produit) {
    this.venteForm.patchValue({
      prixUnitaire: produit.prixMoyenVente
    });
  }
}
```

---

### 2. Validation du stock en temps réel

```typescript
// Ajouter un validateur personnalisé
quantiteValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const nomProduit = this.venteForm?.get('nomProduit')?.value;
    const quantite = control.value;

    if (!nomProduit || !quantite) return null;

    const produit = this.produitsDisponibles.find(
      p => p.nomProduit === nomProduit
    );

    if (produit && quantite > produit.stockDisponible) {
      return {
        stockInsuffisant: {
          disponible: produit.stockDisponible,
          demande: quantite
        }
      };
    }

    return null;
  };
}
```

```html
<!-- Afficher l'erreur -->
<div *ngIf="venteForm.get('quantite')?.hasError('stockInsuffisant')"
     class="alert alert-danger">
  Stock insuffisant ! Disponible :
  {{ venteForm.get('quantite')?.errors?.['stockInsuffisant'].disponible }}
</div>
```

---

### 3. Badge de statut visuel

```html
<option *ngFor="let produit of produitsDisponibles" [value]="produit.nomProduit">
  {{ produit.nomProduit }}

  <!-- Badge selon le statut -->
  <span *ngIf="produit.statut === 'STOCK_BAS'" class="badge bg-warning">
    ⚠️ Stock bas
  </span>
  <span *ngIf="produit.statut === 'EN_STOCK'" class="badge bg-success">
    ✅ En stock
  </span>

  ({{ produit.stockDisponible }} unités)
</option>
```

---

### 4. Filtre de recherche

```typescript
produitsDisponibles: ProduitDisponible[] = [];
produitsFiltres: ProduitDisponible[] = [];
searchTerm: string = '';

filtrerProduits() {
  this.produitsFiltres = this.produitsDisponibles.filter(p =>
    p.nomProduit.toLowerCase().includes(this.searchTerm.toLowerCase())
  );
}
```

```html
<!-- Barre de recherche -->
<input
  type="text"
  [(ngModel)]="searchTerm"
  (ngModelChange)="filtrerProduits()"
  placeholder="Rechercher un produit..."
  class="form-control mb-2">

<!-- Liste filtrée -->
<select formControlName="nomProduit" class="form-control">
  <option *ngFor="let produit of produitsFiltres" [value]="produit.nomProduit">
    {{ produit.nomProduit }} ({{ produit.stockDisponible }})
  </option>
</select>
```

---

## 🧪 Test des Endpoints

### Test avec cURL

```bash
# Produits complets
curl http://localhost:8080/api/stock/produits-disponibles \
  -H "Authorization: Bearer {votre-token}"

# Noms seulement
curl http://localhost:8080/api/stock/noms-produits \
  -H "Authorization: Bearer {votre-token}"
```

---

### Test avec Postman

1. **GET** `http://localhost:8080/api/stock/produits-disponibles`
2. Headers : `Authorization: Bearer {token}`
3. Send
4. Vous devriez voir la liste des produits avec stock > 0

---

## 📊 Exemple de données

### Scénario
- Collier en or : 50 achetés, 30 vendus = **20 en stock**
- Bracelet : 20 achetés, 18 vendus = **2 en stock** (STOCK_BAS)
- Bague : 10 achetés, 10 vendus = **0 en stock** (n'apparaît pas)

### Réponse de l'API
```json
[
  {
    "nomProduit": "bracelet",
    "stockDisponible": 2,
    "prixMoyenVente": 8000.00,
    "statut": "STOCK_BAS"
  },
  {
    "nomProduit": "collier en or",
    "stockDisponible": 20,
    "prixMoyenVente": 25000.00,
    "statut": "EN_STOCK"
  }
]
```

**Note** : "Bague" n'apparaît pas car stock = 0

---

## ✅ Avantages de cette approche

| Avant | Après |
|-------|-------|
| ❌ Saisie manuelle (erreurs de frappe) | ✅ Liste déroulante (pas d'erreur) |
| ❌ Risque de vendre un produit inexistant | ✅ Uniquement les produits en stock |
| ❌ Pas d'info sur le stock | ✅ Stock affiché directement |
| ❌ Prix à saisir manuellement | ✅ Prix pré-rempli automatiquement |
| ❌ Pas d'alerte sur stock bas | ✅ Indication visuelle (couleurs) |

---

## 🎯 Résumé

**2 nouveaux endpoints** pour faciliter la création de ventes :

1. **`GET /api/stock/produits-disponibles`** (recommandé)
   - Produits complets avec détails
   - Stock, prix moyen, statut

2. **`GET /api/stock/noms-produits`** (simple)
   - Uniquement les noms
   - Pour `<select>` basique

**Utilisez-les dans votre formulaire Angular** pour :
- ✅ Afficher une liste déroulante
- ✅ Pré-remplir le prix
- ✅ Valider le stock
- ✅ Améliorer l'UX

---

## 🚀 Prochaines étapes

1. Compiler le backend : `./mvnw clean compile`
2. Tester les endpoints avec Postman
3. Intégrer dans le formulaire de vente Angular
4. Profiter de l'expérience utilisateur améliorée ! 🎉
