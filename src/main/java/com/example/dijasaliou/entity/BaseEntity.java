package com.example.dijasaliou.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Classe de base finale ajoutant le versioning (Optimistic Locking)
 *
 * Hérite de AbstractAuditable (donc a : id, createdDate, lastModifiedDate)
 *
 * LIFECYCLE CALLBACKS CENTRALISÉS ICI
 * Les classes enfants ne doivent PAS redéfinir @PrePersist/@PreUpdate
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity extends AbstractAuditable {

    /**
     * Version pour l'optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    /*
      CALLBACKS COMMUNS POUR TOUTES LES ENTITÉS
      Ces méthodes sont appelées automatiquement par JPA
      AVANT insert/update pour TOUTES les entités qui héritent
     */

    /**
     * Appelé AVANT l'insertion en base
     * Utile pour validation ou initialisation
     */
    @PrePersist
    protected void onBasePersist() {
        // Validation commune à toutes les entités
        // Les sous-classes peuvent surcharger avec leur propre logique
        beforePersist();
    }

    /**
     * Appelé AVANT la mise à jour en base
     */
    @PreUpdate
    protected void onBaseUpdate() {
        // Validation commune à toutes les entités
        beforeUpdate();
    }

    /**
     * Méthode à surcharger dans les sous-classes
     * pour ajouter une logique spécifique AVANT persist
     */
    protected void beforePersist() {
        // Implémentation par défaut vide
        // Les sous-classes peuvent override
    }

    /**
     * Méthode à surcharger dans les sous-classes
     * pour ajouter une logique spécifique AVANT update
     */
    protected void beforeUpdate() {
        // Implémentation par défaut vide
        // Les sous-classes peuvent override
    }
}
