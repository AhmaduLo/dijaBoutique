package com.example.dijasaliou.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.dijasaliou.entity.TenantEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service de gestion des photos via Cloudinary (stockage cloud)
 *
 * Les photos sont stockées sur Cloudinary dans la structure :
 * dijasaliou/{tenant_uuid}/{type}/{public_id}
 *
 * Les URLs retournées sont des URLs Cloudinary CDN :
 * https://res.cloudinary.com/dkgvinoos/image/upload/...
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${file.max-size:5242880}")
    private long maxFileSize;

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );

    public FileStorageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        log.info("Cloudinary initialisé (cloud: {})", cloudName);
    }

    /**
     * Upload une photo sur Cloudinary
     *
     * @param file   Le fichier image
     * @param tenant Le tenant propriétaire
     * @param type   Type : achats, ventes, produits
     * @return URL Cloudinary CDN de la photo
     */
    public String uploadPhoto(MultipartFile file, TenantEntity tenant, String type) {
        validateFile(file);

        String tenantUuid = tenant.getTenantUuid();
        String folder = "dijasaliou/" + tenantUuid + "/" + type;
        String publicId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", publicId,
                    "resource_type", "image",
                    "quality", "auto:good",
                    "fetch_format", "auto",
                    "width", 800,
                    "height", 800,
                    "crop", "limit"
            );

            Map result = cloudinary.uploader().upload(file.getBytes(), params);
            String url = (String) result.get("secure_url");

            log.info("Photo uploadée sur Cloudinary : {} (Tenant: {}, Type: {})", url, tenantUuid, type);

            return url;

        } catch (IOException e) {
            log.error("Erreur lors de l'upload sur Cloudinary : {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'upload de la photo", e);
        }
    }

    /**
     * Supprime une photo de Cloudinary
     *
     * @param photoUrl URL Cloudinary de la photo
     * @param tenant   Le tenant propriétaire (vérification de sécurité)
     */
    public void deletePhoto(String photoUrl, TenantEntity tenant) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return;
        }

        if (!photoUrl.contains(tenant.getTenantUuid())) {
            throw new SecurityException("Vous ne pouvez pas supprimer la photo d'un autre tenant");
        }

        try {
            String publicId = extractPublicId(photoUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Photo supprimée de Cloudinary : {} (Tenant: {})", publicId, tenant.getTenantUuid());
            }
        } catch (IOException e) {
            log.error("Erreur lors de la suppression de la photo : {}", e.getMessage());
        }
    }

    /**
     * Extrait le public_id depuis une URL Cloudinary
     * URL format : https://res.cloudinary.com/{cloud}/image/upload/v123/{folder}/{id}.jpg
     * → dijasaliou/{tenant_uuid}/{type}/{id}
     */
    private String extractPublicId(String cloudinaryUrl) {
        try {
            String[] parts = cloudinaryUrl.split("/upload/");
            if (parts.length < 2) return null;
            String path = parts[1].replaceFirst("^v\\d+/", "");
            int lastDot = path.lastIndexOf('.');
            if (lastDot > 0) {
                path = path.substring(0, lastDot);
            }
            return path;
        } catch (Exception e) {
            log.warn("Impossible d'extraire le public_id de l'URL : {}", cloudinaryUrl);
            return null;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("Le fichier est trop volumineux (max %d MB)", maxFileSize / 1024 / 1024)
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Le nom du fichier est invalide");
        }

        String extension = getFileExtension(StringUtils.cleanPath(originalFilename));
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Extension non autorisée. Extensions acceptées : " + ALLOWED_EXTENSIONS
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Type de fichier non autorisé. Types acceptés : images (JPG, PNG, WEBP)"
            );
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return "";
        return fileName.substring(lastDot + 1).toLowerCase();
    }
}
