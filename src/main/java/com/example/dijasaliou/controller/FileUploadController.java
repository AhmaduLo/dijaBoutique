package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.service.FileStorageService;
import com.example.dijasaliou.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur pour gérer l'upload et la récupération des photos de produits
 *
 * Endpoints :
 * - POST /api/files/upload : Upload une photo
 * - GET /api/files/photos/{tenantUuid}/{type}/{filename} : Récupère une photo
 * - DELETE /api/files/photos/{photoUrl} : Supprime une photo (non implémenté encore)
 *
 * SÉCURITÉ :
 * - Seuls les utilisateurs authentifiés peuvent uploader
 * - Seuls les ADMIN peuvent uploader des photos
 * - Chaque tenant ne peut accéder qu'à ses propres photos
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
     * Upload une photo de produit
     *
     * Requête :
     * POST /api/files/upload?type=achats
     * Content-Type: multipart/form-data
     * Body: file (le fichier image)
     *
     * Réponse :
     * {
     *   "success": true,
     *   "photoUrl": "/api/files/photos/{tenant_uuid}/achats/2024-01-15_produit_abc123.jpg",
     *   "message": "Photo uploadée avec succès"
     * }
     *
     * @param file Le fichier image uploadé
     * @param type Type de photo : "achats", "ventes", "produits"
     * @return L'URL de la photo uploadée
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "achats") String type) {

        try {
            // Valider le type
            if (!type.matches("^(achats|ventes|produits)$")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Type invalide. Types acceptés : achats, ventes, produits"
                ));
            }

            // Récupérer le tenant actuel
            TenantEntity tenant = tenantService.getCurrentTenant();

            // Upload la photo
            String photoUrl = fileStorageService.uploadPhoto(file, tenant, type);

            // Construire la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("photoUrl", photoUrl);
            response.put("message", "Photo uploadée avec succès");

            log.info("Photo uploadée : {} (Tenant: {}, Type: {})",
                    photoUrl, tenant.getTenantUuid(), type);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Erreur de validation lors de l'upload : {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("Erreur lors de l'upload de la photo : {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors de l'upload de la photo : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Récupère une photo
     *
     * Requête :
     * GET /api/files/photos/{tenantUuid}/achats/2024-01-15_produit_abc123.jpg
     *
     * Réponse :
     * Le fichier image (Content-Type: image/jpeg ou image/png)
     *
     * SÉCURITÉ : Chaque tenant ne peut accéder qu'à ses propres photos
     *
     * @param tenantUuid UUID du tenant
     * @param type Type de photo (achats, ventes, produits)
     * @param filename Nom du fichier
     * @return Le fichier image
     */
    @GetMapping("/photos/{tenantUuid}/{type}/{filename:.+}")
    public ResponseEntity<byte[]> getPhoto(
            @PathVariable String tenantUuid,
            @PathVariable String type,
            @PathVariable String filename) {

        try {
            // SÉCURITÉ : Vérifier que l'utilisateur a le droit d'accéder à cette photo
            TenantEntity currentTenant = tenantService.getCurrentTenant();
            if (!currentTenant.getTenantUuid().equals(tenantUuid)) {
                log.warn("Tentative d'accès à une photo d'un autre tenant ! Current: {}, Requested: {}",
                        currentTenant.getTenantUuid(), tenantUuid);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Récupérer la photo
            byte[] photoBytes = fileStorageService.getPhoto(tenantUuid, type, filename);

            // Déterminer le Content-Type selon l'extension
            String contentType = MediaType.IMAGE_JPEG_VALUE;
            if (filename.toLowerCase().endsWith(".png")) {
                contentType = MediaType.IMAGE_PNG_VALUE;
            } else if (filename.toLowerCase().endsWith(".webp")) {
                contentType = "image/webp";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(photoBytes);

        } catch (IOException e) {
            log.error("Photo introuvable : {}/{}/{}", tenantUuid, type, filename);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la photo : {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Supprime une photo (optionnel, pour une gestion complète)
     *
     * DELETE /api/files/photos?url=/api/files/photos/{tenant_uuid}/achats/photo.jpg
     *
     * @param photoUrl URL de la photo à supprimer
     * @return Confirmation de suppression
     */
    @DeleteMapping("/photos")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> deletePhoto(@RequestParam("url") String photoUrl) {
        try {
            TenantEntity tenant = tenantService.getCurrentTenant();
            fileStorageService.deletePhoto(photoUrl, tenant);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Photo supprimée avec succès");

            return ResponseEntity.ok(response);

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

    /**
     * Endpoint de test pour vérifier que le service fonctionne
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "File Upload Service"
        ));
    }
}
