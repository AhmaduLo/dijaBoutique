package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.UserNotificationPreference;
import com.example.dijasaliou.entity.UserNotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationPreferenceRepository
        extends JpaRepository<UserNotificationPreference, Long> {

    List<UserNotificationPreference> findByUser(UserEntity user);

    Optional<UserNotificationPreference> findByUserAndType(UserEntity user, UserNotificationType type);

    /**
     * Retourne les préférences activées d'un tenant pour un type donné.
     * Utile pour cibler tous les users d'un tenant qui reçoivent ce type.
     */
    @Query("SELECT p FROM UserNotificationPreference p " +
           "WHERE p.tenant = :tenant AND p.type = :type AND p.enabled = true")
    List<UserNotificationPreference> findEnabledByTenantAndType(
            @Param("tenant") TenantEntity tenant,
            @Param("type") UserNotificationType type);
}
