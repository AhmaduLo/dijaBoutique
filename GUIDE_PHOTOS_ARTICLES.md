# Guide : Ajouter des Photos aux Articles

## 📸 Pourquoi Photo + Nom ?

### Comparaison des Approches

| Fonctionnalité | Photo seule | Nom seul | **Photo + Nom** ✨ |
|----------------|-------------|----------|-------------------|
| Identification visuelle rapide | ✅ | ❌ | ✅ |
| Recherche textuelle | ❌ | ✅ | ✅ |
| Exports (PDF/Excel) | ❌ | ✅ | ✅ |
| Inventaire physique | ✅ | ❌ | ✅ |
| Accessibilité | ❌ | ✅ | ✅ |
| Expérience utilisateur | ⚠️ | ⚠️ | ✅ Excellent |

### Cas d'Usage Concrets

#### Scénario 1 : Vente au comptoir
```
❌ SANS PHOTO :
Client: "Je veux du riz"
Vendeur: "Quel type ? On a le riz parfumé, le riz brisé, le riz jasmin..."
→ Perte de temps, risque d'erreur

✅ AVEC PHOTO + NOM :
Vendeur montre l'écran avec photos
Client: "Celui-là !" (montre la photo)
→ Vente rapide et sans erreur
```

#### Scénario 2 : Inventaire
```
❌ SANS PHOTO :
- Liste : "Huile végétale 5L"
- Vous voyez 3 bidons différents dans le stock
- Lequel compter ? 🤔

✅ AVEC PHOTO + NOM :
- Vous comparez la photo avec le bidon physique
- Identification immédiate ✅
```

#### Scénario 3 : Formation d'un nouveau vendeur
```
❌ SANS PHOTO :
- Il doit mémoriser tous les noms
- Risque de confusion entre produits similaires

✅ AVEC PHOTO + NOM :
- Il reconnaît visuellement les produits
- Formation 3x plus rapide
```

---

## 🏗️ Architecture Technique

### Structure de Stockage des Photos

```
D:\boutique dijaSaliou\dijaSaliou\
├── uploads/
│   └── photos/
│       ├── {tenant_uuid}/
│       │   ├── achats/
│       │   │   ├── 2024-01-15_sac-riz-50kg_abc123.jpg
│       │   │   ├── 2024-01-16_huile-5l_def456.jpg
│       │   │   └── ...
│       │   ├── ventes/
│       │   └── produits/
```

**Avantages de cette structure :**
- ✅ Isolation par tenant (multi-tenant)
- ✅ Organisation par type (achats, ventes, produits)
- ✅ Noms de fichiers uniques avec timestamp
- ✅ Facile à sauvegarder/restaurer

### Format des Fichiers

**Formats acceptés :**
- ✅ JPG/JPEG (recommandé pour photos)
- ✅ PNG (recommandé pour logos/images nettes)
- ✅ WEBP (moderne, plus léger)

**Taille maximale :**
- 5 MB par photo (suffisant pour une bonne qualité)

**Optimisation automatique :**
- Redimensionnement à 800x800px maximum
- Compression qualité 85%
- Conversion en JPEG si nécessaire

---

## 📋 Implémentation

### Étape 1 : Modifier AchatEntity

Ajout du champ `photoUrl` dans la table `achats` :

```java
@Column(name = "photo_url", length = 500)
private String photoUrl;  // URL relative : /api/files/photos/{tenant_uuid}/achats/photo.jpg
```

**Pourquoi URL relative ?**
- Fonctionne en développement ET en production
- Pas besoin de changer le code lors du déploiement
- Le frontend construit l'URL complète

### Étape 2 : Service de Gestion des Fichiers

Créer `FileStorageService` pour :
- ✅ Uploader une photo
- ✅ Redimensionner/optimiser l'image
- ✅ Générer un nom de fichier unique
- ✅ Stocker dans le dossier du tenant
- ✅ Supprimer une photo
- ✅ Récupérer une photo

### Étape 3 : Controller d'Upload

```
POST /api/files/upload
→ Upload une photo et retourne l'URL

GET /api/files/photos/{tenantUuid}/achats/{filename}
→ Récupère une photo

DELETE /api/files/photos/{tenantUuid}/achats/{filename}
→ Supprime une photo
```

### Étape 4 : Modifier AchatController

```java
POST /api/achats
Body: {
  "nomProduit": "Sac de riz 50kg",
  "quantite": 10,
  "prixUnitaire": 25000,
  "photoUrl": "/api/files/photos/{tenant_uuid}/achats/2024-01-15_sac-riz_abc123.jpg"  // Optionnel
}
```

---

## 🎨 Interface Utilisateur (Frontend)

### Formulaire d'Ajout d'Achat

```
┌─────────────────────────────────────────┐
│  📦 Nouvel Achat                        │
├─────────────────────────────────────────┤
│                                         │
│  📸 Photo du produit (optionnel)       │
│  ┌─────────────────────────────────┐   │
│  │                                 │   │
│  │      [Cliquer pour ajouter]     │   │
│  │      ou glisser-déposer         │   │
│  │                                 │   │
│  └─────────────────────────────────┘   │
│                                         │
│  📝 Nom du produit *                    │
│  ┌─────────────────────────────────┐   │
│  │ Sac de riz 50kg                 │   │
│  └─────────────────────────────────┘   │
│                                         │
│  📦 Quantité *                          │
│  ┌─────────────────────────────────┐   │
│  │ 10                              │   │
│  └─────────────────────────────────┘   │
│                                         │
│  💰 Prix unitaire (CFA) *               │
│  ┌─────────────────────────────────┐   │
│  │ 25000                           │   │
│  └─────────────────────────────────┘   │
│                                         │
│  💵 Prix de vente suggéré (CFA)        │
│  ┌─────────────────────────────────┐   │
│  │ 30000                           │   │
│  └─────────────────────────────────┘   │
│                                         │
│  🏢 Fournisseur                         │
│  ┌─────────────────────────────────┐   │
│  │ Ets Diop & Fils                 │   │
│  └─────────────────────────────────┘   │
│                                         │
│     [Annuler]      [Enregistrer]       │
└─────────────────────────────────────────┘
```

### Liste des Achats avec Photos

```
┌────────────────────────────────────────────────────────────┐
│  📋 Historique des Achats                                  │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌──────┬──────────────────────┬──────┬────────┬─────────┐│
│  │ 📸   │ Produit              │ Qté  │ P.U    │ Total   ││
│  ├──────┼──────────────────────┼──────┼────────┼─────────┤│
│  │ [🌾] │ Sac de riz 50kg      │ 10   │ 25000  │ 250000  ││
│  │ [🛢️] │ Huile végétale 5L    │ 20   │ 8500   │ 170000  ││
│  │ [📦] │ Sucre en morceaux 1kg│ 50   │ 1200   │ 60000   ││
│  │ [🥫] │ Concentré de tomate  │ 100  │ 450    │ 45000   ││
│  └──────┴──────────────────────┴──────┴────────┴─────────┘│
│                                                            │
│  💡 Astuce: Cliquez sur une photo pour l'agrandir         │
└────────────────────────────────────────────────────────────┘
```

---

## ⚙️ Configuration

### application.properties

```properties
# Configuration du stockage des fichiers
file.upload.dir=D:/boutique dijaSaliou/dijaSaliou/uploads/photos
file.max-size=5MB
file.allowed-extensions=jpg,jpeg,png,webp

# Configuration des images
image.max-width=800
image.max-height=800
image.compression-quality=0.85
```

---

## 🔒 Sécurité

### Règles de Sécurité

1. **Isolation Multi-Tenant**
   - Chaque tenant ne peut accéder qu'à SES photos
   - Vérification du `tenant_uuid` dans le JWT

2. **Validation des Fichiers**
   - ✅ Extension vérifiée (pas de .exe, .php, etc.)
   - ✅ Type MIME vérifié (vraie image, pas un script déguisé)
   - ✅ Taille maximale respectée

3. **Noms de Fichiers Sécurisés**
   - Pas de caractères spéciaux dangereux
   - UUID généré pour éviter les collisions
   - Timestamp pour éviter le cache

4. **Permissions d'Accès**
   - Seul l'ADMIN peut uploader/supprimer
   - Les USERS peuvent seulement voir

---

## 📊 Base de Données : Migration

### Script SQL à Exécuter

```sql
-- Ajouter la colonne photo_url dans la table achats
ALTER TABLE achats
ADD COLUMN photo_url VARCHAR(500);

-- Ajouter un index pour améliorer les performances
CREATE INDEX idx_achat_photo ON achats(photo_url);

-- Faire pareil pour les ventes (si besoin)
ALTER TABLE ventes
ADD COLUMN photo_url VARCHAR(500);

CREATE INDEX idx_vente_photo ON ventes(photo_url);

-- Faire pareil pour les dépenses (si besoin)
ALTER TABLE depenses
ADD COLUMN photo_url VARCHAR(500);

CREATE INDEX idx_depense_photo ON depenses(photo_url);
```

---

## 🎯 Bonnes Pratiques

### 1. Photo + Nom = Obligatoire ?

**Recommandation :**
- ✅ **Nom : OBLIGATOIRE**
- ⚠️ **Photo : OPTIONNEL (mais fortement recommandé)**

**Pourquoi ?**
- Vous pouvez commencer sans photos et les ajouter progressivement
- Certains produits n'ont pas besoin de photo (services, frais généraux)
- Flexibilité pour l'utilisateur

### 2. Quand Prendre la Photo ?

**Option 1 : Photo existante**
- Télécharger depuis la galerie du téléphone
- Rapide pour les produits déjà en stock

**Option 2 : Prendre une nouvelle photo**
- Utiliser l'appareil photo directement
- Pratique lors de la réception de marchandises

**Option 3 : Scanner le code-barres + photo**
- Scanner le code-barres pour identifier
- Proposer de prendre une photo si manquante

### 3. Optimisation du Stockage

**Compression intelligente :**
- Photos > 1MB → Compression automatique
- Résolution > 800x800px → Redimensionnement
- Format PNG → Conversion en JPEG (plus léger)

**Nettoyage automatique :**
- Supprimer les photos orphelines (achat supprimé)
- Archiver les photos des achats > 1 an
- Proposer un outil de nettoyage dans l'admin

### 4. Accessibilité

**Balises alt pour les images :**
```html
<img src="/api/files/photos/..."
     alt="Sac de riz 50kg - Photo du produit" />
```

**Mode liste/grille :**
- Grille : Affichage visuel avec grandes photos
- Liste : Affichage compact avec petites icônes

---

## 📱 Expérience Mobile

### Capture Photo depuis Mobile

```
┌─────────────────────┐
│  📦 Nouvel Achat    │
├─────────────────────┤
│                     │
│  Photo du produit   │
│  ┌───────────────┐  │
│  │               │  │
│  │   📸 Photo    │  │
│  │   📁 Galerie  │  │
│  │               │  │
│  └───────────────┘  │
│                     │
│  Nom: ____________  │
│  Qté: ___  P.U: ___ │
│                     │
│     [Enregistrer]   │
└─────────────────────┘
```

---

## 🚀 Plan d'Implémentation Progressif

### Phase 1 : MVP (Minimum Viable Product)
1. Ajouter le champ `photoUrl` dans `AchatEntity`
2. Créer le `FileStorageService` basique
3. Créer l'endpoint d'upload
4. Modifier le formulaire frontend pour accepter une photo

**Temps estimé : 4-6 heures**

### Phase 2 : Optimisations
1. Compression automatique des images
2. Redimensionnement intelligent
3. Prévisualisation avant upload
4. Drag & drop

**Temps estimé : 3-4 heures**

### Phase 3 : Fonctionnalités Avancées
1. Capture photo depuis mobile
2. Recadrage de l'image
3. Filtres et améliorations
4. Galerie de photos réutilisables

**Temps estimé : 6-8 heures**

---

## ❓ FAQ

### Q1 : Photo obligatoire ou optionnelle ?
**R :** Optionnelle. Le nom reste obligatoire. La photo améliore l'UX mais n'est pas critique.

### Q2 : Où stocker les photos ? Base de données ou fichiers ?
**R :** Fichiers sur disque. Plus performant, plus simple, moins cher que base64 en DB.

### Q3 : Que se passe-t-il si je supprime un achat ?
**R :** La photo est automatiquement supprimée (nettoyage via `@PreRemove` lifecycle).

### Q4 : Puis-je réutiliser une photo pour plusieurs achats ?
**R :** Oui ! Créer un catalogue de produits avec photos. Lors d'un nouvel achat, sélectionner le produit existant.

### Q5 : Comment gérer les photos sur mobile avec une connexion lente ?
**R :** Compression côté client avant upload. Photo de 3MB réduite à 300KB avant envoi.

---

## 📚 Prochaines Étapes

1. **Implémenter le service de stockage** ([FileStorageService.java](src/main/java/com/example/dijasaliou/service/FileStorageService.java))
2. **Créer le controller d'upload** ([FileUploadController.java](src/main/java/com/example/dijasaliou/controller/FileUploadController.java))
3. **Modifier AchatEntity** pour ajouter `photoUrl`
4. **Modifier le frontend** pour gérer l'upload de photos
5. **Tester** avec différents formats et tailles d'images

---

## 📞 Support

Si vous avez des questions sur l'implémentation, consultez :
- [FLUX_INSCRIPTION_PAIEMENT.md](FLUX_INSCRIPTION_PAIEMENT.md) pour comprendre l'architecture
- [PLAN_RESTRICTIONS.md](PLAN_RESTRICTIONS.md) pour les fonctionnalités par plan

**Voulez-vous que je vous aide à implémenter cette fonctionnalité maintenant ?**
