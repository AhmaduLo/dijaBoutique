package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.CreateDeviseDto;
import com.example.dijasaliou.dto.UpdateDeviseDto;
import com.example.dijasaliou.entity.DeviseEntity;
import com.example.dijasaliou.repository.DeviseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service pour la gestion des devises
 */
@Service
public class DeviseService {

    private final DeviseRepository deviseRepository;

    public DeviseService(DeviseRepository deviseRepository) {
        this.deviseRepository = deviseRepository;
    }

    /**
     * Récupère toutes les devises
     */
    public List<DeviseEntity> obtenirToutesLesDevises() {
        return deviseRepository.findAll();
    }

    /**
     * Récupère une devise par son ID
     */
    public DeviseEntity obtenirDeviseParId(Long id) {
        return deviseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devise non trouvée avec l'ID : " + id));
    }

    /**
     * Récupère une devise par son code
     */
    public DeviseEntity obtenirDeviseParCode(String code) {
        return deviseRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Devise non trouvée avec le code : " + code));
    }

    /**
     * Récupère la devise par défaut
     */
    public DeviseEntity obtenirDeviseParDefaut() {
        return deviseRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new RuntimeException("Aucune devise par défaut n'est définie"));
    }

    /**
     * Crée une nouvelle devise
     */
    @Transactional
    public DeviseEntity creerDevise(CreateDeviseDto dto) {
        // Vérifier si le code existe déjà
        String codeNormalise = dto.getCode().toUpperCase();
        if (deviseRepository.existsByCode(codeNormalise)) {
            throw new RuntimeException("Une devise avec le code " + codeNormalise + " existe déjà");
        }

        // Si c'est la première devise ou si isDefault est true, gérer la devise par défaut
        boolean isDefault = dto.getIsDefault() != null && dto.getIsDefault();
        if (isDefault) {
            // Retirer le statut par défaut des autres devises
            retirerDeviseParDefaut();
        } else if (!deviseRepository.existsByIsDefaultTrue()) {
            // Si aucune devise par défaut n'existe, cette devise devient par défaut
            isDefault = true;
        }

        // Créer la devise
        DeviseEntity devise = DeviseEntity.builder()
                .code(codeNormalise)
                .nom(dto.getNom())
                .symbole(dto.getSymbole())
                .pays(dto.getPays())
                .tauxChange(dto.getTauxChange())
                .isDefault(isDefault)
                .build();

        return deviseRepository.save(devise);
    }

    /**
     * Met à jour une devise
     */
    @Transactional
    public DeviseEntity modifierDevise(Long id, UpdateDeviseDto dto) {
        DeviseEntity devise = obtenirDeviseParId(id);

        // Mettre à jour les champs si présents
        if (dto.getCode() != null) {
            String nouveauCode = dto.getCode().toUpperCase();
            // Vérifier que le nouveau code n'est pas déjà utilisé par une autre devise
            if (!devise.getCode().equals(nouveauCode) && deviseRepository.existsByCode(nouveauCode)) {
                throw new RuntimeException("Une devise avec le code " + nouveauCode + " existe déjà");
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

        if (dto.getIsDefault() != null && dto.getIsDefault()) {
            // Si on veut définir cette devise comme par défaut, retirer le statut des autres
            retirerDeviseParDefaut();
            devise.setIsDefault(true);
        }

        return deviseRepository.save(devise);
    }

    /**
     * Supprime une devise
     */
    @Transactional
    public void supprimerDevise(Long id) {
        DeviseEntity devise = obtenirDeviseParId(id);

        // Vérifier que ce n'est pas la devise par défaut
        if (devise.getIsDefault()) {
            throw new RuntimeException(
                    "Impossible de supprimer la devise par défaut. " +
                    "Définissez d'abord une autre devise comme devise par défaut."
            );
        }

        // TODO: Vérifier si la devise est utilisée dans des transactions
        // Si oui, lancer une exception

        deviseRepository.deleteById(id);
    }

    /**
     * Définit une devise comme devise par défaut
     */
    @Transactional
    public DeviseEntity definirDeviseParDefaut(Long id) {
        DeviseEntity devise = obtenirDeviseParId(id);

        // Si déjà par défaut, ne rien faire
        if (devise.getIsDefault()) {
            return devise;
        }

        // Retirer le statut par défaut des autres devises
        retirerDeviseParDefaut();

        // Définir cette devise comme par défaut
        devise.setIsDefault(true);
        return deviseRepository.save(devise);
    }

    /**
     * Retire le statut de devise par défaut de toutes les devises
     */
    private void retirerDeviseParDefaut() {
        deviseRepository.findByIsDefaultTrue().ifPresent(deviseParDefaut -> {
            deviseParDefaut.setIsDefault(false);
            deviseRepository.save(deviseParDefaut);
        });
    }

    /**
     * Convertit un montant d'une devise à une autre
     */
    public Double convertir(Double montant, String codeDeviseSource, String codeDeviseCible) {
        if (montant == null || montant == 0) {
            return 0.0;
        }

        DeviseEntity deviseSource = obtenirDeviseParCode(codeDeviseSource);
        DeviseEntity deviseCible = obtenirDeviseParCode(codeDeviseCible);

        // Conversion en 2 étapes :
        // 1. Convertir de la devise source vers la devise de référence
        Double montantReference = deviseSource.convertirVersReference(montant);

        // 2. Convertir de la devise de référence vers la devise cible
        return deviseCible.convertirDepuisReference(montantReference);
    }
}
