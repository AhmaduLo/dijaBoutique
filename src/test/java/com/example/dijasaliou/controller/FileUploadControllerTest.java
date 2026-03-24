package com.example.dijasaliou.controller;

import com.example.dijasaliou.entity.TenantEntity;
import com.example.dijasaliou.service.FileStorageService;
import com.example.dijasaliou.service.TenantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FileUploadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@DisplayName("Tests du FileUploadController")
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean(name = "jwtAuthenticationFilter")
    private com.example.dijasaliou.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "jwtService")
    private com.example.dijasaliou.jwt.JwtService jwtService;

    @MockitoBean
    private com.example.dijasaliou.config.HibernateFilterInterceptor hibernateFilterInterceptor;

    @MockitoBean
    private com.example.dijasaliou.filter.SubscriptionExpirationFilter subscriptionExpirationFilter;

    // ==================== GET /files/health ====================

    @Test
    @DisplayName("GET /files/health → 200, body contient status=ok")
    void health_RetourneOkAvecStatus() throws Exception {
        mockMvc.perform(get("/files/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("File Upload Service (Cloudinary)"));
    }

    // ==================== POST /files/upload ====================

    @Test
    @DisplayName("POST /files/upload?type=invalide → 400, success=false")
    void uploadPhoto_TypeInvalide_RetourneBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "test".getBytes());

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .param("type", "invalide"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Type invalide")));
    }

    @Test
    @DisplayName("POST /files/upload?type=achats → 200, success=true, photoUrl présent")
    void uploadPhoto_TypeAchatsValide_RetourneOkAvecPhotoUrl() throws Exception {
        // Arrange — isTenantDefined() retourne false par défaut (aspect ignore la vérification)
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("upload-tenant-001")
                .nomEntreprise("Boutique Upload")
                .numeroTelephone("+221770000001")
                .plan(TenantEntity.Plan.PRO)
                .build();
        when(tenantService.getCurrentTenant()).thenReturn(tenant);
        when(fileStorageService.uploadPhoto(any(), eq(tenant), eq("achats")))
                .thenReturn("https://res.cloudinary.com/test/image/upload/achats/photo.jpg");

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "test".getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .param("type", "achats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.photoUrl").value("https://res.cloudinary.com/test/image/upload/achats/photo.jpg"))
                .andExpect(jsonPath("$.message").value("Photo uploadée avec succès"));
    }

    @Test
    @DisplayName("POST /files/upload?type=ventes → FileStorageService lance Exception → 500")
    void uploadPhoto_TypeVentesServiceLanceException_Retourne500() throws Exception {
        // Arrange
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("upload-tenant-002")
                .nomEntreprise("Boutique Upload 2")
                .numeroTelephone("+221770000002")
                .plan(TenantEntity.Plan.PRO)
                .build();
        when(tenantService.getCurrentTenant()).thenReturn(tenant);
        when(fileStorageService.uploadPhoto(any(), any(), eq("ventes")))
                .thenThrow(new RuntimeException("Erreur Cloudinary"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "test".getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .param("type", "ventes"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Une erreur est survenue lors de l'upload. Veuillez réessayer."));
    }

    @Test
    @DisplayName("POST /files/upload?type=produits → FileStorageService lance IllegalArgumentException → 400")
    void uploadPhoto_TypeProduitsServiceLanceIllegalArgument_Retourne400() throws Exception {
        // Arrange
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("upload-tenant-003")
                .nomEntreprise("Boutique Upload 3")
                .numeroTelephone("+221770000003")
                .plan(TenantEntity.Plan.BUSINESS)
                .build();
        when(tenantService.getCurrentTenant()).thenReturn(tenant);
        when(fileStorageService.uploadPhoto(any(), any(), eq("produits")))
                .thenThrow(new IllegalArgumentException("Fichier non supporté : seules les images JPG, PNG et WebP sont acceptées"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "test".getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .param("type", "produits"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Fichier non supporté")));
    }

    // ==================== DELETE /files/photos ====================

    @Test
    @DisplayName("DELETE /files/photos?url=... → succès → 200, success=true")
    void deletePhoto_Succes_RetourneOk() throws Exception {
        // Arrange
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("upload-tenant-004")
                .nomEntreprise("Boutique Upload 4")
                .numeroTelephone("+221770000004")
                .plan(TenantEntity.Plan.PRO)
                .build();
        when(tenantService.getCurrentTenant()).thenReturn(tenant);
        doNothing().when(fileStorageService).deletePhoto(any(), any());

        String photoUrl = "https://res.cloudinary.com/test/image/upload/achats/photo123.jpg";

        // Act & Assert
        mockMvc.perform(delete("/files/photos").param("url", photoUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Photo supprimée avec succès"));
    }

    @Test
    @DisplayName("DELETE /files/photos?url=... → SecurityException → 403")
    void deletePhoto_SecurityException_Retourne403() throws Exception {
        // Arrange
        TenantEntity tenant = TenantEntity.builder()
                .tenantUuid("upload-tenant-005")
                .nomEntreprise("Boutique Upload 5")
                .numeroTelephone("+221770000005")
                .plan(TenantEntity.Plan.PRO)
                .build();
        when(tenantService.getCurrentTenant()).thenReturn(tenant);
        doThrow(new SecurityException("Cette photo n'appartient pas à votre entreprise"))
                .when(fileStorageService).deletePhoto(any(), any());

        String photoUrl = "https://res.cloudinary.com/test/image/upload/autre-tenant/photo.jpg";

        // Act & Assert
        mockMvc.perform(delete("/files/photos").param("url", photoUrl))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cette photo n'appartient pas à votre entreprise"));
    }
}
