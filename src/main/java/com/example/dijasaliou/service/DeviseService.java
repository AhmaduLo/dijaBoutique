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
     */
    @Transactional
    public DeviseEntity creerDevise(CreateDeviseDto dto) {
        TenantEntity tenant = tenantService.getCurrentTenant();
        String codeNormalise = dto.getCode().toUpperCase();

        // Le code ne doit collisionner ni avec une devise système, ni avec une
        // devise déjà créée par cette même boutique (deux boutiques différentes
        // peuvent en revanche avoir chacune leur propre devise du même code).
        if (deviseRepository.existsByCodeAndTenantIsNull(codeNormalise)) {
            throw new RuntimeException("Le code " + codeNormalise + " est déjà utilisé par une devise système");
        }
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
     * Met à jour une devise — réservé aux devises personnalisées de la
     * boutique courante. Les devises système (XOF/EUR/USD…) ne sont pas
     * modifiables via cette méthode.
     */
    @Transactional
    public DeviseEntity modifierDevise(Long id, UpdateDeviseDto dto) {
        DeviseEntity devise = deviseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devise non trouvée avec l'ID : " + id));
        verifierAppartientAuTenantCourant(devise);

        if (dto.getCode() != null) {
            String nouveauCode = dto.getCode().toUpperCase();
            if (!devise.getCode().equals(nouveauCode)) {
                TenantEntity tenant = tenantService.getCurrentTenant();
                if (deviseRepository.existsByCodeAndTenantIsNull(nouveauCode)
                        || deviseRepository.existsByCodeAndTenant(nouveauCode, tenant)) {
                    throw new RuntimeException("Une devise avec le code " + nouveauCode + " existe déjà");
                }
            }
            devise.setCode(nouveauCode);
        }

        if (dto.getNom() != null) {
            devise.setNom(dto.getNom());
        }

        if (dto.getSymbole() != null) {
            devise.setSymbole(dto.getSymbole());
        }

        if (dto.getPays() != null) {
            devise.setPays(dto.getPays());
        }

        if (dto.getTauxChange() != null) {
            devise.setTauxChange(dto.getTauxChange());
        }

        return deviseRepository.save(devise);
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
     * Bloque toute modification/suppression d'une devise système, ou d'une
     * devise personnalisée appartenant à une AUTRE boutique.
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
