package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.NotificationLueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLueRepository extends JpaRepository<NotificationLueEntity, Long> {

    boolean existsByNotificationIdAndTenantId(Long notificationId, Long tenantId);
}
