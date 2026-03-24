package com.example.dijasaliou.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Classe abstraite de base pour toutes les entités persistantes.
 * Chaque sous-classe déclare son propre @Id (Long IDENTITY ou String UUID).
 *
 * @MappedSuperclass : Les champs sont hérités mais cette classe
 * n'est PAS une table en base de données
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractPersistable implements Serializable {
}
