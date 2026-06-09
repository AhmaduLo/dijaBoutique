package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.NotificationPreferenceDto;
import com.example.dijasaliou.entity.NotificationType;
import com.example.dijasaliou.entity.SuperAdminNotificationPreference;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.SuperAdminNotificationPreferenceRepository;
import com.example.dijasaliou.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gère les préférences de notifications des super admins.
 *
 * Règle d'or : si pas de préférence enregistrée pour un (user, type) →
 * on applique la valeur par défaut définie dans NotificationType.isDefautActif().
 * Ça évite de devoir initialiser la table pour chaque nouveau super admin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceService {

    private final SuperAdminNotificationPreferenceRepository repository;
    private final UserRepository userRepository;

    /**
     * Retourne la liste complète des notifications avec leur état activé/désactivé
     * pour un super admin donné. Toujours toutes les valeurs de l'enum.
     */
    @Transactional(readOnly = true)
    public List<NotificationPreferenceDto> getPreferences(String userEmail) {
        UserEntity user = userRepository.findByEmailAndDeletedFalse(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + userEmail));

        Map<NotificationType, Boolean> stored = repository.findByUser(user).stream()
                .collect(Collectors.toMap(
                        SuperAdminNotificationPreference::getType,
                        SuperAdminNotificationPreference::isEnabled));

        return Arrays.stream(NotificationType.values())
                .map(t -> NotificationPreferenceDto.from(t, stored.getOrDefault(t, t.isDefautActif())))
                .toList();
    }

    /**
     * Active ou désactive une notification pour un super admin.
     */
    @Transactional
    public void updatePreference(String userEmail, NotificationType type, boolean enabled) {
        UserEntity user = userRepository.findByEmailAndDeletedFalse(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + userEmail));

        SuperAdminNotificationPreference pref = repository.findByUserAndType(user, type)
                .orElseGet(() -> SuperAdminNotificationPreference.builder()
                        .user(user)
                        .type(type)
                        .enabled(enabled)
                        .build());
        pref.setEnabled(enabled);
        pref.setUpdatedAt(LocalDateTime.now());
        repository.save(pref);
        log.info("[NOTIF_PREF] {} : {} -> {}", userEmail, type, enabled);
    }

    /**
     * Vrai si AU MOINS UN super admin a cette notification activée
     * (sert à savoir si on doit envoyer le push à tous les abonnés).
     */
    @Transactional(readOnly = true)
    public boolean isAnyoneSubscribedTo(NotificationType type) {
        // S'il existe des prefs explicites enabled=true → oui
        if (!repository.findByTypeAndEnabledTrue(type).isEmpty()) return true;
        // Sinon, on retombe sur le défaut (si défaut = true → considéré actif)
        return type.isDefautActif();
    }
}
