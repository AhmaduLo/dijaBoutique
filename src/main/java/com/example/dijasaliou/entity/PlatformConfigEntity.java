package com.example.dijasaliou.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Configuration globale de la plateforme HeasyStock.
 * Table non-tenant : partagée par toutes les boutiques.
 * Ex: numéro WhatsApp support, liens CGU, etc.
 */
@Entity
@Table(name = "platform_config")
@Getter
@Setter
public class PlatformConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cle", length = 50, unique = true, nullable = false)
    private String cle;

    @Column(name = "valeur", length = 255)
    private String valeur;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
