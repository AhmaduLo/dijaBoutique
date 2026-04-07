package com.example.dijasaliou.controller;

import com.example.dijasaliou.service.PlatformConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint public (authentifié) pour les configs visibles par les boutiques.
 * Ex: numéro WhatsApp support HeasyStock.
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class PublicConfigController {

    private final PlatformConfigService platformConfigService;

    /** GET /config/public → toutes les configs publiques sous forme map */
    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> getConfigsPubliques() {
        return ResponseEntity.ok(platformConfigService.obtenirConfigsPubliques());
    }

    /** GET /config/{cle} → valeur d'une config publique spécifique
     *  Ex: GET /config/whatsapp_support → { "valeur": "+33751130937" }
     */
    @GetMapping("/{cle}")
    public ResponseEntity<Map<String, String>> getConfig(@PathVariable String cle) {
        String valeur = platformConfigService.obtenirConfigPublique(cle);
        return ResponseEntity.ok(Map.of("valeur", valeur));
    }
}
