package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Table des catégories de produits — gérée par le super admin.
 * Utilisée dans produit_reference pour classifier les produits scannés.
 */
@Entity
@Table(name = "categories_reference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorieReferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    @Size(max = 100)
    @Column(name = "nom", nullable = false, unique = true, length = 100)
    private String nom;

    @Column(name = "ordre", nullable = false)
    @Builder.Default
    private Integer ordre = 0;
}
