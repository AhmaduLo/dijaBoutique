package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Service pour charger les utilisateurs depuis la base
 *
 * Spring Security l'utilise pour :
 * - Vérifier les credentials lors de la connexion
 * - Charger l'utilisateur lors de la validation du token JWT
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Charger un utilisateur par son email
     *
     * Spring Security appelle cette méthode automatiquement
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Chercher l'utilisateur dans la base
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));

        // 2. Convertir UserEntity en UserDetails (format Spring Security)
        // Note : On n'utilise PAS le préfixe "ROLE_" pour utiliser hasAuthority() dans SecurityConfig
        return User.builder()
                .username(user.getEmail())
                .password(user.getMotDePasse())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority(user.getRole().name())
                ))
                .build();
    }
}
