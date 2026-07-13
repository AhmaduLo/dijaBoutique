package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ResumeHebdoConfig;
import com.example.dijasaliou.dto.ResumeMensuelConfig;
import com.example.dijasaliou.dto.ResumeQuotidienConfig;
import com.example.dijasaliou.dto.SeuilMontantConfig;
import com.example.dijasaliou.dto.UserNotificationPreferenceDto;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.UserNotificationPreference;
import com.example.dijasaliou.entity.UserNotificationType;
import com.example.dijasaliou.repository.UserNotificationPreferenceRepository;
import com.example.dijasaliou.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Gère les préférences de notifications des utilisateurs (admin d'un tenant).
 *
 * Règle d'or : si aucune préférence n'est enregistrée pour un (user, type) →
 * on applique la valeur par défaut définie dans {@link UserNotificationType#isDefautActif()}.
 * Aucune initialisation en base nécessaire à la création d'un compte.
 *
 * Miroir de {@link NotificationPreferenceService} (super admins) — même pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserNotificationPreferenceService {

    private final UserNotificationPreferenceRepository repository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Retourne la liste complète des types + leur état activé/désactivé
     * pour un utilisateur donné (tous les types de l'enum, pas seulement ceux en base).
     */
    @Transactional(readOnly = true)
    public List<UserNotificationPreferenceDto> getPreferences(String userEmail) {
        UserEntity user = userRepository.findByEmailAndDeletedFalse(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + userEmail));

        // On filtre les préférences orphelines (type == null : valeur en base plus
        // reconnue par l'enum, gérée par UserNotificationTypeConverter).
        Map<UserNotificationType, UserNotificationPreference> stored = repository.findByUser(user).stream()
                .filter(p -> p.getType() != null)
                .collect(Collectors.toMap(
                        UserNotificationPreference::getType,
                        p -> p));

        return Arrays.stream(UserNotificationType.values())
                .map(t -> {
                    UserNotificationPreference existing = stored.get(t);
                    boolean enabled = existing != null ? existing.isEnabled() : t.isDefautActif();
                    String config = existing != null ? existing.getConfig() : null;
                    return UserNotificationPreferenceDto.from(t, enabled, config);
                })
                .toList();
    }

    /**
     * Active ou désactive un type de notification pour un utilisateur, avec config optionnel.
     * Si {@code configJson} est {@code null}, la valeur existante en base est conservée.
     */
    @Transactional
    public void updatePreference(String userEmail, UserNotificationType type, boolean enabled, String configJson) {
        UserEntity user = userRepository.findByEmailAndDeletedFalse(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + userEmail));

        TenantEntity tenant = user.getTenant();
        if (tenant == null) {
            throw new RuntimeException("L'utilisateur n'appartient à aucun tenant : " + userEmail);
        }

        UserNotificationPreference pref = repository.findByUserAndType(user, type)
                .orElseGet(() -> UserNotificationPreference.builder()
                        .user(user)
                        .tenant(tenant)
                        .type(type)
                        .enabled(enabled)
                        .build());
        pref.setEnabled(enabled);
        if (configJson != null) {
            pref.setConfig(configJson);
        }
        pref.setUpdatedAt(LocalDateTime.now());
        repository.save(pref);
        log.info("[USER_NOTIF_PREF] {} : {} -> enabled={} config={}",
                userEmail, type, enabled, configJson != null ? "updated" : "unchanged");
    }

    /**
     * Surcharge sans config — équivalent à {@code updatePreference(userEmail, type, enabled, null)}.
     */
    @Transactional
    public void updatePreference(String userEmail, UserNotificationType type, boolean enabled) {
        updatePreference(userEmail, type, enabled, null);
    }

    /**
     * Valide un bloc de config reçu du frontend contre le schéma du type,
     * puis le sérialise en JSON prêt à être persisté.
     * Lève {@link IllegalArgumentException} si le contenu est invalide.
     */
    public String validateAndSerializeConfig(UserNotificationType type, Object rawConfig) {
        try {
            switch (type) {
                case RESUME_QUOTIDIEN -> {
                    ResumeQuotidienConfig config = objectMapper.convertValue(rawConfig, ResumeQuotidienConfig.class);
                    return objectMapper.writeValueAsString(config);
                }
                case RESUME_HEBDO -> {
                    ResumeHebdoConfig config = objectMapper.convertValue(rawConfig, ResumeHebdoConfig.class);
                    return objectMapper.writeValueAsString(config);
                }
                case RESUME_MENSUEL -> {
                    ResumeMensuelConfig config = objectMapper.convertValue(rawConfig, ResumeMensuelConfig.class);
                    return objectMapper.writeValueAsString(config);
                }
                case VENTE_EMPLOYE, SORTIE_CAISSE_IMPORTANTE, NOUVEAU_CREDIT, ACHAT_EMPLOYE -> {
                    SeuilMontantConfig config = objectMapper.convertValue(rawConfig, SeuilMontantConfig.class);
                    return objectMapper.writeValueAsString(config);
                }
                default -> {
                    // Types sans schéma typé (STOCK_BAS, RUPTURE, VENTE_A_PERTE, etc.)
                    // acceptent le JSON générique mais ne l'utilisent pas.
                    return objectMapper.writeValueAsString(rawConfig);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Format de config invalide : " + e.getMessage(), e);
        }
    }

    /** Config typée pour RESUME_HEBDO — fallback aux défauts si absente / illisible. */
    @Transactional(readOnly = true)
    public ResumeHebdoConfig getResumeHebdoConfig(UserEntity user) {
        return readConfigOrDefault(user, UserNotificationType.RESUME_HEBDO,
                ResumeHebdoConfig.class, ResumeHebdoConfig.defaults());
    }

    /** Config typée pour RESUME_MENSUEL — fallback aux défauts si absente / illisible. */
    @Transactional(readOnly = true)
    public ResumeMensuelConfig getResumeMensuelConfig(UserEntity user) {
        return readConfigOrDefault(user, UserNotificationType.RESUME_MENSUEL,
                ResumeMensuelConfig.class, ResumeMensuelConfig.defaults());
    }

    /** Config typée "seuil montant" pour un type donné (VENTE_EMPLOYE, SORTIE_CAISSE_IMPORTANTE, NOUVEAU_CREDIT). */
    @Transactional(readOnly = true)
    public SeuilMontantConfig getSeuilMontantConfig(UserEntity user, UserNotificationType type) {
        return readConfigOrDefault(user, type, SeuilMontantConfig.class, SeuilMontantConfig.defaults());
    }

    private <T> T readConfigOrDefault(UserEntity user, UserNotificationType type, Class<T> clazz, T defaults) {
        Optional<UserNotificationPreference> pref = repository.findByUserAndType(user, type);
        String json = pref.map(UserNotificationPreference::getConfig).orElse(null);
        if (json == null || json.isBlank()) return defaults;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("[USER_NOTIF_PREF] Config {} illisible pour user={}, retour au défaut : {}",
                    type, user.getEmail(), e.getMessage());
            return defaults;
        }
    }

    /**
     * Retourne la config typée pour RESUME_QUOTIDIEN pour un user donné,
     * ou les valeurs par défaut si absente / illisible.
     */
    @Transactional(readOnly = true)
    public ResumeQuotidienConfig getResumeQuotidienConfig(UserEntity user) {
        return readConfigOrDefault(user, UserNotificationType.RESUME_QUOTIDIEN,
                ResumeQuotidienConfig.class, ResumeQuotidienConfig.defaults());
    }

    /**
     * Vrai si l'utilisateur donné a activé (ou laissé activé par défaut) ce type.
     * Utilisé par les émetteurs de push pour filtrer avant envoi.
     */
    @Transactional(readOnly = true)
    public boolean isEnabled(UserEntity user, UserNotificationType type) {
        return repository.findByUserAndType(user, type)
                .map(UserNotificationPreference::isEnabled)
                .orElse(type.isDefautActif());
    }
}
