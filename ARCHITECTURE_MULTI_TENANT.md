# 🏗️ ARCHITECTURE MULTI-TENANT - DOCUMENTATION TECHNIQUE

## 📐 Vue d'ensemble de l'architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND (Angular)                       │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP Request + JWT Token
                            │ (contient tenant_id)
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    JwtAuthenticationFilter                       │
│  1. Valide le JWT                                               │
│  2. Extrait email + tenant_id                                   │
│  3. Stocke tenant_id dans TenantContext (ThreadLocal)           │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Spring Security                           │
│  Authentification + Autorisation                                │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Controllers                             │
│  (AchatController, VenteController, etc.)                       │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                           Services                               │
│  (AchatService, VenteService, etc.)                             │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Repositories                             │
│  (AchatRepository, VenteRepository, etc.)                       │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                       TenantJpaConfig (AOP)                      │
│  Active automatiquement le filtre Hibernate                     │
│  AVANT chaque appel au Repository                               │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Hibernate + JPA Filter                      │
│  Ajoute automatiquement :                                       │
│  WHERE tenant_id = (SELECT id FROM tenants                      │
│                     WHERE tenant_uuid = :tenantId)              │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    TenantEntityListener                          │
│  Assigne automatiquement le tenant_id                           │
│  lors de la création d'entités (@PrePersist)                    │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                        BASE DE DONNÉES                           │
│  Tables : tenants, utilisateurs, achats, ventes, depenses       │
│  Toutes les tables métier ont une colonne tenant_id             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Flux de sécurité

### 1. Inscription (Création d'un nouveau tenant)

```
User fills form
    │
    ▼
POST /api/auth/register
{
  "nomEntreprise": "Boutique A",
  "numeroTelephone": "+221771111111",
  "email": "user@boutique-a.com",
  "motDePasse": "password123"
}
    │
    ▼
AuthService.register()
    │
    ├─► 1. Vérifier email unique
    ├─► 2. Créer TENANT (TenantEntity)
    │      └─► UUID généré automatiquement
    ├─► 3. Créer USER (UserEntity)
    │      └─► Lié au tenant
    │      └─► Role = ADMIN (premier user)
    └─► 4. Générer JWT
           └─► Claims : { email, tenant_id }
    │
    ▼
Response
{
  "token": "eyJhbGc...",  ← Contient tenant_id
  "user": { ... }
}
```

### 2. Connexion

```
User logs in
    │
    ▼
POST /api/auth/login
{
  "email": "user@boutique-a.com",
  "motDePasse": "password123"
}
    │
    ▼
AuthService.login()
    │
    ├─► 1. Trouver user par email
    ├─► 2. Vérifier mot de passe
    ├─► 3. Récupérer tenant de l'user
    │      └─► user.getTenant().getTenantUuid()
    ├─► 4. Vérifier tenant actif
    └─► 5. Générer JWT
           └─► Claims : { email, tenant_id }
    │
    ▼
Response
{
  "token": "eyJhbGc...",  ← Contient tenant_id
  "user": { ... }
}
```

### 3. Requête API (Exemple : Liste des achats)

```
GET /api/achats
Headers:
  Authorization: Bearer eyJhbGc...  ← JWT avec tenant_id
    │
    ▼
┌──────────────────────────────────────┐
│  JwtAuthenticationFilter             │
│  1. Extrait JWT du header            │
│  2. Valide JWT (signature + expiry)  │
│  3. Extrait tenant_id du JWT         │
│  4. TenantContext.set(tenant_id)     │ ← Stocke dans ThreadLocal
└──────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────┐
│  Spring Security                     │
│  Vérifie autorisation (roles)        │
└──────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────┐
│  AchatController.getAllAchats()      │
│  Appelle AchatService                │
└──────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────┐
│  AchatService.findAll()              │
│  Appelle AchatRepository             │
└──────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────┐
│  TenantJpaConfig (AOP Aspect)        │
│  @Before Repository call             │
│  1. Récupère tenant_id du context    │
│  2. Active Hibernate Filter          │
│  3. Inject tenant_id dans le filtre  │
└──────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────┐
│  Hibernate génère SQL:               │
│  SELECT * FROM achats                │
│  WHERE tenant_id = (                 │
│    SELECT id FROM tenants            │
│    WHERE tenant_uuid = 'abc-123'     │ ← tenant_id du JWT
│  )                                   │
└──────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────┐
│  BASE DE DONNÉES                     │
│  Retourne UNIQUEMENT les achats      │
│  du tenant connecté                  │
└──────────────────────────────────────┘
    │
    ▼
Response: [ { achat1 }, { achat2 }, ... ]
    │
    ▼
┌──────────────────────────────────────┐
│  JwtAuthenticationFilter (finally)   │
│  TenantContext.clear()               │ ← Nettoie ThreadLocal
└──────────────────────────────────────┘
```

### 4. Création d'entité (Exemple : Nouvel achat)

```
POST /api/achats
Headers:
  Authorization: Bearer eyJhbGc...
Body:
{
  "nomProduit": "Ordinateur",
  "quantite": 5,
  "prixUnitaire": 500000
}
    │
    ▼
[Même flux que ci-dessus jusqu'au Service]
    │
    ▼
┌──────────────────────────────────────┐
│  AchatService.create(achat)          │
│  Repository.save(achat)              │
└──────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────┐
│  TenantEntityListener                │
│  @PrePersist déclenché               │
│  1. Récupère tenant_id du context    │
│  2. Charge TenantEntity de la BD     │
│  3. achat.setTenant(tenant)          │ ← Assignation automatique
└──────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────┐
│  Hibernate génère SQL:               │
│  INSERT INTO achats (                │
│    nom_produit, quantite,            │
│    prix_unitaire, tenant_id, ...     │
│  ) VALUES (                          │
│    'Ordinateur', 5, 500000, 1, ...   │ ← tenant_id assigné
│  )                                   │
└──────────────────────────────────────┘
    │
    ▼
Response: { id: 123, nomProduit: "Ordinateur", ... }
```

---

## 🛡️ Mécanismes de sécurité

### 1. JWT avec tenant_id

**Fichier** : `jwt/JwtService.java`

```java
// Génération du token
public String generateToken(String email, String tenantId) {
    return Jwts.builder()
        .setSubject(email)
        .claim("tenant_id", tenantId)  // ← CRITIQUE
        .signWith(getSigningKey())
        .compact();
}

// Extraction du tenant_id
public String getTenantIdFromToken(String token) {
    Claims claims = getClaims(token);
    return claims.get("tenant_id", String.class);
}
```

**Sécurité** :
- ✅ Le tenant_id est **signé** (impossible de le modifier sans invalider le token)
- ✅ Le tenant_id est **vérifié** à chaque requête
- ✅ Si le token est falsifié → rejeté par Spring Security

### 2. TenantContext (ThreadLocal)

**Fichier** : `tenant/TenantContext.java`

```java
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();  // ← CRITIQUE : Évite fuite mémoire
    }
}
```

**Sécurité** :
- ✅ **ThreadLocal** : Isolation automatique entre requêtes HTTP
- ✅ Un utilisateur **ne peut PAS** accéder au tenant d'un autre
- ✅ Nettoyage automatique dans `finally` block

### 3. Hibernate Filter

**Fichier** : Entités (`AchatEntity.java`, etc.)

```java
@FilterDef(name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter",
    condition = "tenant_id = (SELECT id FROM tenants WHERE tenant_uuid = :tenantId)")
public class AchatEntity { ... }
```

**Activation** : `config/TenantJpaConfig.java`

```java
@Before("execution(* com.example.dijasaliou.repository.*.*(..))")
public void enableTenantFilter() {
    String tenantId = TenantContext.getCurrentTenant();
    Session session = entityManager.unwrap(Session.class);
    Filter filter = session.enableFilter("tenantFilter");
    filter.setParameter("tenantId", tenantId);
}
```

**Sécurité** :
- ✅ Filtre activé **automatiquement** (AOP)
- ✅ **Impossible d'oublier** d'activer le filtre
- ✅ Appliqué à **TOUTES** les requêtes SELECT

### 4. Entity Listener

**Fichier** : `tenant/TenantEntityListener.java`

```java
@PrePersist
public void setTenant(Object entity) {
    String tenantId = TenantContext.getCurrentTenant();
    TenantEntity tenant = tenantRepository.findByTenantUuid(tenantId)
        .orElseThrow();
    entity.setTenant(tenant);  // Via réflexion
}
```

**Sécurité** :
- ✅ Assignation **automatique** du tenant
- ✅ **Impossible d'oublier** d'assigner le tenant
- ✅ Exception levée si tenant manquant

---

## 📊 Schéma de base de données

```sql
┌─────────────────────────┐
│       TENANTS           │
├─────────────────────────┤
│ id (PK)                 │
│ tenant_uuid (UNIQUE)    │ ← UUID généré auto
│ nom_entreprise          │
│ numero_telephone        │
│ actif                   │
│ plan (ENUM)             │
│ date_creation           │
│ date_expiration         │
└─────────────────────────┘
         ▲
         │ (FK tenant_id)
         │
    ┌────┴────┬────────┬────────┬────────┐
    │         │        │        │        │
┌───▼───┐ ┌──▼───┐ ┌──▼───┐ ┌──▼───┐ ┌──▼───┐
│ USERS │ │ACHATS│ │VENTES│ │DEPE..│ │ ... │
├───────┤ ├──────┤ ├──────┤ ├──────┤ └──────┘
│ id    │ │ id   │ │ id   │ │ id   │
│ email │ │ ...  │ │ ...  │ │ ...  │
│tenant │ │tenant│ │tenant│ │tenant│
│  _id  │ │  _id │ │  _id │ │  _id │
└───────┘ └──────┘ └──────┘ └──────┘
```

**Clés** :
- Chaque table métier a `tenant_id` (FK vers `tenants.id`)
- Index sur `tenant_id` pour performance
- Contrainte ON DELETE RESTRICT (ne pas supprimer un tenant avec des données)

---

## 🎯 Points clés de l'implémentation

### ✅ Ce qui est automatique

1. **Extraction du tenant_id** depuis le JWT → `JwtAuthenticationFilter`
2. **Stockage du tenant_id** dans ThreadLocal → `TenantContext`
3. **Activation du filtre** Hibernate → `TenantJpaConfig` (AOP)
4. **Filtrage des requêtes** SELECT → Hibernate Filter
5. **Assignation du tenant** lors de l'INSERT → `TenantEntityListener`
6. **Nettoyage du contexte** après la requête → `JwtAuthenticationFilter` (finally)

### ⚠️ Points d'attention

1. **Ne JAMAIS désactiver le filtre tenant** sauf cas exceptionnel
2. **Ne JAMAIS utiliser de requêtes SQL natives** sans filtre tenant
3. **Toujours vérifier** que le tenant est actif lors du login
4. **Toujours logger** les tentatives d'accès suspects
5. **Toujours nettoyer** le TenantContext (déjà fait dans le filtre)

---

## 🚀 Évolutions futures possibles

### 1. Limitation par plan

```java
// Dans TenantService
public void verifierLimites(String tenantId, String feature) {
    TenantEntity tenant = tenantRepository.findByTenantUuid(tenantId)
        .orElseThrow();

    if (tenant.getPlan() == Plan.GRATUIT) {
        long nbAchats = achatRepository.countByTenant(tenant);
        if (nbAchats >= 100) {
            throw new LimiteAtteinte Exception("Limite du plan gratuit atteinte");
        }
    }
}
```

### 2. Audit par tenant

```java
// Table d'audit
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    user_id BIGINT,
    action VARCHAR(50),
    entity VARCHAR(50),
    timestamp DATETIME
);
```

### 3. Backup par tenant

```bash
# Script de backup individuel
mysqldump dijasaliou \
  --where="tenant_id=(SELECT id FROM tenants WHERE tenant_uuid='abc-123')" \
  achats ventes depenses > backup_tenant_abc123.sql
```

### 4. Statistiques par tenant

```java
// Dashboard admin
public TenantStats getStats(String tenantId) {
    return TenantStats.builder()
        .nbUsers(userRepository.countByTenantUuid(tenantId))
        .nbAchats(achatRepository.countByTenantUuid(tenantId))
        .nbVentes(venteRepository.countByTenantUuid(tenantId))
        .chiffreAffaires(venteRepository.sumByTenantUuid(tenantId))
        .build();
}
```

---

**🎉 Votre architecture multi-tenant est maintenant complète et sécurisée !**
