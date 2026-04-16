package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findAllByOrderByDateEnvoiDesc(Pageable pageable);

    /**
     * Notifications non lues pour un tenant donné :
     * - globales (tenant_id IS NULL) OU ciblées pour ce tenant
     * - filtre par plan si renseigné
     * - pas encore marquées comme lues par ce tenant
     */
    @Query("SELECT n FROM NotificationEntity n WHERE " +
           "(n.tenant IS NULL OR n.tenant.id = :tenantId) AND " +
           "(n.filtrePlan IS NULL OR n.filtrePlan = :plan) AND " +
           "n.canalApp = true AND " +
           "n.id NOT IN (SELECT nl.notification.id FROM NotificationLueEntity nl WHERE nl.tenant.id = :tenantId) " +
           "ORDER BY n.dateEnvoi DESC")
    List<NotificationEntity> findNonLuesParTenant(@Param("tenantId") Long tenantId, @Param("plan") String plan);
}
