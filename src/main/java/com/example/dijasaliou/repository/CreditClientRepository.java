package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.ClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity;
import com.example.dijasaliou.entity.CreditClientEntity.StatutCredit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CreditClientRepository extends JpaRepository<CreditClientEntity, String>,
        JpaSpecificationExecutor<CreditClientEntity> {

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CreditClientEntity c WHERE c.id = :id")
    java.util.Optional<CreditClientEntity> findByIdForUpdate(@Param("id") String id);

    List<CreditClientEntity> findByClientOrderByCreatedDateAsc(ClientEntity client);

    List<CreditClientEntity> findByVenteId(String venteId);

    /**
     * Charge les crédits d'une vente avec leurs paiements en une seule requête (évite N+1).
     * Utiliser à la place de findByVenteId() quand on accède à credit.getPaiements().
     */
    @Query("SELECT DISTINCT c FROM CreditClientEntity c LEFT JOIN FETCH c.paiements WHERE c.vente.id = :venteId")
    List<CreditClientEntity> findByVenteIdWithPaiements(@Param("venteId") String venteId);

    boolean existsByVenteIdAndStatutIn(String venteId, List<StatutCredit> statuts);

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
    long countCreditsActifsByClientId(@Param("clientId") String clientId, @Param("statut") StatutCredit statut, @Param("tenantUuid") String tenantUuid);

    /**
     * Crédits passés en perte sur une période (date de passage en perte dans [debut, fin]).
     * Charge la vente en FETCH pour permettre le calcul prorata FIFO côté service.
     */
    @Query("SELECT c FROM CreditClientEntity c " +
           "LEFT JOIN FETCH c.vente v " +
           "WHERE c.statut = com.example.dijasaliou.entity.CreditClientEntity.StatutCredit.PERTE " +
           "AND c.tenant.tenantUuid = :tenantUuid " +
           "AND c.datePassageEnPerte BETWEEN :debut AND :fin")
    List<CreditClientEntity> findCreditsPassesEnPerteBetween(@Param("debut") LocalDate debut,
                                                             @Param("fin") LocalDate fin,
                                                             @Param("tenantUuid") String tenantUuid);

    /**
     * Compte les crédits actifs pour une liste de clients en une seule requête.
     * Évite le problème N+1 de ClientService (1 requête au lieu de 1 par client).
     * Retourne Object[] : [clientId, count]
     */
    @Query("SELECT c.client.id, COUNT(c) FROM CreditClientEntity c " +
           "WHERE c.client.id IN :clientIds AND c.statut != :statut AND c.tenant.tenantUuid = :tenantUuid " +
           "GROUP BY c.client.id")
    List<Object[]> countCreditsActifsByClientIds(@Param("clientIds") List<String> clientIds,
                                                  @Param("statut") StatutCredit statut,
                                                  @Param("tenantUuid") String tenantUuid);

    /**
     * Somme le montant restant dû sur les crédits non soldés dont la vente a été créée dans la période.
     * Retourne List<Object[]> : chaque Object[] = [count, sum(montantRestant)]
     */
    @Query("SELECT COUNT(c), COALESCE(SUM(c.montantRestant), 0) " +
           "FROM CreditClientEntity c " +
           "WHERE c.vente IS NOT NULL " +
           "AND c.vente.dateVente BETWEEN :debut AND :fin " +
           "AND c.statut <> :statut " +
           "AND c.tenant.tenantUuid = :tenantUuid")
    List<Object[]> sumCreditsRestantParPeriode(@Param("debut") LocalDateTime debut,
                                               @Param("fin") LocalDateTime fin,
                                               @Param("statut") StatutCredit statut,
                                               @Param("tenantUuid") String tenantUuid);
}
