package com.example.dijasaliou.controller;

import com.example.dijasaliou.annotation.RequiresPlan;
import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.service.FileStorageService;
import com.example.dijasaliou.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur pour gérer l'upload des photos de produits via Cloudinary
 *
 * Endpoints :
 * - POST /api/files/upload : Upload une photo → retourne une URL Cloudinary CDN
 * - DELETE /api/files/photos : Supprime une photo de Cloudinary
 *
 * Les images sont servies directement via le CDN Cloudinary (plus d'endpoint GET local)
 */
@RestController
@RequestMapping("/files")
@Slf4j
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final TenantService tenantService;

    public FileUploadController(FileStorageService fileStorageService, TenantService tenantService) {
        this.fileStorageService = fileStorageService;
        this.tenantService = tenantService;
    }

    /**
     * POST /api/files/upload?type=achats
     * Upload une photo et retourne l'URL Cloudinary CDN
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequiresPlan(plans = {TenantEntity.Plan.ENTREPRISE}, message = "Les photos sont réservées au plan ENTERPRISE")
    public ResponseEntity<Map<String, Object>> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "achats") String type) {

        try {
            if (!type.matches("^(achats|ventes|produits)$")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Type invalide. Types acceptés : achats, ventes, produits"
                ));
            }

            TenantEntity tenant = tenantService.getCurrentTenant();
            String photoUrl = fileStorageService.uploadPhoto(file, tenant, type);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("photoUrl", photoUrl);
            response.put("message", "Photo uploadée avec succès");

            log.info("Photo uploadée : {} (Tenant: {}, Type: {})", photoUrl, tenant.getTenantUuid(), type);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Erreur de validation lors de l'upload : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("Erreur lors de l'upload de la photo : {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de l'upload de la photo : " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/files/photos?url=https://res.cloudinary.com/...
     * Supprime une photo de Cloudinary
     */
    @DeleteMapping("/photos")
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequiresPlan(plans = {TenantEntity.Plan.ENTREPRISE}, message = "Les photos sont réservées au plan ENTERPRISE")
    public ResponseEntity<Map<String, Object>> deletePhoto(@RequestParam("url") String photoUrl) {
        try {
            TenantEntity tenant = tenantService.getCurrentTenant();
            fileStorageService.deletePhoto(photoUrl, tenant);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Photo supprimée avec succès"
            ));

        } catch (SecurityException e) {
            log.error("Erreur de sécurité lors de la suppression : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la photo : {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la suppression de la photo"
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "File Upload Service (Cloudinary)"
        ));
    }
}
