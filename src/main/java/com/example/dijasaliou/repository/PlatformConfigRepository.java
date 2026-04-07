package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.PlatformConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformConfigRepository extends JpaRepository<PlatformConfigEntity, Long> {
    Optional<PlatformConfigEntity> findByCle(String cle);
}
