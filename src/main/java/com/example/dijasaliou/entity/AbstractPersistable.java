package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Classe abstraite de base pour toutes les entités persistantes
 * Contient uniquement l'ID (clé primaire)
 *
 * @MappedSuperclass : Les champs sont hérités mais cette classe
 * n'est PAS une table en base de données
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractPersistable implements Serializable {

    /**
     * Identifiant unique de l'entité
     * Généré automatiquement par MySQL (AUTO_INCREMENT)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Vérifie si l'entité est nouvelle (pas encore en base)
     */
    public boolean isNew() {
        return this.id == null;
    }
}
