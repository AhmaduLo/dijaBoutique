package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Trace les produits archivés (en rupture de stock depuis 30+ jours).
 * Un produit archivé disparaît de la liste stock principale mais reste consultable.
 * Si un achat est enregistré pour ce produit → désarchivage automatique.
 */
@Entity
@Table(name = "produits_archives", indexes = {
        @Index(name = "idx_archive_tenant", columnList = "tenant_id"),
        @Index(name = "idx_archive_nom", columnList = "nom_produit")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_archive_tenant_produit", columnNames = {"tenant_id", "nom_produit"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitArchiveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Column(name = "nom_produit", nullable = false, length = 100)
    private String nomProduit;

    @Column(name = "date_rupture", nullable = false)
    private LocalDateTime dateRupture;

    @Column(name = "date_archivage")
    private LocalDateTime dateArchivage;
}
