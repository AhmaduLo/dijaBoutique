package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.UserPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPushSubscriptionRepository extends JpaRepository<UserPushSubscription, Long> {

    Optional<UserPushSubscription> findByEndpoint(String endpoint);

    List<UserPushSubscription> findByUser(UserEntity user);

    /**
     * Toutes les subscriptions d'un tenant — utilisées pour cibler tous
     * les appareils des admins de ce tenant lors d'un envoi push.
     */
    List<UserPushSubscription> findByTenant(TenantEntity tenant);

    /**
     * Utilisateurs distincts ayant au moins une subscription active.
     * Utilisé par les crons de résumé pour ne calculer les stats qu'une fois
     * par user (un user peut avoir plusieurs appareils = plusieurs subscriptions).
     */
    @Query("SELECT DISTINCT s.user FROM UserPushSubscription s")
    List<UserEntity> findDistinctSubscribedUsers();

    void deleteByEndpoint(String endpoint);
}
