package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.PlatformConfigEntity;
import com.example.dijasaliou.repository.PlatformConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformConfigService {

    private static final List<String> CLES_PUBLIQUES = List.of("whatsapp_support");

    private final PlatformConfigRepository platformConfigRepository;

    @Cacheable(value = "platformConfig", key = "'all'")
    @Transactional(readOnly = true)
    public List<PlatformConfigEntity> obtenirToutes() {
        return platformConfigRepository.findAll();
    }

    @Cacheable(value = "platformConfig", key = "'publiques'")
    @Transactional(readOnly = true)
    public Map<String, String> obtenirConfigsPubliques() {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        for (String cle : CLES_PUBLIQUES) {
            platformConfigRepository.findByCle(cle)
                    .ifPresent(c -> result.put(c.getCle(), c.getValeur()));
        }
        return result;
    }

    private static final Map<String, String> VALEURS_DEFAUT = Map.of(
            "whatsapp_support", "+33751130937"
    );

    @Transactional(readOnly = true)
    public String obtenirConfigPublique(String cle) {
        if (!CLES_PUBLIQUES.contains(cle)) {
            throw new IllegalArgumentException("Configuration non accessible : " + cle);
        }
        return platformConfigRepository.findByCle(cle)
                .map(PlatformConfigEntity::getValeur)
                .orElse(VALEURS_DEFAUT.getOrDefault(cle, ""));
    }

    @CacheEvict(value = "platformConfig", allEntries = true)
    @Transactional
    public PlatformConfigEntity modifier(String cle, String nouvelleValeur) {
        PlatformConfigEntity config = platformConfigRepository.findByCle(cle)
                .orElseThrow(() -> new IllegalArgumentException("Configuration introuvable : " + cle));
        config.setValeur(nouvelleValeur);
        return platformConfigRepository.save(config);
    }
}
