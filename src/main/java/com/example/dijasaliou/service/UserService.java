package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service pour la logique métier des utilisateurs
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TenantService tenantService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantService = tenantService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Récupérer tous les utilisateurs actifs (non supprimés)
     */
    public List<UserEntity> obtenirTousLesUtilisateurs() {
        return userRepository.findByDeletedFalse();
    }

    /**
     * Récupérer un utilisateur par ID
     * Note: Peut retourner des utilisateurs supprimés (pour les besoins internes)
     */
    public UserEntity obtenirUtilisateurParId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + id));
    }

    /**
     * Récupérer un utilisateur actif par email (non supprimé)
     */
    public UserEntity obtenirUtilisateurParEmail(String email) {
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email : " + email));
    }

    /**
     * Créer un nouvel utilisateur
     */
    public UserEntity creerUtilisateur(UserEntity utilisateur) {
        // Validation : Email unique — requête native pour bypasser le filtre Hibernate tenant
        // (la contrainte UNIQUE sur email est globale en base, indépendante du tenant)
        if (userRepository.existsByEmailGlobal(utilisateur.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé par un autre compte.");
        }

        // Validation : Champs obligatoires
        validerUtilisateur(utilisateur);

        // Hasher le mot de passe avec BCrypt
        if (utilisateur.getMotDePasse() != null && !utilisateur.getMotDePasse().isEmpty()) {
            utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        }

        // MULTI-TENANT : Assigner le tenant actuel (CRUCIAL!)
        utilisateur.setTenant(tenantService.getCurrentTenant());

        return userRepository.save(utilisateur);
    }

    /**
     * Modifier un utilisateur
     */
    public UserEntity modifierUtilisateur(Long id, UserEntity utilisateurModifie) {
        UserEntity utilisateurExistant = obtenirUtilisateurParId(id);

        // Validation
        validerUtilisateur(utilisateurModifie);

        // Mise à jour
        utilisateurExistant.setNom(utilisateurModifie.getNom());
        utilisateurExistant.setPrenom(utilisateurModifie.getPrenom());
        utilisateurExistant.setEmail(utilisateurModifie.getEmail());
        utilisateurExistant.setRole(utilisateurModifie.getRole());

        return userRepository.save(utilisateurExistant);
    }

    /**
     * Supprimer un utilisateur
     */
    public void supprimerUtilisateur(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé avec l'ID : " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * VALIDATION
     */
    private void validerUtilisateur(UserEntity utilisateur) {
        if (utilisateur.getNom() == null || utilisateur.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom est obligatoire");
        }

        if (utilisateur.getPrenom() == null || utilisateur.getPrenom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le prénom est obligatoire");
        }

        if (utilisateur.getEmail() == null || utilisateur.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }

        // Validation format email
        if (!utilisateur.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Format d'email invalide");
        }
    }
}