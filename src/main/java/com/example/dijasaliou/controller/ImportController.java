package com.example.dijasaliou.controller;

import com.example.dijasaliou.annotation.RequiresPlan;
import com.example.dijasaliou.dto.ImportPreviewDto;
import com.example.dijasaliou.dto.ImportResultatDto;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.entity.UserEntity;
import com.example.dijasaliou.service.ImportService;
import com.example.dijasaliou.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/import")
@PreAuthorize("hasAnyAuthority('ADMIN', 'GERANT')")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final UserService   userService;

    /**
     * POST /api/import/preview
     *
     * Prévisualise un fichier Excel sans insérer en base.
     * Retourne les 5 premières lignes valides + la liste des erreurs.
     *
     * @param fichier     Fichier .xlsx ou .xls (max 5 MB)
     * @param type        "achats", "ventes" ou "depenses"
     * @param margeDefaut Marge par défaut pour le prix de vente (achats uniquement), défaut 20%
     */
    @RequiresPlan(plans = {TenantEntity.Plan.BUSINESS}, message = "L'import de fichiers est réservé au plan Business")
    @PostMapping("/preview")
    public ResponseEntity<ImportPreviewDto> previsualiser(
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam("type") String type,
            @RequestParam(value = "margeDefaut", defaultValue = "20") Double margeDefaut,
            Authentication auth) {

        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(auth.getName());
        return ResponseEntity.ok(importService.previsualiser(fichier, type, margeDefaut, utilisateur));
    }

    /**
     * POST /api/import/confirmer
     *
     * Importe les données du fichier Excel en base par lots de 50.
     * Les lignes invalides ou doublons sont ignorées et signalées dans le rapport.
     *
     * @param fichier     Fichier .xlsx ou .xls (max 5 MB, max 1000 lignes)
     * @param type        "achats", "ventes" ou "depenses"
     * @param margeDefaut Marge par défaut pour le prix de vente (achats uniquement), défaut 20%
     */
    @RequiresPlan(plans = {TenantEntity.Plan.BUSINESS}, message = "L'import de fichiers est réservé au plan Business")
    @PostMapping("/confirmer")
    public ResponseEntity<ImportResultatDto> confirmer(
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam("type") String type,
            @RequestParam(value = "margeDefaut", defaultValue = "20") Double margeDefaut,
            Authentication auth) {

        UserEntity utilisateur = userService.obtenirUtilisateurParEmail(auth.getName());
        return ResponseEntity.ok(importService.confirmer(fichier, type, margeDefaut, utilisateur));
    }
}
