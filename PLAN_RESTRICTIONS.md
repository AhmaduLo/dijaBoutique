# Restrictions par Plan d'Abonnement

Ce document décrit les fonctionnalités disponibles selon le plan d'abonnement.

## Plans Disponibles

### 1. Plan GRATUIT
- **Statut** : Non actif (compte créé mais pas encore payé)
- **Accès** : Aucun accès à l'application
- **Message** : "Veuillez souscrire à un abonnement pour accéder à l'application"

### 2. Plan BASIC (6 555 CFA / 9,99€ par mois)
**✅ Fonctionnalités incluses :**
- Gestion des achats (CRUD)
- Gestion des ventes (CRUD)
- Gestion des dépenses (CRUD)
- Gestion du stock
- Tableaux de bord
- **Export des rapports globaux** (achats + ventes + dépenses combinés)
- Maximum 3 utilisateurs

**❌ Fonctionnalités bloquées :**
- ❌ Export individuel des achats (Excel/PDF)
- ❌ Export individuel des ventes (Excel/PDF)
- ❌ Export individuel des dépenses (Excel/PDF)

**Message d'erreur pour les exports individuels :**
> "Cette fonctionnalité est réservée aux plans Premium, Entreprise. Votre plan actuel (Plan Basic) ne permet pas d'exporter les données individuellement. Vous pouvez cependant exporter les rapports globaux depuis la page des rapports. Veuillez mettre à jour votre abonnement pour accéder aux exports individuels."

### 3. Plan PREMIUM (10 000 CFA / 15,24€ par mois)
**✅ Toutes les fonctionnalités du plan BASIC +**
- ✅ Export individuel des achats (Excel/PDF)
- ✅ Export individuel des ventes (Excel/PDF)
- ✅ Export individuel des dépenses (Excel/PDF)
- Maximum 10 utilisateurs
- Support prioritaire

### 4. Plan ENTREPRISE (15 000 CFA / 22,87€ par mois)
**✅ Toutes les fonctionnalités du plan PREMIUM +**
- Utilisateurs illimités
- Support dédié
- Fonctionnalités avancées futures

---

## Implémentation Technique

### 1. Annotation @RequiresPlan

Pour restreindre une fonctionnalité à certains plans, utilisez l'annotation `@RequiresPlan` :

```java
@GetMapping("/export/excel")
@RequiresPlan(plans = {TenantEntity.Plan.PREMIUM, TenantEntity.Plan.ENTREPRISE})
public ResponseEntity<byte[]> exporterAchatsExcel() {
    // Cette méthode est accessible uniquement aux plans PREMIUM et ENTREPRISE
    // Les utilisateurs BASIC recevront une erreur 403
}
```

### 2. Message personnalisé

Vous pouvez personnaliser le message d'erreur :

```java
@GetMapping("/export/excel")
@RequiresPlan(
    plans = {TenantEntity.Plan.PREMIUM, TenantEntity.Plan.ENTREPRISE},
    message = "L'export Excel des achats est réservé aux plans Premium et Entreprise. Passez à un plan supérieur pour débloquer cette fonctionnalité."
)
public ResponseEntity<byte[]> exporterAchatsExcel() {
    // ...
}
```

### 3. Endpoints à protéger (FUTURS)

Quand vous implémenterez les exports, appliquez les restrictions suivantes :

#### AchatController
```java
// ❌ BASIC - ✅ PREMIUM - ✅ ENTREPRISE
@GetMapping("/export/excel")
@RequiresPlan(plans = {TenantEntity.Plan.PREMIUM, TenantEntity.Plan.ENTREPRISE})
public ResponseEntity<byte[]> exporterAchatsExcel() { ... }

@GetMapping("/export/pdf")
@RequiresPlan(plans = {TenantEntity.Plan.PREMIUM, TenantEntity.Plan.ENTREPRISE})
public ResponseEntity<byte[]> exporterAchatsPdf() { ... }
```

#### VenteController
```java
// ❌ BASIC - ✅ PREMIUM - ✅ ENTREPRISE
@GetMapping("/export/excel")
@RequiresPlan(plans = {TenantEntity.Plan.PREMIUM, TenantEntity.Plan.ENTREPRISE})
public ResponseEntity<byte[]> exporterVentesExcel() { ... }

@GetMapping("/export/pdf")
@RequiresPlan(plans = {TenantEntity.Plan.PREMIUM, TenantEntity.Plan.ENTREPRISE})
public ResponseEntity<byte[]> exporterVentesPdf() { ... }
```

#### DepenseController
```java
// ❌ BASIC - ✅ PREMIUM - ✅ ENTREPRISE
@GetMapping("/export/excel")
@RequiresPlan(plans = {TenantEntity.Plan.PREMIUM, TenantEntity.Plan.ENTREPRISE})
public ResponseEntity<byte[]> exporterDepensesExcel() { ... }

@GetMapping("/export/pdf")
@RequiresPlan(plans = {TenantEntity.Plan.PREMIUM, TenantEntity.Plan.ENTREPRISE})
public ResponseEntity<byte[]> exporterDepensesPdf() { ... }
```

#### RapportController (si créé)
```java
// ✅ BASIC - ✅ PREMIUM - ✅ ENTREPRISE (PAS DE RESTRICTION)
@GetMapping("/export/global/excel")
public ResponseEntity<byte[]> exporterRapportGlobalExcel() { ... }

@GetMapping("/export/global/pdf")
public ResponseEntity<byte[]> exporterRapportGlobalPdf() { ... }
```

---

## Gestion des Erreurs

L'aspect `PlanRestrictionAspect` intercepte automatiquement les méthodes annotées et lance une `PlanRestrictionException` si le plan n'est pas autorisé.

Pour gérer cette exception dans votre `GlobalExceptionHandler`, ajoutez :

```java
@ExceptionHandler(PlanRestrictionAspect.PlanRestrictionException.class)
public ResponseEntity<Map<String, String>> handlePlanRestriction(
        PlanRestrictionAspect.PlanRestrictionException ex) {

    Map<String, String> error = new HashMap<>();
    error.put("error", "Plan insuffisant");
    error.put("message", ex.getMessage());
    error.put("code", "PLAN_RESTRICTION");

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
}
```

---

## Frontend

Le frontend doit :

1. **Récupérer le plan actuel** via `GET /api/payment/subscription`
2. **Cacher/Afficher les boutons d'export** selon le plan
3. **Afficher un badge "Premium" ou "Entreprise"** sur les fonctionnalités restreintes
4. **Proposer une mise à niveau** avec un bouton "Passer à Premium"

Exemple Angular :

```typescript
// Dans le composant
subscription: any;
isPremiumOrEnterprise: boolean = false;

ngOnInit() {
  this.subscriptionService.getStatus().subscribe(data => {
    this.subscription = data;
    this.isPremiumOrEnterprise =
      data.plan === 'PREMIUM' || data.plan === 'ENTREPRISE';
  });
}
```

```html
<!-- Dans le template -->
<button *ngIf="isPremiumOrEnterprise" (click)="exporterExcel()">
  📊 Exporter Excel
</button>

<button *ngIf="!isPremiumOrEnterprise"
        (click)="showUpgradeModal()"
        class="btn-disabled">
  📊 Exporter Excel
  <span class="badge badge-premium">Premium</span>
</button>
```

---

## Résumé Visuel

| Fonctionnalité | BASIC | PREMIUM | ENTREPRISE |
|----------------|-------|---------|------------|
| Gestion CRUD | ✅ | ✅ | ✅ |
| Tableaux de bord | ✅ | ✅ | ✅ |
| Export rapports globaux | ✅ | ✅ | ✅ |
| Export achats individuels | ❌ | ✅ | ✅ |
| Export ventes individuelles | ❌ | ✅ | ✅ |
| Export dépenses individuelles | ❌ | ✅ | ✅ |
| Max utilisateurs | 3 | 10 | ∞ |
| Prix mensuel (EUR) | 9,99€ | 15,24€ | 22,87€ |
| Prix mensuel (CFA) | 6 555 | 10 000 | 15 000 |

---

## Notes Importantes

1. **Plan GRATUIT** : Bloque totalement l'accès à l'application (géré par `SubscriptionExpirationFilter`)
2. **Plan BASIC** : Accès complet sauf exports individuels (géré par `@RequiresPlan`)
3. **Plans PREMIUM/ENTREPRISE** : Accès total

4. Les restrictions sont appliquées **côté backend** avec Spring AOP
5. Le frontend doit **également** cacher les boutons pour une meilleure UX
6. En cas de tentative d'accès non autorisé, l'utilisateur reçoit un **403 Forbidden** avec message explicite
