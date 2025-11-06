package com.example.dijasaliou.repository;

import com.example.dijasaliou.entity.AchatEntity;
import com.example.dijasaliou.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AchatRepository extends JpaRepository<AchatEntity, Long> {

    /**Trouver tous les achats d'un produit*/
    List<AchatEntity> findByNomProduit(String nomProduit);

    /**
     * Trouver les achats contenant un mot dans le nom
     */
    List<AchatEntity> findByNomProduitContaining(String keyword);

    /**
     * Trouver tous les achats d'un utilisateur
     */
    List<AchatEntity> findByUtilisateur(UserEntity utilisateur);

    /**
     * Trouver les achats d'une date précise
     */
    List<AchatEntity> findByDateAchat(LocalDate date);

    /**
     * Trouver les achats entre deux dates
     */
    List<AchatEntity> findByDateAchatBetween(LocalDate debut, LocalDate fin);

    /**
     * Compter les achats d'un produit
     */
    long countByNomProduit(String nomProduit);

    /**
     * Supprimer tous les achats d'un utilisateur
     */
    void deleteByUtilisateur(UserEntity utilisateur);

}