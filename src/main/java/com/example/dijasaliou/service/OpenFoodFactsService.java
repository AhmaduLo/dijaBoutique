package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.CodeBarreLookupDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service pour interroger l'API Open Food Facts.
 *
 * API gratuite, open source, sans clé — retourne nom, photo, catégorie, marque
 * pour les produits alimentaires et non-alimentaires référencés mondialement.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenFoodFactsService {

    private final RestTemplate restTemplate;

    private static final String API_URL = "https://world.openfoodfacts.org/api/v2/product/%s?fields=product_name,image_url,categories,brands";

    /**
     * Recherche un produit par code-barre sur Open Food Facts.
     *
     * @param codeBarre code EAN-13, EAN-8, UPC-A, etc.
     * @return CodeBarreLookupDto avec trouve=true si trouvé, sinon trouve=false
     */
    public CodeBarreLookupDto rechercherParCodeBarre(String codeBarre) {
        try {
            String url = String.format(API_URL, codeBarre);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                return nonTrouve(codeBarre);
            }

            // L'API retourne status = 1 si le produit existe
            Object status = response.get("status");
            if (status == null || !(status instanceof Number) || ((Number) status).intValue() != 1) {
                return nonTrouve(codeBarre);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) response.get("product");
            if (product == null) {
                return nonTrouve(codeBarre);
            }

            String nom = getString(product, "product_name");
            String imageUrl = getString(product, "image_url");
            String categories = getString(product, "categories");
            String marque = getString(product, "brands");

            // Si aucun nom → considérer comme non trouvé
            if (nom == null || nom.isBlank()) {
                return nonTrouve(codeBarre);
            }

            return CodeBarreLookupDto.builder()
                    .trouve(true)
                    .codeBarre(codeBarre)
                    .source("OPEN_FOOD_FACTS")
                    .nomProduit(nom.trim())
                    .photoUrl(imageUrl)
                    .categorie(categories)
                    .marque(marque)
                    .build();

        } catch (Exception e) {
            log.warn("Erreur lors de l'appel Open Food Facts pour le code-barre {} : {}", codeBarre, e.getMessage());
            return nonTrouve(codeBarre);
        }
    }

    private CodeBarreLookupDto nonTrouve(String codeBarre) {
        return CodeBarreLookupDto.builder()
                .trouve(false)
                .codeBarre(codeBarre)
                .build();
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
