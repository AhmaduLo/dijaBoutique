-- ===========================================================================
-- Script SQL pour créer le premier compte ADMINISTRATEUR
-- Boutique Dija Saliou
-- ===========================================================================

-- IMPORTANT : Ce script doit être exécuté UNE SEULE FOIS lors de l'initialisation
-- Il crée le compte ADMIN initial qui pourra ensuite créer d'autres comptes

-- ===========================================================================
-- ÉTAPE 1 : Création du premier ADMIN
-- ===========================================================================

-- Mot de passe : admin123
-- Le hash ci-dessous correspond à "admin123" avec BCrypt (coût 10)
-- Hash généré avec BCryptPasswordEncoder

INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role, date_creation, created_by_user_id)
VALUES (
    'Admin',
    'Dija Saliou',
    'admin@dijaboutique.com',
    '$2a$10$N9qo8uLOickgx2ZMmRVAQOuf8U70xZ8n8YxCn9FZ/0l3uH9K4L0k6',
    'ADMIN',
    NOW(),
    NULL  -- NULL car c'est le premier compte (pas créé par quelqu'un d'autre)
);

-- ===========================================================================
-- ÉTAPE 2 : Vérification
-- ===========================================================================

-- Afficher le compte créé
SELECT
    id,
    nom,
    prenom,
    email,
    role,
    date_creation
FROM utilisateurs
WHERE email = 'admin@dijaboutique.com';

-- ===========================================================================
-- INSTRUCTIONS D'UTILISATION
-- ===========================================================================

/*

1. EXÉCUTER CE SCRIPT
   -----------------
   Dans MySQL Workbench ou votre client MySQL :

   mysql -u root -p dija_boutique < create_admin.sql

2. SE CONNECTER
   ------------
   Endpoint : POST http://localhost:8080/api/auth/login

   Body JSON :
   {
     "email": "admin@dijaboutique.com",
     "motDePasse": "admin123"
   }

   Réponse :
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

3. UTILISER LE TOKEN
   -----------------
   Pour toutes les requêtes suivantes, ajouter le header :

   Authorization: Bearer {le-token-reçu}

4. CRÉER D'AUTRES COMPTES
   ----------------------
   Endpoint : POST http://localhost:8080/api/admin/utilisateurs
   Header : Authorization: Bearer {token-admin}

   Body JSON :
   {
     "nom": "Diop",
     "prenom": "Mamadou",
     "email": "mamadou@dijaboutique.com",
     "motDePasse": "password123",
     "role": "USER"
   }

5. CHANGER LE MOT DE PASSE ADMIN
   -----------------------------
   Endpoint : PUT http://localhost:8080/api/admin/utilisateurs/1
   Header : Authorization: Bearer {token-admin}

   Body JSON :
   {
     "motDePasse": "nouveau-mot-de-passe-securise"
   }

⚠️ SÉCURITÉ : Changez le mot de passe par défaut immédiatement !

*/

-- ===========================================================================
-- NOTES
-- ===========================================================================

/*

GÉNÉRATION D'UN NOUVEAU HASH BCRYPT :
--------------------------------------
Si vous souhaitez utiliser un autre mot de passe, vous devez générer un hash BCrypt.

Option 1 : En ligne
- Allez sur https://bcrypt-generator.com/
- Entrez votre mot de passe
- Coût : 10 (par défaut)
- Copiez le hash généré

Option 2 : Avec Java
```java
String password = "votre-mot-de-passe";
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode(password);
System.out.println(hash);
```

Option 3 : Avec un endpoint temporaire
- Créez temporairement un endpoint dans votre application
- Appelez-le avec le mot de passe souhaité
- Récupérez le hash
- Supprimez l'endpoint

STRUCTURE DE LA BASE DE DONNÉES :
----------------------------------
La colonne `created_by_id` permet de tracer qui a créé chaque compte :
- NULL = compte initial (premier ADMIN)
- ID = ID de l'utilisateur ADMIN qui a créé ce compte

Cela permet de :
- Savoir qui a créé quel compte
- Empêcher la suppression en cascade si un ADMIN est supprimé
- Générer des rapports sur l'activité des ADMIN

*/
