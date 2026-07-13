package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.DepenseDto;
import com.example.dijasaliou.dto.PagedResponse;
import com.example.dijasaliou.entity.DepenseEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.entity.UserNotificationType;
import com.example.dijasaliou.repository.DepenseRepository;
import com.example.dijasaliou.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service pour la logique métier des dépenses
 */
@Service
@Slf4j
public class DepenseService {

    private final DepenseRepository depenseRepository;
    private final TenantService tenantService;
    private final UserPushNotificationService userPushService;
    private final UserRepository userRepository;

    public DepenseService(DepenseRepository depenseRepository,
                          TenantService tenantService,
                          UserPushNotificationService userPushService,
                          UserRepository userRepository) {
        this.depenseRepository = depenseRepository;
        this.tenantService = tenantService;
        this.userPushService = userPushService;
        this.userRepository = userRepository;
    }


    /**
     * Récupérer toutes les dépenses du tenant courant (rapports/export)
     */
    @Transactional(readOnly = true)
    public List<DepenseEntity> obtenirToutesLesDepenses() {
        return depenseRepository.findAllByTenant(tenantService.getCurrentTenant());
    }

    /**
     * Récupérer les dépenses paginées avec recherche et filtre catégorie optionnels
     */
    @Transactional(readOnly = true)
    public PagedResponse<DepenseDto> obtenirDepensesPaginees(int page, int size, String search, String categorie) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateDepense"));
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        DepenseEntity.CategorieDepense categorieParam = null;
        if (categorie != null && !categorie.isBlank()) {
            try {
                categorieParam = DepenseEntity.CategorieDepense.valueOf(categorie);
            } catch (IllegalArgumentException e) {
                // Valeur inconnue : pas de filtre catégorie
            }
        }
        String tenantUuid = tenantService.getCurrentTenant().getTenantUuid();
        Page<DepenseEntity> depensesPage = depenseRepository.findAllWithSearch(tenantUuid, searchParam, categorieParam, pageable);
        Page<DepenseDto> dtoPage = depensesPage.map(DepenseDto::fromEntity);
        return PagedResponse.from(dtoPage);
    }

    /**
     * Récupérer une dépense par ID
     */
    @Transactional(readOnly = true)
    public DepenseEntity obtenirDepenseParId(String id) {
        return depenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dépense non trouvée avec l'ID : " + id));
    }

    /**
     * Créer une nouvelle dépense
     */
    public DepenseEntity creerDepense(DepenseEntity depense, UserEntity utilisateur) {
        // Validation
        validerDepense(depense);

        // Associer l'utilisateur
        depense.setUtilisateur(utilisateur);

        // MULTI-TENANT : Assigner le tenant actuel (CRUCIAL!)
        depense.setTenant(tenantService.getCurrentTenant());

        // DATE : Utiliser la date fournie par le frontend ; fallback = maintenant
        if (depense.getDateDepense() == null) {
            depense.setDateDepense(LocalDateTime.now());
        }

        DepenseEntity saved = depenseRepository.save(depense);

        // NOTIFICATION PUSH — DEPENSE_EMPLOYE si auteur ≠ ADMIN
        try {
            envoyerNotifDepenseEmploye(saved, utilisateur);
        } catch (Exception e) {
            log.warn("[DEPENSE_NOTIF] Echec envoi notif pour depense {} : {}",
                    saved.getId(), e.getMessage());
        }

        return saved;
    }

    /**
     * Notifie l'admin quand une dépense est saisie par un employé (non-ADMIN).
     * Le patron veut savoir tout de suite quand ses gérants ou vendeurs
     * enregistrent des sorties d'argent (contrôle interne).
     */
    private void envoyerNotifDepenseEmploye(DepenseEntity depense, UserEntity auteur) {
        if (auteur == null || auteur.getRole() == UserEntity.Role.ADMIN) return;
        TenantEntity tenant = depense.getTenant();
        if (tenant == null) return;

        UserEntity admin = userRepository.findFirstByTenantAndRole(tenant, UserEntity.Role.ADMIN).orElse(null);
        if (admin == null) return;

        String montantFmt = depense.getMontant() != null
                ? String.format("%,d", depense.getMontant().longValue()).replace(',', ' ')
                : "0";
        String title = "Dépense saisie par " + auteur.getPrenom();
        String categorieLabel = depense.getCategorie() != null ? depense.getCategorie().getLibelle() : null;
        String body = auteur.getPrenom() + " " + auteur.getNom() + " vient d'enregistrer une dépense de "
                + montantFmt + " CFA"
                + (categorieLabel != null && !categorieLabel.isBlank()
                    ? " (" + categorieLabel + ")." : ".");
        userPushService.notifyUser(admin, UserNotificationType.DEPENSE_EMPLOYE, title, body, "/depenses");
    }

    /**
     * Modifier une dépense
     */
    public DepenseEntity modifierDepense(String id, DepenseEntity depenseModifiee) {
        DepenseEntity depenseExistante = obtenirDepenseParId(id);

        // SÉCURITÉ : Vérifier que la dépense appartient au tenant actuel (double sécurité)
        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!depenseExistante.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        validerDepense(depenseModifiee);

        depenseExistante.setLibelle(depenseModifiee.getLibelle());
        depenseExistante.setMontant(depenseModifiee.getMontant());
        depenseExistante.setCategorie(depenseModifiee.getCategorie());
        depenseExistante.setEstRecurrente(depenseModifiee.getEstRecurrente());
        depenseExistante.setNotes(depenseModifiee.getNotes());
        depenseExistante.setUtilisateur(depenseModifiee.getUtilisateur());
        if (depenseModifiee.getDateDepense() != null) {
            depenseExistante.setDateDepense(depenseModifiee.getDateDepense());
        }
        // Caisse multi-comptes : permettre la modification du mode de paiement
        if (depenseModifiee.getModePaiement() != null) {
            depenseExistante.setModePaiement(depenseModifiee.getModePaiement());
        }

        return depenseRepository.save(depenseExistante);
    }

    /**
     * Supprimer une dépense
     */
    public void supprimerDepense(String id) {
        // Récupérer la dépense existante
        DepenseEntity depenseExistante = obtenirDepenseParId(id);

        // SÉCURITÉ : Vérifier que la dépense appartient au tenant actuel (double sécurité)
        TenantEntity tenantActuel = tenantService.getCurrentTenant();
        if (!depenseExistante.getTenant().getTenantUuid().equals(tenantActuel.getTenantUuid())) {
            throw new SecurityException("Accès refusé : cette ressource ne vous appartient pas");
        }

        depenseRepository.deleteById(id);
    }

    /**
     * Récupérer les dépenses d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<DepenseEntity> obtenirDepensesParUtilisateur(UserEntity utilisateur) {
        return depenseRepository.findByUtilisateur(utilisateur);
    }

    /**
     * Récupérer les dépenses d'une période
     */
    @Transactional(readOnly = true)
    public List<DepenseEntity> obtenirDepensesParPeriode(LocalDate debut, LocalDate fin) {
        return depenseRepository.findByDateDepenseBetween(debut.atStartOfDay(), fin.atTime(LocalTime.MAX));
    }

    /**
     * Calculer le total des dépenses d'une période
     */
    @Transactional(readOnly = true)
    public BigDecimal calculerTotalDepenses(LocalDate debut, LocalDate fin) {
        List<DepenseEntity> depenses = obtenirDepensesParPeriode(debut, fin);

        return depenses.stream()
                .map(DepenseEntity::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Récupérer les dépenses par catégorie
     */
    @Transactional(readOnly = true)
    public List<DepenseEntity> obtenirDepensesParCategorie(DepenseEntity.CategorieDepense categorie) {
        return depenseRepository.findByCategorie(categorie);
    }

    /**
     * VALIDATION
     */
    private void validerDepense(DepenseEntity depense) {
        if (depense.getLibelle() == null || depense.getLibelle().trim().isEmpty()) {
            throw new IllegalArgumentException("Le libellé est obligatoire");
        }

        if (depense.getMontant() == null ||
                depense.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à 0");
        }

        if (depense.getCategorie() == null) {
            throw new IllegalArgumentException("La catégorie est obligatoire");
        }
    }
}
