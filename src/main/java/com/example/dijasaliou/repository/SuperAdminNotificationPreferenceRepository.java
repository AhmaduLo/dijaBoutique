package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.NotificationType;
import com.example.dijasaliou.entity.SuperAdminNotificationPreference;
import com.example.dijasaliou.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuperAdminNotificationPreferenceRepository
        extends JpaRepository<SuperAdminNotificationPreference, Long> {

    List<SuperAdminNotificationPreference> findByUser(UserEntity user);

    Optional<SuperAdminNotificationPreference> findByUserAndType(UserEntity user, NotificationType type);

    List<SuperAdminNotificationPreference> findByTypeAndEnabledTrue(NotificationType type);
}
