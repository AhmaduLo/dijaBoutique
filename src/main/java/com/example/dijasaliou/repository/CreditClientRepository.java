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
           "(:search IS NULL OR LOWER(c.client.nom) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "c.tenant.tenantUuid = :tenantUuid",
           countQuery = "SELECT COUNT(c) FROM CreditClientEntity c WHERE " +
           "(:statut IS NULL OR c.statut = :statut) AND " +
           "(:search IS NULL OR LOWER(c.client.nom) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "c.tenant.tenantUuid = :tenantUuid")
    Page<CreditClientEntity> findAllWithSearch(
            @Param("statut") StatutCredit statut,
            @Param("search") String search,
            @Param("tenantUuid") String tenantUuid,
            Pageable pageable);

    List<CreditClientEntity> findByClientOrderByCreatedDateAsc(ClientEntity client);

    List<CreditClientEntity> findByVenteId(Long venteId);

    @Query("SELECT COALESCE(SUM(c.montantRestant), 0) FROM CreditClientEntity c WHERE c.statut != :statut AND c.tenant.tenantUuid = :tenantUuid")
    BigDecimal sumMontantRestantActif(@Param("statut") StatutCredit statut, @Param("tenantUuid") String tenantUuid);

    @Query("SELECT COALESCE(SUM(c.montantInitial), 0) FROM CreditClientEntity c WHERE c.statut != :statut AND c.tenant.tenantUuid = :tenantUuid")
    BigDecimal sumMontantInitialActif(@Param("statut") StatutCredit statut, @Param("tenantUuid") String tenantUuid);

    @Query("SELECT COUNT(c) FROM CreditClientEntity c WHERE c.statut != :statut AND c.tenant.tenantUuid = :tenantUuid")
    long countCreditsActifs(@Param("statut") StatutCredit statut, @Param("tenantUuid") String tenantUuid);

    @Query("SELECT COUNT(DISTINCT c.client) FROM CreditClientEntity c WHERE c.statut != :statut AND c.tenant.tenantUuid = :tenantUuid")
    long countClientsCrediteurs(@Param("statut") StatutCredit statut, @Param("tenantUuid") String tenantUuid);

    @Query("SELECT COUNT(c) FROM CreditClientEntity c WHERE c.statut != :statut AND c.dateEcheance IS NOT NULL AND c.dateEcheance < :today AND c.tenant.tenantUuid = :tenantUuid")
    long countCreditsEnRetard(@Param("statut") StatutCredit statut, @Param("today") LocalDate today, @Param("tenantUuid") String tenantUuid);

    @Query("SELECT COUNT(c) FROM CreditClientEntity c WHERE c.client.id = :clientId AND c.statut != :statut AND c.tenant.tenantUuid = :tenantUuid")
    long countCreditsActifsByClientId(@Param("clientId") Long clientId, @Param("statut") StatutCredit statut, @Param("tenantUuid") String tenantUuid);
}
