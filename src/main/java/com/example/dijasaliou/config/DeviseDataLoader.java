package com.example.dijasaliou.config;

import com.example.dijasaliou.entity.DeviseEntity;
import com.example.dijasaliou.repository.DeviseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chargement des données initiales pour les devises
 *
 * Initialise le Franc CFA comme devise par défaut si aucune devise n'existe
 */
@Configuration
@Slf4j
public class DeviseDataLoader {

    @Bean
    public CommandLineRunner loadDeviseData(DeviseRepository deviseRepository) {
        return args -> {
            // Vérifier s'il existe déjà des devises
            if (deviseRepository.count() == 0) {
                log.info("🔄 Initialisation des devises par défaut...");

                // Créer le Franc CFA comme devise par défaut
                DeviseEntity francCFA = DeviseEntity.builder()
                        .code("XOF")
                        .nom("Franc CFA")
                        .symbole("CFA")
                        .pays("Sénégal, Côte d'Ivoire, Mali, Burkina Faso, etc.")
                        .tauxChange(1.0)  // Devise de référence
                        .isDefault(true)
                        .build();

                deviseRepository.save(francCFA);
                log.info("✅ Franc CFA (XOF) créé comme devise par défaut");

                // Optionnel : Ajouter d'autres devises courantes
                DeviseEntity euro = DeviseEntity.builder()
                        .code("EUR")
                        .nom("Euro")
                        .symbole("€")
                        .pays("Zone Euro")
                        .tauxChange(655.957)  // 1 EUR = 655.957 XOF (taux indicatif)
                        .isDefault(false)
                        .build();

                DeviseEntity dollar = DeviseEntity.builder()
                        .code("USD")
                        .nom("Dollar américain")
                        .symbole("$")
                        .pays("États-Unis")
                        .tauxChange(600.0)  // 1 USD = 600 XOF (taux indicatif)
                        .isDefault(false)
                        .build();

                deviseRepository.save(euro);
                deviseRepository.save(dollar);

                log.info("✅ Euro (EUR) et Dollar (USD) ajoutés");
                log.info("✅ {} devises chargées avec succès", deviseRepository.count());
            } else {
                log.info("ℹ️ Les devises existent déjà ({} devises), pas de chargement initial",
                        deviseRepository.count());
            }
        };
    }
}
