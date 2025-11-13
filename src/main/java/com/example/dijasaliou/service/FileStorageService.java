package com.example.dijasaliou.service;

import com.example.dijasaliou.entity.TenantEntity;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des fichiers uploadés (photos de produits)
 *
 * Fonctionnalités :
 * - Upload de photos avec validation
 * - Redimensionnement automatique des images
 * - Stockage organisé par tenant
 * - Suppression sécurisée
 * - Génération d'URLs relatives
 *
 * Structure de stockage :
 * uploads/photos/{tenant_uuid}/achats/2024-01-15_sac-riz_abc123.jpg
 * uploads/photos/{tenant_uuid}/ventes/2024-01-16_huile-5l_def456.jpg
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload.dir:uploads/photos}")
    private String uploadDir;

    @Value("${file.max-size:5242880}") // 5 MB par défaut
    private long maxFileSize;

    @Value("${image.max-width:800}")
    private int maxWidth;

    @Value("${image.max-height:800}")
    private int maxHeight;

    // Extensions autorisées pour les photos
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");

    // Types MIME autorisés
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );

    /**
     * Initialise le répertoire de stockage au démarrage
     */
    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Répertoire de stockage créé : {}", uploadPath.toAbsolutePath());
            } else {
                log.info("Répertoire de stockage existant : {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Impossible de créer le répertoire de stockage : {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'initialisation du stockage de fichiers", e);
        }
    }

    /**
     * Upload une photo de produit
     *
     * @param file    Le fichier uploadé
     * @param tenant  Le tenant propriétaire
     * @param type    Type de photo (achats, ventes, produits)
     * @return URL relative de la photo : /api/files/photos/{tenant_uuid}/achats/photo.jpg
     */
    public String uploadPhoto(MultipartFile file, TenantEntity tenant, String type) {
        // 1. Validations
        validateFile(file);

        // 2. Créer le répertoire du tenant si nécessaire
        String tenantUuid = tenant.getTenantUuid();
        Path tenantPath = Paths.get(uploadDir, tenantUuid, type);
        try {
            Files.createDirectories(tenantPath);
        } catch (IOException e) {
            log.error("Erreur lors de la création du répertoire tenant : {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la création du répertoire", e);
        }

        // 3. Générer un nom de fichier unique
        String fileName = generateFileName(file.getOriginalFilename());

        // 4. Chemin complet du fichier
        Path filePath = tenantPath.resolve(fileName);

        try {
            // 5. Optimiser l'image (redimensionnement si trop grande)
            BufferedImage optimizedImage = optimizeImage(file);

            // 6. Sauvegarder l'image optimisée
            String extension = getFileExtension(file.getOriginalFilename());
            ImageIO.write(optimizedImage, extension.equals("png") ? "PNG" : "JPEG", filePath.toFile());

            log.info("Photo uploadée avec succès : {} (Tenant: {}, Type: {})",
                    fileName, tenantUuid, type);

            // 7. Retourner l'URL relative
            return String.format("/api/files/photos/%s/%s/%s", tenantUuid, type, fileName);

        } catch (IOException e) {
            log.error("Erreur lors de l'upload de la photo : {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'upload de la photo", e);
        }
    }

    /**
     * Supprime une photo
     *
     * @param photoUrl URL de la photo à supprimer
     * @param tenant   Le tenant propriétaire (pour vérification de sécurité)
     */
    public void deletePhoto(String photoUrl, TenantEntity tenant) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return;
        }

        try {
            // Extraire le chemin relatif depuis l'URL
            // Format : /api/files/photos/{tenant_uuid}/achats/photo.jpg
            String[] parts = photoUrl.split("/photos/");
            if (parts.length != 2) {
                log.warn("Format d'URL invalide : {}", photoUrl);
                return;
            }

            // Vérifier que le tenant correspond (sécurité)
            if (!photoUrl.contains(tenant.getTenantUuid())) {
                log.error("Tentative de suppression d'une photo d'un autre tenant ! Tenant: {}, URL: {}",
                        tenant.getTenantUuid(), photoUrl);
                throw new SecurityException("Vous ne pouvez pas supprimer la photo d'un autre tenant");
            }

            // Construire le chemin complet
            Path filePath = Paths.get(uploadDir, parts[1]);

            // Supprimer le fichier
            Files.deleteIfExists(filePath);

            log.info("Photo supprimée : {} (Tenant: {})", photoUrl, tenant.getTenantUuid());

        } catch (IOException e) {
            log.error("Erreur lors de la suppression de la photo : {}", e.getMessage());
            // Ne pas lever d'exception, la suppression de fichier n'est pas critique
        }
    }

    /**
     * Récupère une photo (pour la servir via HTTP)
     *
     * @param tenantUuid UUID du tenant
     * @param type       Type de photo (achats, ventes, produits)
     * @param fileName   Nom du fichier
     * @return Le fichier sous forme de byte[]
     */
    public byte[] getPhoto(String tenantUuid, String type, String fileName) throws IOException {
        Path filePath = Paths.get(uploadDir, tenantUuid, type, fileName);

        if (!Files.exists(filePath)) {
            throw new IOException("Photo introuvable : " + fileName);
        }

        return Files.readAllBytes(filePath);
    }

    /**
     * Valide le fichier uploadé
     */
    private void validateFile(MultipartFile file) {
        // 1. Vérifier que le fichier n'est pas vide
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        // 2. Vérifier la taille
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("Le fichier est trop volumineux (max %d MB)", maxFileSize / 1024 / 1024)
            );
        }

        // 3. Vérifier l'extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Le nom du fichier est invalide");
        }
        String fileName = StringUtils.cleanPath(originalFilename);
        String extension = getFileExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Extension de fichier non autorisée. Extensions acceptées : " + ALLOWED_EXTENSIONS
            );
        }

        // 4. Vérifier le type MIME (sécurité renforcée)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Type de fichier non autorisé. Types acceptés : images (JPG, PNG, WEBP)"
            );
        }
    }

    /**
     * Génère un nom de fichier unique
     *
     * Format : 2024-01-15_produit-nom_abc123.jpg
     */
    private String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Nettoyer le nom original (garder seulement les caractères alphanumériques)
        String baseName = originalFilename
                .replaceAll("\\.[^.]*$", "") // Enlever l'extension
                .replaceAll("[^a-zA-Z0-9-]", "-") // Remplacer caractères spéciaux par -
                .toLowerCase()
                .substring(0, Math.min(20, originalFilename.length())); // Max 20 caractères

        return String.format("%s_%s_%s.%s", timestamp, baseName, uniqueId, extension);
    }

    /**
     * Extrait l'extension d'un nom de fichier
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase();
    }

    /**
     * Optimise une image (redimensionnement si trop grande)
     *
     * @param file Le fichier image
     * @return L'image optimisée
     */
    private BufferedImage optimizeImage(MultipartFile file) throws IOException {
        // 1. Lire l'image originale
        BufferedImage originalImage;
        try (InputStream inputStream = file.getInputStream()) {
            originalImage = ImageIO.read(inputStream);
        }

        if (originalImage == null) {
            throw new IOException("Impossible de lire l'image. Fichier corrompu ?");
        }

        // 2. Calculer les nouvelles dimensions si nécessaire
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Si l'image est déjà petite, pas besoin de redimensionner
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return originalImage;
        }

        // 3. Calculer le ratio pour garder les proportions
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        // 4. Redimensionner l'image avec une bonne qualité
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();

        // Configuration pour une bonne qualité de redimensionnement
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        graphics.dispose();

        log.debug("Image redimensionnée : {}x{} → {}x{}", originalWidth, originalHeight, newWidth, newHeight);

        return resizedImage;
    }
}
