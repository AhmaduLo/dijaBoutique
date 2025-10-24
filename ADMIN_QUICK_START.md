# 🚀 Démarrage Rapide - Système d'Administration

## 📌 Résumé en 5 minutes

### Ce qui a changé

✅ **L'inscription publique est DÉSACTIVÉE**
- Plus personne ne peut s'inscrire via `POST /api/auth/register`
- Seul l'ADMIN peut créer des comptes

✅ **Un seul ADMIN initial**
- Créé manuellement avec le script SQL `create_admin.sql`
- Email : `admin@dijaboutique.com`
- Mot de passe : `admin123` (⚠️ À CHANGER IMMÉDIATEMENT)

✅ **L'ADMIN a tous les droits**
- Voir tous les comptes
- Créer des comptes employés (USER)
- Modifier n'importe quel compte (sauf supprimer le sien)
- Changer les rôles (USER ↔ ADMIN)

✅ **Les USER (employés) sont limités**
- ❌ Ne peuvent PAS modifier leurs infos
- ❌ Ne peuvent PAS supprimer leur compte
- ❌ Ne peuvent PAS créer d'autres comptes
- ✅ Peuvent utiliser l'application (achats, ventes, dépenses, stock)

---

## 🔧 Installation en 3 étapes

### Étape 1 : Créer le compte ADMIN

Exécutez le script SQL :

```bash
mysql -u root -p dija_boutique < create_admin.sql
```

Ou dans MySQL Workbench, ouvrez `create_admin.sql` et exécutez-le.

### Étape 2 : Se connecter en tant qu'ADMIN

**Postman ou Angular :**

```
POST http://localhost:8080/api/auth/login

Body JSON :
{
  "email": "admin@dijaboutique.com",
  "motDePasse": "admin123"
}
```

**Réponse :**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "nom": "Admin",
    "prenom": "Dija Saliou",
    "email": "admin@dijaboutique.com",
    "role": "ADMIN"
  }
}
```

### Étape 3 : Changer le mot de passe ADMIN (IMPORTANT !)

```
PUT http://localhost:8080/api/admin/utilisateurs/1
Header : Authorization: Bearer {le-token-de-l-étape-2}

Body JSON :
{
  "motDePasse": "VotreNouveauMotDePasseSecurise!"
}
```

---

## 👥 Créer un compte employé

```
POST http://localhost:8080/api/admin/utilisateurs
Header : Authorization: Bearer {token-admin}

Body JSON :
{
  "nom": "Diop",
  "prenom": "Mamadou",
  "email": "mamadou@dijaboutique.com",
  "motDePasse": "MotDePasseTemporaire123",
  "role": "USER"
}
```

**Donnez ensuite les identifiants à l'employé :**
- Email : `mamadou@dijaboutique.com`
- Mot de passe : `MotDePasseTemporaire123`

L'employé peut maintenant se connecter et utiliser l'application !

---

## 🔍 Voir tous les comptes

```
GET http://localhost:8080/api/admin/utilisateurs
Header : Authorization: Bearer {token-admin}
```

**Réponse :**
```json
[
  {
    "id": 1,
    "nom": "Admin",
    "prenom": "Dija Saliou",
    "email": "admin@dijaboutique.com",
    "role": "ADMIN"
  },
  {
    "id": 2,
    "nom": "Diop",
    "prenom": "Mamadou",
    "email": "mamadou@dijaboutique.com",
    "role": "USER"
  }
]
```

---

## 🗑️ Supprimer un compte

```
DELETE http://localhost:8080/api/admin/utilisateurs/2
Header : Authorization: Bearer {token-admin}
```

⚠️ **Vous ne pouvez PAS supprimer votre propre compte !**

---

## 🔄 Changer le rôle (USER → ADMIN)

```
PUT http://localhost:8080/api/admin/utilisateurs/2/role
Header : Authorization: Bearer {token-admin}

Body JSON :
{
  "role": "ADMIN"
}
```

---

## 📊 Statistiques

```
GET http://localhost:8080/api/admin/statistiques
Header : Authorization: Bearer {token-admin}
```

**Réponse :**
```json
{
  "nombreTotal": 5,
  "nombreAdmins": 1,
  "nombreUsers": 4,
  "nouveauxUtilisateurs7Jours": 2
}
```

---

## ⚠️ Erreurs courantes

### 403 Forbidden

**Cause :** Token invalide ou utilisateur USER essaie d'accéder à une route ADMIN.

**Solution :**
- Vérifiez le header `Authorization: Bearer {token}`
- Assurez-vous d'être connecté en tant qu'ADMIN

### 401 Unauthorized

**Cause :** Token expiré ou pas de token.

**Solution :** Reconnectez-vous avec `POST /api/auth/login`.

### "L'inscription publique est désactivée"

**Cause :** Tentative d'utiliser `POST /api/auth/register`.

**Solution :** Seul l'ADMIN peut créer des comptes via `POST /api/admin/utilisateurs`.

---

## 📖 Documentation complète

Pour plus de détails, consultez [ADMINISTRATION_SYSTEME.md](./ADMINISTRATION_SYSTEME.md).

---

## ✅ Checklist de démarrage

- [ ] Exécuter `create_admin.sql`
- [ ] Se connecter avec `admin@dijaboutique.com` / `admin123`
- [ ] Changer le mot de passe ADMIN
- [ ] Créer les comptes employés
- [ ] Tester la connexion d'un employé
- [ ] Vérifier que les employés ne peuvent pas modifier leur compte

---

**🎉 Votre système d'administration est opérationnel !**
