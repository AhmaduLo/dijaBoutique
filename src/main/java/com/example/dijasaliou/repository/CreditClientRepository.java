package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.ClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity.StatutCredit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CreditClientRepository extends JpaRepository<CreditClientEntity, Long> {

    @Query(value = "SELECT c FROM CreditClientEntity c WHERE " +
           "(:statut IS NULL OR c.statut = :statut) AND " +
           "(:search IS NULL OR LOWER(c.client.nom) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(c) FROM CreditClientEntity c WHERE " +
           "(:statut IS NULL OR c.statut = :statut) AND " +
           "(:search IS NULL OR LOWER(c.client.nom) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CreditClientEntity> findAllWithSearch(
            @Param("statut") StatutCredit statut,
            @Param("search") String search,
            Pageable pageable);

    List<CreditClientEntity> findByClientOrderByCreatedDateAsc(ClientEntity client);

    @Query("SELECT COALESCE(SUM(c.montantRestant), 0) FROM CreditClientEntity c WHERE c.statut != :statut")
    BigDecimal sumMontantRestantActif(@Param("statut") StatutCredit statut);

    @Query("SELECT COALESCE(SUM(c.montantInitial), 0) FROM CreditClientEntity c WHERE c.statut != :statut")
    BigDecimal sumMontantInitialActif(@Param("statut") StatutCredit statut);

    @Query("SELECT COUNT(c) FROM CreditClientEntity c WHERE c.statut != :statut")
    long countCreditsActifs(@Param("statut") StatutCredit statut);

    @Query("SELECT COUNT(DISTINCT c.client) FROM CreditClientEntity c WHERE c.statut != :statut")
    long countClientsCrediteurs(@Param("statut") StatutCredit statut);

    @Query("SELECT COUNT(c) FROM CreditClientEntity c WHERE c.statut != :statut AND c.dateEcheance IS NOT NULL AND c.dateEcheance < :today")
    long countCreditsEnRetard(@Param("statut") StatutCredit statut, @Param("today") LocalDate today);

    @Query("SELECT COUNT(c) FROM CreditClientEntity c WHERE c.client.id = :clientId AND c.statut != :statut")
    long countCreditsActifsByClientId(@Param("clientId") Long clientId, @Param("statut") StatutCredit statut);
}
