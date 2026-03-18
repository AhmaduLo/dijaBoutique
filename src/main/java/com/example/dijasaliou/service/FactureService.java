package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.FactureDto;
import com.example.dijasaliou.entity.FactureEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.repository.FactureRepository;
import com.example.dijasaliou.repository.TenantRepository;
import com.example.dijasaliou.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des factures d'abonnement
 *
 * Chaque changement de plan crée automatiquement une facture.
 * Le SUPER_ADMIN peut envoyer n'importe quelle facture par email.
 */
@Service
@Slf4j
public class FactureService {

    private final FactureRepository factureRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public FactureService(FactureRepository factureRepository,
                          TenantRepository tenantRepository,
                          UserRepository userRepository,
                          EmailService emailService) {
        this.factureRepository = factureRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Liste toutes les factures d'un tenant
     */
    public List<FactureDto> getFacturesByTenant(Long tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé : " + tenantId));
        return factureRepository.findByTenantOrderByDateFactureDesc(tenant)
                .stream()
                .map(FactureDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Crée une facture lors d'un changement de plan (par SUPER_ADMIN)
     *
     * @param tenant    Le tenant concerné
     * @param plan      Le nouveau plan souscrit
     * @param jours     Durée en jours
     * @param statut    MANUELLE (super admin) ou PAYEE (paiement Wave)
     * @param waveId    ID transaction Wave (null si MANUELLE)
     * @return La facture créée
     */
    @Transactional
    public FactureEntity creerFacture(TenantEntity tenant, TenantEntity.Plan plan, int jours,
                                      FactureEntity.StatutFacture statut, String waveId) {

        UserEntity admin = userRepository
                .findFirstByTenantAndRole(tenant, UserEntity.Role.ADMIN)
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();
        String numeroFacture = genererNumeroFacture();

        FactureEntity facture = FactureEntity.builder()
                .numeroFacture(numeroFacture)
                .tenant(tenant)
                // Snapshot client
                .nomEntreprise(tenant.getNomEntreprise())
                .adresse(tenant.getAdresse())
                .ville(tenant.getVille())
                .pays(tenant.getPays())
                .nineaSiret(tenant.getNineaSiret())
                .adminEmail(admin != null ? admin.getEmail() : null)
                .adminNom(admin != null ? admin.getNom() : null)
                .adminPrenom(admin != null ? admin.getPrenom() : null)
                .adminTelephone(admin != null ? admin.getNumeroTelephone() : null)
                // Facturation
                .plan(plan.name())
                .montantCFA(plan.getPrixCFA())
                .montantEuro(plan.getPrixEuro())
                .dateFacture(now)
                .dateDebutPeriode(now)
                .dateFinPeriode(now.plusDays(jours))
                .statut(statut)
                .waveTransactionId(waveId)
                .emailEnvoye(false)
                .build();

        FactureEntity saved = factureRepository.save(facture);
        log.info("[FACTURE] Créée : {} — Tenant: {} — Plan: {} — {} jours — {}",
                numeroFacture, tenant.getNomEntreprise(), plan.name(), jours, statut);
        return saved;
    }

    /**
     * Envoie une facture par email au client
     */
    @Transactional
    public FactureDto envoyerFactureParEmail(Long factureId) {
        FactureEntity facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée : " + factureId));

        if (facture.getAdminEmail() == null || facture.getAdminEmail().isBlank()) {
            throw new RuntimeException("Pas d'email admin pour cette facture");
        }

        emailService.sendFactureEmail(facture);

        facture.setEmailEnvoye(true);
        facture.setDateEnvoiEmail(LocalDateTime.now());
        factureRepository.save(facture);

        log.info("[FACTURE] Envoyée par email : {} → {}", facture.getNumeroFacture(), facture.getAdminEmail());
        return FactureDto.fromEntity(facture);
    }

    // ==================== PRIVÉ ====================

    /**
     * Génère un numéro de facture unique : FAC-202503-0001
     */
    private String genererNumeroFacture() {
        String prefix = "FAC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        long count = factureRepository.countByNumeroFactureStartingWith(prefix);
        return prefix + String.format("%04d", count + 1);
    }
}
