# 🔐 Configuration Sécurisée du JWT Secret

## ⚠️ CRITIQUE : Ne JAMAIS utiliser le secret par défaut en production

Le secret JWT par défaut dans `application.properties` est **uniquement pour le développement local**. En production, vous DEVEZ générer un secret unique et fort.

---

## 🔑 Générer un JWT Secret Fort

### Méthode 1 : OpenSSL (Recommandée)

```bash
# Générer un secret de 256 bits (64 caractères base64)
openssl rand -base64 64
```

**Exemple de sortie :**
```
qK9vL2mN4pR5sT8uV1wX3yZ6aC7bD9eF0gH2iJ4kL5mN6oP8qR9sT1uV2wX4yZ5aC6bD8eF9gH0iJ1kL3mN4oP5q
```

### Méthode 2 : Node.js

```bash
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

### Méthode 3 : Python

```bash
python -c "import secrets; print(secrets.token_urlsafe(64))"
```

### Méthode 4 : En ligne (Moins sécurisé)

Si vous n'avez pas accès aux commandes ci-dessus, utilisez un générateur en ligne :
- https://www.grc.com/passwords.htm
- https://passwordsgenerator.net/

**⚠️ ATTENTION** : Ne JAMAIS réutiliser le même secret sur plusieurs serveurs.

---

## 🖥️ Configuration en Production

### Sur Heroku

```bash
# Générer le secret
JWT_SECRET=$(openssl rand -base64 64)

# Configurer sur Heroku
heroku config:set JWT_SECRET="$JWT_SECRET"
```

### Sur AWS / VPS

```bash
# 1. Générer le secret
openssl rand -base64 64

# 2. Copier le résultat

# 3. Ajouter dans les variables d'environnement du serveur
export JWT_SECRET="qK9vL2mN4pR5sT8uV1wX3yZ6aC7bD9eF0gH2iJ4kL5mN6oP8qR9sT1uV2wX4yZ5aC6bD8eF9gH0iJ1kL3mN4oP5q"

# 4. Ajouter dans /etc/environment pour persistance
echo 'JWT_SECRET="votre-secret-ici"' | sudo tee -a /etc/environment
```

### Avec Docker

```yaml
# docker-compose.yml
version: '3.8'
services:
  api:
    image: dijasaliou-backend
    environment:
      - JWT_SECRET=${JWT_SECRET}
    env_file:
      - .env
```

Puis créer `.env` :
```bash
JWT_SECRET=qK9vL2mN4pR5sT8uV1wX3yZ6aC7bD9eF0gH2iJ4kL5mN6oP8qR9sT1uV2wX4yZ5aC6bD8eF9gH0iJ1kL3mN4oP5q
```

---

## ✅ Vérifier que le JWT Secret est configuré

### Au démarrage de l'application

Ajouter ce log dans `JwtService.java` (constructeur) :

```java
@PostConstruct
public void init() {
    if (secretKey.length() < 32) {
        log.error("❌ CRITIQUE: JWT Secret trop court! Minimum 32 caractères requis.");
        throw new IllegalStateException("JWT Secret non sécurisé");
    }
    log.info("✅ JWT Secret configuré ({} caractères)", secretKey.length());
}
```

### Test manuel

```bash
# Sur Heroku
heroku config:get JWT_SECRET

# Sur serveur Linux
echo $JWT_SECRET
```

---

## 🚨 Que faire si le secret a été exposé ?

Si votre JWT secret a été commité dans Git ou exposé publiquement :

### 1. Générer un NOUVEAU secret immédiatement

```bash
openssl rand -base64 64
```

### 2. Mettre à jour la production

```bash
heroku config:set JWT_SECRET="nouveau-secret-ici"
```

### 3. Invalider tous les tokens existants

Tous les utilisateurs devront se reconnecter, car leurs anciens tokens ne seront plus valides avec le nouveau secret.

### 4. Nettoyer l'historique Git (si le secret était commité)

```bash
# ⚠️ DANGER: Cela réécrit l'historique Git
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch src/main/resources/application.properties" \
  --prune-empty --tag-name-filter cat -- --all
```

**Alternative plus simple** : Créer un nouveau repository et ne pousser que le code nettoyé.

---

## 📋 Checklist de Sécurité JWT

- [ ] Secret de minimum 256 bits (64 caractères)
- [ ] Secret généré aléatoirement (pas de phrase)
- [ ] Secret unique par environnement (dev ≠ prod)
- [ ] Secret chargé depuis variable d'environnement
- [ ] Secret JAMAIS dans le code source
- [ ] Secret JAMAIS dans Git
- [ ] `.env` ajouté au `.gitignore`
- [ ] Vérification de la longueur au démarrage
- [ ] Rotation du secret tous les 6-12 mois

---

## 🔄 Rotation du Secret (Optionnel)

Pour changer le secret sans déconnecter tous les utilisateurs :

1. Configurer 2 secrets : `JWT_SECRET` (actuel) et `JWT_SECRET_OLD` (ancien)
2. Signer les nouveaux tokens avec `JWT_SECRET`
3. Valider les tokens avec `JWT_SECRET` OU `JWT_SECRET_OLD`
4. Après 24h (durée d'expiration), retirer `JWT_SECRET_OLD`

---

## 📞 Support

En cas de problème avec la configuration JWT :
- Email : ahmadubambagayelo@gmail.com

---

## ✅ Configuration Actuelle

### Développement Local
- Secret par défaut dans `application.properties` (OK pour dev)
- Longueur : 88 caractères (✅ Suffisant)

### Production
- Secret chargé depuis `${JWT_SECRET}` dans `application-prod.properties`
- **⚠️ VOUS DEVEZ configurer cette variable d'environnement**
