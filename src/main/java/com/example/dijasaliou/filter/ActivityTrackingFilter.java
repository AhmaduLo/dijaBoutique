package com.example.dijasaliou.filter;

import com.example.dijasaliou.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mise à jour de derniereConnexion à chaque requête authentifiée.
 *
 * PROBLÈME résolu : un utilisateur qui reste connecté toute la journée
 * (token valide 24h) ne met à jour sa dernière activité que lors du login.
 * Ce filtre corrige ça en mettant à jour sur chaque appel API.
 *
 * THROTTLE : au maximum 1 écriture BDD par utilisateur toutes les 5 minutes
 * pour ne pas générer une écriture à chaque requête HTTP.
 *
 * S'exécute APRÈS JwtAuthenticationFilter (SecurityContext déjà populé).
 */
@Component
@Slf4j
public class ActivityTrackingFilter extends OncePerRequestFilter {

    private static final long THROTTLE_MINUTES = 5;

    private final UserRepository userRepository;

    /** Cache in-process : email → dernière écriture en BDD */
    private final ConcurrentHashMap<String, LocalDateTime> lastWritten = new ConcurrentHashMap<>();

    public ActivityTrackingFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {

            String email = auth.getName();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastUpdate = lastWritten.get(email);

            if (lastUpdate == null || lastUpdate.plusMinutes(THROTTLE_MINUTES).isBefore(now)) {
                try {
                    userRepository.updateDerniereConnexion(email, now);
                    lastWritten.put(email, now);
                } catch (Exception e) {
                    // Ne pas bloquer la requête si la mise à jour échoue
                    log.debug("[ActivityTracking] Impossible de mettre à jour derniereConnexion pour {} : {}", email, e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
