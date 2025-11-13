# 📸 Guide d'Utilisation : Photos de Produits

## ✅ Ce qui a été implémenté

### 1. Backend (Spring Boot)

✅ **AchatEntity.java** - Ajout du champ `photoUrl` (VARCHAR 500)
✅ **VenteEntity.java** - Ajout du champ `photoUrl` (VARCHAR 500)
✅ **FileStorageService.java** - Service de gestion des photos avec optimisation automatique
✅ **FileUploadController.java** - API REST pour upload/récupération/suppression
✅ **migration_add_photos.sql** - Script SQL pour mettre à jour la base de données

### 2. Architecture

```
┌────────────────────────────────────────────────┐
│                 FRONTEND                       │
│  (Angular/React - À implémenter)               │
└────────────────┬───────────────────────────────┘
                 │
                 │ POST /api/files/upload
                 │ (multipart/form-data)
                 ▼
┌────────────────────────────────────────────────┐
│          FileUploadController                  │
│  - Validation sécurité                         │
│  - Vérification tenant                         │
└────────────────┬───────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────┐
│         FileStorageService                     │
│  - Validation fichier (type, taille)           │
│  - Redimensionnement automatique (800x800px)   │
│  - Compression qualité 85%                     │
│  - Génération nom unique                       │
│  - Stockage sur disque                         │
└────────────────┬───────────────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────┐
│    uploads/photos/{tenant_uuid}/achats/        │
│    2024-01-15_123456_sac-riz_abc123.jpg        │
└────────────────────────────────────────────────┘
```

---

## 🚀 Comment Utiliser (Étapes)

### Étape 1 : Exécuter la Migration SQL

Connectez-vous à votre base de données PostgreSQL et exécutez :

```bash
psql -U votre_utilisateur -d votre_base -f migration_add_photos.sql
```

Ou via pgAdmin :
1. Ouvrez pgAdmin
2. Sélectionnez votre base de données
3. Ouvrez l'éditeur SQL (Tools → Query Tool)
4. Copiez-collez le contenu de `migration_add_photos.sql`
5. Exécutez (F5)

Vérifiez que tout s'est bien passé :
```sql
-- Vous devriez voir la nouvelle colonne photo_url
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'achats' AND column_name = 'photo_url';
```

### Étape 2 : Redémarrer l'Application

```bash
# Windows
cd "D:\boutique dijaSaliou\dijaSaliou"
.\mvnw.cmd spring-boot:run

# Linux/Mac
cd /path/to/dijaSaliou
./mvnw spring-boot:run
```

L'application va automatiquement créer le répertoire `uploads/photos/` au démarrage.

### Étape 3 : Tester l'API avec Postman/Curl

#### Test 1 : Vérifier que le service fonctionne

```bash
GET http://localhost:8080/api/files/health

Réponse attendue:
{
  "status": "ok",
  "service": "File Upload Service"
}
```

#### Test 2 : Upload une photo

```bash
POST http://localhost:8080/api/files/upload?type=achats
Authorization: Bearer {votre_token_jwt}
Content-Type: multipart/form-data

Body (form-data):
- file: [Sélectionner une image JPG/PNG]

Réponse attendue:
{
  "success": true,
  "photoUrl": "/api/files/photos/7d2ec4ac-ea4f-41f1-90ac-ff008945455c/achats/2024-01-15_143025_sac-riz_abc12345.jpg",
  "message": "Photo uploadée avec succès"
}
```

#### Test 3 : Récupérer la photo

```bash
GET http://localhost:8080/api/files/photos/7d2ec4ac-ea4f-41f1-90ac-ff008945455c/achats/2024-01-15_143025_sac-riz_abc12345.jpg
Authorization: Bearer {votre_token_jwt}

Réponse: L'image (Content-Type: image/jpeg)
```

#### Test 4 : Créer un achat avec photo

```bash
POST http://localhost:8080/api/achats
Authorization: Bearer {votre_token_jwt}
Content-Type: application/json

Body:
{
  "nomProduit": "Sac de riz 50kg",
  "quantite": 10,
  "prixUnitaire": 25000,
  "prixVenteSuggere": 30000,
  "fournisseur": "Ets Diop & Fils",
  "dateAchat": "2025-01-12",
  "photoUrl": "/api/files/photos/7d2ec4ac-ea4f-41f1-90ac-ff008945455c/achats/2024-01-15_143025_sac-riz_abc12345.jpg"
}

Réponse:
{
  "id": 123,
  "nomProduit": "Sac de riz 50kg",
  "quantite": 10,
  "prixUnitaire": 25000,
  "prixTotal": 250000,
  "photoUrl": "/api/files/photos/...",
  ...
}
```

---

## 🎨 Intégration Frontend (Angular/React)

### Exemple Angular

#### 1. Service d'Upload (file-upload.service.ts)

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileUploadService {
  private apiUrl = 'http://localhost:8080/api/files';

  constructor(private http: HttpClient) {}

  uploadPhoto(file: File, type: string = 'achats'): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post(`${this.apiUrl}/upload?type=${type}`, formData);
  }

  getPhotoUrl(photoUrl: string): string {
    return `http://localhost:8080${photoUrl}`;
  }

  deletePhoto(photoUrl: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/photos?url=${encodeURIComponent(photoUrl)}`);
  }
}
```

#### 2. Composant de Formulaire (achat-form.component.ts)

```typescript
import { Component } from '@angular/core';
import { FileUploadService } from './file-upload.service';

@Component({
  selector: 'app-achat-form',
  templateUrl: './achat-form.component.html'
})
export class AchatFormComponent {
  achat = {
    nomProduit: '',
    quantite: null,
    prixUnitaire: null,
    prixVenteSuggere: null,
    fournisseur: '',
    dateAchat: new Date().toISOString().split('T')[0],
    photoUrl: null
  };

  selectedFile: File | null = null;
  photoPreview: string | null = null;
  uploadProgress = false;

  constructor(private fileUploadService: FileUploadService) {}

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;

      // Prévisualisation
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.photoPreview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  async uploadPhoto() {
    if (!this.selectedFile) return;

    this.uploadProgress = true;

    try {
      const response = await this.fileUploadService.uploadPhoto(this.selectedFile, 'achats').toPromise();

      if (response.success) {
        this.achat.photoUrl = response.photoUrl;
        alert('Photo uploadée avec succès !');
      }
    } catch (error) {
      alert('Erreur lors de l\'upload de la photo');
      console.error(error);
    } finally {
      this.uploadProgress = false;
    }
  }

  submitAchat() {
    // Soumettre l'achat avec photoUrl
    console.log('Achat:', this.achat);
    // Appeler votre service d'achats ici
  }
}
```

#### 3. Template HTML (achat-form.component.html)

```html
<div class="form-container">
  <h2>📦 Nouvel Achat</h2>

  <form (ngSubmit)="submitAchat()">

    <!-- Section Photo -->
    <div class="photo-section">
      <label>📸 Photo du produit (optionnel)</label>

      <div class="photo-upload" *ngIf="!photoPreview">
        <input
          type="file"
          accept="image/*"
          (change)="onFileSelected($event)"
          id="photo-input"
          hidden>

        <label for="photo-input" class="upload-box">
          <div class="upload-icon">📷</div>
          <p>Cliquer pour ajouter une photo</p>
          <p class="hint">ou glisser-déposer</p>
        </label>
      </div>

      <div class="photo-preview" *ngIf="photoPreview">
        <img [src]="photoPreview" alt="Prévisualisation">
        <button type="button" (click)="uploadPhoto()" [disabled]="uploadProgress">
          {{ uploadProgress ? '⏳ Upload...' : '✅ Valider la photo' }}
        </button>
        <button type="button" (click)="photoPreview = null; selectedFile = null">
          ❌ Supprimer
        </button>
      </div>

      <div class="photo-success" *ngIf="achat.photoUrl">
        ✅ Photo enregistrée !
      </div>
    </div>

    <!-- Autres champs -->
    <div class="form-group">
      <label>📝 Nom du produit *</label>
      <input
        type="text"
        [(ngModel)]="achat.nomProduit"
        name="nomProduit"
        required
        placeholder="Ex: Sac de riz 50kg">
    </div>

    <div class="form-group">
      <label>📦 Quantité *</label>
      <input
        type="number"
        [(ngModel)]="achat.quantite"
        name="quantite"
        required
        min="1">
    </div>

    <div class="form-group">
      <label>💰 Prix unitaire (CFA) *</label>
      <input
        type="number"
        [(ngModel)]="achat.prixUnitaire"
        name="prixUnitaire"
        required
        min="0">
    </div>

    <div class="form-group">
      <label>💵 Prix de vente suggéré (CFA)</label>
      <input
        type="number"
        [(ngModel)]="achat.prixVenteSuggere"
        name="prixVenteSuggere"
        min="0">
    </div>

    <div class="form-group">
      <label>🏢 Fournisseur</label>
      <input
        type="text"
        [(ngModel)]="achat.fournisseur"
        name="fournisseur"
        placeholder="Ex: Ets Diop & Fils">
    </div>

    <div class="form-group">
      <label>📅 Date d'achat *</label>
      <input
        type="date"
        [(ngModel)]="achat.dateAchat"
        name="dateAchat"
        required>
    </div>

    <div class="form-actions">
      <button type="button" class="btn-secondary">Annuler</button>
      <button type="submit" class="btn-primary">Enregistrer</button>
    </div>
  </form>
</div>
```

#### 4. CSS (achat-form.component.css)

```css
.photo-upload {
  margin: 20px 0;
}

.upload-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed #ccc;
  border-radius: 8px;
  padding: 40px;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-box:hover {
  border-color: #007bff;
  background: #f8f9fa;
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.photo-preview img {
  max-width: 400px;
  max-height: 400px;
  border-radius: 8px;
  margin-bottom: 15px;
}

.photo-success {
  color: green;
  padding: 10px;
  background: #d4edda;
  border-radius: 5px;
  margin-top: 10px;
}
```

---

## 📱 Exemple React

```jsx
import React, { useState } from 'react';
import axios from 'axios';

function AchatForm() {
  const [achat, setAchat] = useState({
    nomProduit: '',
    quantite: '',
    prixUnitaire: '',
    prixVenteSuggere: '',
    fournisseur: '',
    dateAchat: new Date().toISOString().split('T')[0],
    photoUrl: null
  });

  const [selectedFile, setSelectedFile] = useState(null);
  const [photoPreview, setPhotoPreview] = useState(null);
  const [uploading, setUploading] = useState(false);

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (file) {
      setSelectedFile(file);

      // Prévisualisation
      const reader = new FileReader();
      reader.onloadend = () => {
        setPhotoPreview(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const uploadPhoto = async () => {
    if (!selectedFile) return;

    setUploading(true);
    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      const response = await axios.post(
        'http://localhost:8080/api/files/upload?type=achats',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        }
      );

      if (response.data.success) {
        setAchat({ ...achat, photoUrl: response.data.photoUrl });
        alert('Photo uploadée avec succès !');
      }
    } catch (error) {
      alert('Erreur lors de l\'upload');
      console.error(error);
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    console.log('Achat:', achat);
    // Soumettre l'achat
  };

  return (
    <div className="form-container">
      <h2>📦 Nouvel Achat</h2>

      <form onSubmit={handleSubmit}>
        {/* Photo Section */}
        <div className="photo-section">
          <label>📸 Photo du produit</label>

          {!photoPreview ? (
            <div className="upload-box">
              <input
                type="file"
                accept="image/*"
                onChange={handleFileSelect}
                style={{display: 'none'}}
                id="photo-input"
              />
              <label htmlFor="photo-input">
                <div className="upload-icon">📷</div>
                <p>Cliquer pour ajouter une photo</p>
              </label>
            </div>
          ) : (
            <div className="photo-preview">
              <img src={photoPreview} alt="Preview" />
              <button type="button" onClick={uploadPhoto} disabled={uploading}>
                {uploading ? '⏳ Upload...' : '✅ Valider'}
              </button>
              <button type="button" onClick={() => {
                setPhotoPreview(null);
                setSelectedFile(null);
              }}>
                ❌ Supprimer
              </button>
            </div>
          )}
        </div>

        {/* Autres champs */}
        <input
          type="text"
          placeholder="Nom du produit"
          value={achat.nomProduit}
          onChange={(e) => setAchat({...achat, nomProduit: e.target.value})}
          required
        />

        {/* ... autres champs ... */}

        <button type="submit">Enregistrer</button>
      </form>
    </div>
  );
}

export default AchatForm;
```

---

## 🔒 Sécurité

### Mesures Implémentées

1. ✅ **Isolation Multi-Tenant** : Chaque tenant ne peut accéder qu'à ses photos
2. ✅ **Validation des fichiers** : Extensions et types MIME vérifiés
3. ✅ **Taille limitée** : 5 MB max par photo
4. ✅ **Authentification requise** : JWT obligatoire
5. ✅ **Permissions** : Seul l'ADMIN peut uploader/supprimer
6. ✅ **Noms de fichiers sécurisés** : UUID + timestamp
7. ✅ **Optimisation automatique** : Redimensionnement à 800x800px max

---

## 📊 Récapitulatif Final

### ✅ Ce qui fonctionne maintenant

- Upload de photos pour les achats et ventes
- Stockage organisé par tenant
- Optimisation automatique des images
- API REST complète (upload, récupération, suppression)
- Sécurité multi-tenant
- Migration SQL prête à exécuter

### 📝 Prochaines étapes (Frontend)

1. Créer le composant d'upload de photo en Angular
2. Intégrer dans le formulaire d'ajout d'achat
3. Afficher les photos dans la liste des achats
4. Ajouter une lightbox pour agrandir les photos
5. Implémenter le drag & drop

### 💡 Recommandations

- **Photo + Nom** : Toujours garder les deux (meilleure UX)
- **Photo optionnelle** : Ne pas forcer l'utilisateur
- **Optimisation** : Les photos sont automatiquement redimensionnées
- **Mobile-first** : Penser à l'expérience mobile dès le début

---

Voulez-vous que je vous aide à implémenter la partie frontend Angular/React maintenant ?
