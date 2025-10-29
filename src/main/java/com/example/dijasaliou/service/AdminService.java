package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.RegisterRequest;
import com.example.dijasaliou.dto.UserDto;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service pour la gestion administrative des utilisateurs
 *
 * RÈGLES :
 * 1. Seul l'ADMIN peut créer des comptes
 * 2. L'ADMIN peut voir tous les comptes
 * 3. L'ADMIN peut modifier tous les comptes (y compris le sien)
 * 4. L'ADMIN peut supprimer tous les comptes (sauf le sien)
 * 5. Les USER ne peuvent pas modifier ni supprimer leur compte
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder, TenantService tenantService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantService = tenantService;
    }

    /**
     * Vérifier qu'un utilisateur est ADMIN
     */
    private UserEntity verifierAdmin(String email) {
        UserEntity admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (admin.getRole() != UserEntity.Role.ADMIN) {
            throw new RuntimeException("Accès refusé : Vous devez être ADMIN pour effectuer cette action");
        }

        return admin;
    }

    /**
     * Obtenir tous les utilisateurs
     * Accessible uniquement aux ADMIN
     */
    public List<UserDto> obtenirTousLesUtilisateurs(String emailAdmin) {
        verifierAdmin(emailAdmin);

        List<UserEntity> utilisateurs = userRepository.findAll();

        return utilisateurs.stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtenir un utilisateur par ID
     */
    public UserDto obtenirUtilisateurParId(Long id, String emailAdmin) {
        verifierAdmin(emailAdmin);

        UserEntity utilisateur = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + id));

        return UserDto.fromEntity(utilisateur);
    }

    /**
     * Créer un nouveau compte employé/client
     * IMPORTANT : Seul l'ADMIN peut créer des comptes
     */
    @Transactional
    public UserDto creerUtilisateur(RegisterRequest request, String emailAdmin) {
        UserEntity admin = verifierAdmin(emailAdmin);

        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        // Par défaut, les comptes créés par l'admin sont des USER
        // Sauf si explicitement spécifié ADMIN dans la requête
        UserEntity.Role role = request.getRole() != null ? request.getRole() : UserEntity.Role.USER;

        // Récupérer le tenant actuel pour copier nomEntreprise et numeroTelephone
        TenantEntity tenant = tenantService.getCurrentTenant();

        // Créer l'utilisateur
        UserEntity nouvelUtilisateur = UserEntity.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .role(role)
                .nomEntreprise(tenant.getNomEntreprise()) // Copier du tenant
                .numeroTelephone(tenant.getNumeroTelephone()) // Copier du tenant
                .createdByUser(admin) // Enregistrer qui a créé ce compte
                .tenant(tenant) // MULTI-TENANT : Assigner le tenant (CRUCIAL!)
                .build();

        UserEntity utilisateurSauvegarde = userRepository.save(nouvelUtilisateur);

        return UserDto.fromEntity(utilisateurSauvegarde);
    }

    /**
     * Modifier les informations d'un utilisateur
     * L'ADMIN peut modifier n'importe quel compte
     */
    @Transactional
    public UserDto modifierUtilisateur(Long id, Map<String, Object> updates, String emailAdmin) {
        verifierAdmin(emailAdmin);

        UserEntity utilisateur = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + id));

        // Appliquer les modifications
        if (updates.containsKey("nom")) {
            utilisateur.setNom((String) updates.get("nom"));
        }
        if (updates.containsKey("prenom")) {
            utilisateur.setPrenom((String) updates.get("prenom"));
        }
        if (updates.containsKey("email")) {
            String nouvelEmail = (String) updates.get("email");
            // Vérifier que le nouvel email n'existe pas déjà (sauf si c'est le même)
            if (!utilisateur.getEmail().equals(nouvelEmail) && userRepository.existsByEmail(nouvelEmail)) {
                throw new RuntimeException("Un utilisateur avec cet email existe déjà");
            }
            utilisateur.setEmail(nouvelEmail);
        }
        if (updates.containsKey("motDePasse")) {
            String nouveauMotDePasse = (String) updates.get("motDePasse");
            utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        }

        UserEntity utilisateurModifie = userRepository.save(utilisateur);

        return UserDto.fromEntity(utilisateurModifie);
    }

    /**
     * Supprimer un utilisateur
     * L'ADMIN peut supprimer n'importe quel compte SAUF LE SIEN
     */
    @Transactional
    public void supprimerUtilisateur(Long id, String emailAdmin) {
        UserEntity admin = verifierAdmin(emailAdmin);

        UserEntity utilisateur = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + id));

        // Empêcher l'admin de se supprimer lui-même
        if (utilisateur.getEmail().equals(admin.getEmail())) {
            throw new RuntimeException("Vous ne pouvez pas supprimer votre propre compte");
        }

        userRepository.deleteById(id);
    }

    /**
     * Modifier le rôle d'un utilisateur (USER <-> ADMIN)
     */
    @Transactional
    public UserDto modifierRole(Long id, String nouveauRole, String emailAdmin) {
        verifierAdmin(emailAdmin);

        UserEntity utilisateur = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + id));

        // Valider le rôle
        UserEntity.Role role;
        try {
            role = UserEntity.Role.valueOf(nouveauRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rôle invalide : " + nouveauRole + ". Valeurs acceptées : ADMIN, USER");
        }

        utilisateur.setRole(role);
        UserEntity utilisateurModifie = userRepository.save(utilisateur);

        return UserDto.fromEntity(utilisateurModifie);
    }

    /**
     * Obtenir des statistiques sur les utilisateurs
     */
    public Map<String, Object> obtenirStatistiques(String emailAdmin) {
        verifierAdmin(emailAdmin);

        List<UserEntity> tousLesUtilisateurs = userRepository.findAll();

        long nombreTotal = tousLesUtilisateurs.size();
        long nombreAdmins = tousLesUtilisateurs.stream()
                .filter(u -> u.getRole() == UserEntity.Role.ADMIN)
                .count();
        long nombreUsers = tousLesUtilisateurs.stream()
                .filter(u -> u.getRole() == UserEntity.Role.USER)
                .count();

        // Utilisateurs créés dans les 7 derniers jours
        LocalDateTime il7Jours = LocalDateTime.now().minusDays(7);
        long nouveauxUtilisateurs = tousLesUtilisateurs.stream()
                .filter(u -> u.getDateCreation().isAfter(il7Jours))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("nombreTotal", nombreTotal);
        stats.put("nombreAdmins", nombreAdmins);
        stats.put("nombreUsers", nombreUsers);
        stats.put("nouveauxUtilisateurs7Jours", nouveauxUtilisateurs);

        return stats;
    }
}
