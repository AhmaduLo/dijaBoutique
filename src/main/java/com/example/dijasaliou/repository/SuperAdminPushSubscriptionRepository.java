package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.SuperAdminPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuperAdminPushSubscriptionRepository extends JpaRepository<SuperAdminPushSubscription, Long> {

    Optional<SuperAdminPushSubscription> findByEndpoint(String endpoint);

    List<SuperAdminPushSubscription> findAll();

    void deleteByEndpoint(String endpoint);
}
