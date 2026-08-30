package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.CreateDeviseDto;
import com.example.dijasaliou.dto.UpdateDeviseDto;
import com.example.dijasaliou.entity.DeviseEntity;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.repository.DeviseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service pour la gestion des devises.
 *
 * Une devise est soit "système" (tenant = null, catalogue partagé XOF/EUR/USD…),
 * soit "personnalisée" (créée par une boutique précise, visible et modifiable
 * uniquement par elle). Toute méthode qui liste/résout/modifie une devise est
 * scopée sur le tenant courant — aucune méthode ne doit exposer ou permettre de
 * modifier la devise personnalisée d'une AUTRE boutique.
 */
@Service
public class DeviseService {

    private final DeviseRepository deviseRepository;
    private final TenantService tenantService;

    public DeviseService(DeviseRepository deviseRepository, TenantService tenantService) {
        this.deviseRepository = deviseRepository;
        this.tenantService = tenantService;
    }

    /**
     * Récupère toutes les devises visibles par la boutique courante :
     * les devises système + ses propres devises personnalisées.
     */
    public List<DeviseEntity> obtenirToutesLesDevises() {
        TenantEntity tenant = tenantService.getCurrentTenant();
        return deviseRepository.findVisiblesPourTenant(tenant);
    }

    /**
     * Récupère une devise par son ID — vérifie qu'elle est visible par la
     * boutique courante (système ou lui appartenant).
     */
    public DeviseEntity obtenirDeviseParId(Long id) {
        DeviseEntity devise = deviseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devise non trouvée avec l'ID : " + id));
        verifierVisiblePourTenantCourant(devise);
        return devise;
    }

    /**
     * Récupère une devise par son code, visible pour la boutique courante :
     * sa propre version personnalisée si elle existe, sinon la version système.
     */
    public DeviseEntity obtenirDeviseParCode(String code) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        List<DeviseEntity> resultats = deviseRepository
                .findByCodeVisiblePourTenant(code.toUpperCase(), tenant);
        if (resultats.isEmpty()) {
            throw new RuntimeException("Devise non trouvée avec le code : " + code);
        }
        // La requête trie déjà personnalisée avant système — le premier résultat
        // est la version prioritaire pour ce tenant.
        return resultats.get(0);
    }

    /**
     * Récupère la devise par défaut (mécanisme legacy, non utilisé par le
     * flux réel — voir tenants.devise_preferee).
     */
    public DeviseEntity obtenirDeviseParDefaut() {
        return deviseRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new RuntimeException("Aucune devise par défaut n'est définie"));
    }

    /**
     * Crée une nouvelle devise, personnalisée pour la boutique courante.
     *
     * Un code déjà utilisé par une devise système (EUR, USD...) est autorisé :
     * cela crée une copie personnalisée qui prend le dessus pour cette
     * boutique uniquement (voir {@link DeviseRepository#findVisiblesPourTenant}),
     * sans toucher à la devise système partagée par les autres. Seul un doublon
     * avec sa PROPRE devise du même code est refusé.
     */
    @Transactional
    public DeviseEntity creerDevise(CreateDeviseDto dto) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        String codeNormalise = dto.getCode().toUpperCase();

        if (deviseRepository.existsByCodeAndTenant(codeNormalise, tenant)) {
            throw new RuntimeException("Vous avez déjà une devise avec le code " + codeNormalise);
        }

        DeviseEntity devise = DeviseEntity.builder()
                .code(codeNormalise)
                .nom(dto.getNom())
                .symbole(dto.getSymbole())
                .pays(dto.getPays())
                .tauxChange(dto.getTauxChange())
                .isDefault(false)
                .tenant(tenant)
                .build();

        return deviseRepository.save(devise);
    }

    /**
     * Met à jour une devise.
     *
     * Modifier sa propre devise personnalisée l'édite directement. Modifier
     * une devise système (EUR/USD...) ne touche JAMAIS la ligne partagée :
     * ça crée (ou réutilise si elle existe déjà) une copie personnalisée pour
     * la boutique courante, qui prend le dessus uniquement pour elle — les
     * autres boutiques continuent de voir la valeur système inchangée.
     */
    @Transactional
    public DeviseEntity modifierDevise(Long id, UpdateDeviseDto dto) {
        DeviseEntity devise = deviseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devise non trouvée avec l'ID : " + id));
        verifierVisiblePourTenantCourant(devise);
        DeviseEntity cible = resoudreCiblePourModification(devise);

        if (dto.getCode() != null) {
            String nouveauCode = dto.getCode().toUpperCase();
            if (!cible.getCode().equals(nouveauCode)) {
                TenantEntity tenant = tenantService.getCurrentTenant();
                if (deviseRepository.existsByCodeAndTenantIsNull(nouveauCode)
                        || deviseRepository.existsByCodeAndTenant(nouveauCode, tenant)) {
                    throw new RuntimeException("Une devise avec le code " + nouveauCode + " existe déjà");
                }
            }
            cible.setCode(nouveauCode);
        }

        if (dto.getNom() != null) {
            cible.setNom(dto.getNom());
        }

        if (dto.getSymbole() != null) {
            cible.setSymbole(dto.getSymbole());
        }

        if (dto.getPays() != null) {
            cible.setPays(dto.getPays());
        }

        if (dto.getTauxChange() != null) {
            cible.setTauxChange(dto.getTauxChange());
        }

        return deviseRepository.save(cible);
    }

    /**
     * Détermine l'entité à réellement modifier : la devise elle-même si elle
     * appartient déjà à la boutique courante, sinon (devise système) sa copie
     * personnalisée — créée à la volée au premier "Modifier", réutilisée
     * ensuite.
     */
    private DeviseEntity resoudreCiblePourModification(DeviseEntity devise) {
        if (devise.getTenant() != null) {
            return devise;
        }
        TenantEntity tenant = tenantService.getCurrentTenant();
        return deviseRepository.findByCodeAndTenant(devise.getCode(), tenant)
                .orElseGet(() -> {
                    DeviseEntity copie = DeviseEntity.builder()
                            .code(devise.getCode())
                            .nom(devise.getNom())
                            .symbole(devise.getSymbole())
                            .pays(devise.getPays())
                            .tauxChange(devise.getTauxChange())
                            .isDefault(false)
                            .tenant(tenant)
                            .build();
                    return deviseRepository.save(copie);
                });
    }

    /**
     * Supprime une devise — réservé aux devises personnalisées de la
     * boutique courante.
     */
    @Transactional
    public void supprimerDevise(Long id) {
        DeviseEntity devise = deviseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devise non trouvée avec l'ID : " + id));
        verifierAppartientAuTenantCourant(devise);

        // Les entités VenteEntity et AchatEntity ne stockent pas de FK vers la devise —
        // elles utilisent DeviseService.convertir() à la volée. La suppression est sûre.
        deviseRepository.deleteById(id);
    }

    /**
     * Définit une devise comme devise par défaut (mécanisme legacy, non
     * utilisé par le flux réel — voir DeviseController#definirDeviseParDefaut
     * qui écrit directement dans tenants.devise_preferee).
     */
    @Transactional
    public DeviseEntity definirDeviseParDefaut(Long id) {
        DeviseEntity devise = deviseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devise non trouvée avec l'ID : " + id));

        if (devise.getIsDefault()) {
            return devise;
        }

        retirerDeviseParDefaut();
        devise.setIsDefault(true);
        return deviseRepository.save(devise);
    }

    /**
     * Retire le statut de devise par défaut de toutes les devises (mécanisme legacy).
     */
    private void retirerDeviseParDefaut() {
        deviseRepository.findByIsDefaultTrue().ifPresent(deviseParDefaut -> {
            deviseParDefaut.setIsDefault(false);
            deviseRepository.save(deviseParDefaut);
        });
    }

    /**
     * Convertit un montant d'une devise à une autre (visibles pour la
     * boutique courante).
     */
    public Double convertir(Double montant, String codeDeviseSource, String codeDeviseCible) {
        if (montant == null || montant == 0) {
            return 0.0;
        }

        DeviseEntity deviseSource = obtenirDeviseParCode(codeDeviseSource);
        DeviseEntity deviseCible = obtenirDeviseParCode(codeDeviseCible);

        Double montantReference = deviseSource.convertirVersReference(montant);
        return deviseCible.convertirDepuisReference(montantReference);
    }

    /**
     * Une devise système est visible par tout le monde ; une devise
     * personnalisée uniquement par sa boutique.
     */
    private void verifierVisiblePourTenantCourant(DeviseEntity devise) {
        if (devise.getTenant() == null) {
            return;
        }
        TenantEntity tenant = tenantService.getCurrentTenant();
        if (!devise.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Devise non trouvée avec l'ID : " + devise.getId());
        }
    }

    /**
     * Bloque la suppression d'une devise système (la ligne partagée ne
     * disparaît jamais — un tenant qui veut "revenir" à la valeur système
     * supprime sa propre copie personnalisée, pas la ligne système elle-même),
     * ou d'une devise personnalisée appartenant à une AUTRE boutique.
     */
    private void verifierAppartientAuTenantCourant(DeviseEntity devise) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        if (devise.getTenant() == null) {
            throw new RuntimeException("Les devises système ne sont pas modifiables");
        }
        if (!devise.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Devise non trouvée avec l'ID : " + devise.getId());
        }
    }
}
