package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.ClientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, Long> {

    @Query("SELECT c FROM ClientEntity c WHERE " +
           "(:search IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.telephone) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<ClientEntity> findAllWithSearch(@Param("search") String search);

    @Query("SELECT c FROM ClientEntity c WHERE " +
           "(:search IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.telephone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ClientEntity> findAllWithSearchPaged(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM ClientEntity c WHERE c.detteTotale > 0 ORDER BY c.detteTotale DESC")
    List<ClientEntity> findClientsAvecDette();

    boolean existsByTelephone(String telephone);
}
