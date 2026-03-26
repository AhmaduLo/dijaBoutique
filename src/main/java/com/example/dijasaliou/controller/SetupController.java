package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/setup")
@Slf4j
public class SetupController {

    @Value("${setup.secret.key:disabled}")
    private String setupSecretKey;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SetupController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/super-admin")
    public ResponseEntity<Map<String, String>> setupSuperAdmin(
            @RequestHeader(value = "X-Setup-Key", required = false) String setupKey,
            @RequestBody Map<String, String> body) {

        // Vérifier la clé secrète
        if (setupKey == null || !setupKey.equals(setupSecretKey) || "disabled".equals(setupSecretKey)) {
            log.warn("[SETUP] Tentative d'accès avec clé invalide");
            return ResponseEntity.status(403).body(Map.of("message", "Clé invalide ou absente"));
        }

        String email = body.get("email");
        String motDePasse = body.get("motDePasse");
        String nom = body.getOrDefault("nom", "Admin");
        String prenom = body.getOrDefault("prenom", "Super");

        if (email == null || motDePasse == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "email et motDePasse sont requis"));
        }

        String hashedPassword = passwordEncoder.encode(motDePasse);

        Optional<UserEntity> existingSuperAdmin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserEntity.Role.SUPER_ADMIN)
                .findFirst();

        if (existingSuperAdmin.isPresent()) {
            UserEntity superAdmin = existingSuperAdmin.get();
            superAdmin.setEmail(email);
            superAdmin.setMotDePasse(hashedPassword);
            superAdmin.setNom(nom);
            superAdmin.setPrenom(prenom);
            userRepository.save(superAdmin);
            log.info("[SETUP] Super admin mis à jour : {}", email);
            return ResponseEntity.ok(Map.of("message", "Super admin mis à jour avec succès"));
        } else {
            UserEntity superAdmin = UserEntity.builder()
                    .email(email)
                    .motDePasse(hashedPassword)
                    .nom(nom)
                    .prenom(prenom)
                    .nomEntreprise("HeasyStock")
                    .numeroTelephone("+221000000000")
                    .role(UserEntity.Role.SUPER_ADMIN)
                    .deleted(false)
                    .acceptationCGU(true)
                    .acceptationPolitiqueConfidentialite(true)
                    .dateCreation(LocalDateTime.now())
                    .tenant(null)
                    .build();
            userRepository.save(superAdmin);
            log.info("[SETUP] Super admin créé : {}", email);
            return ResponseEntity.ok(Map.of("message", "Super admin créé avec succès"));
        }
    }
}
