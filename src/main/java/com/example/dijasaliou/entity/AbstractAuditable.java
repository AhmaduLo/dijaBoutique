package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Classe abstraite ajoutant les champs d'audit
 *
 * AUDIT = Traçabilité des modifications
 * - Qui a créé ? Quand ?
 * - Qui a modifié ? Quand ?
 *
 * Hérite de AbstractPersistable (donc a l'ID)
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractAuditable extends AbstractPersistable {


    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    /**
     * Optionnel : Qui a créé l'enregistrement ?
     * On peut ajouter avec Spring Security plus tard
     */

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

}
