package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.CreditClientEntity;
import com.example.dijasaliou.entity.PaiementCreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaiementCreditRepository extends JpaRepository<PaiementCreditEntity, Long> {

    List<PaiementCreditEntity> findByCreditOrderByCreatedDateDesc(CreditClientEntity credit);
}
