package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ContactRequest;
import com.example.dijasaliou.entity.FactureEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service pour l'envoi d'emails
 *
 * Utilisé pour :
 * - Envoi du lien de réinitialisation de mot de passe
 * - Notifications diverses (optionnel)
 */
@Service
@Slf4j
public class EmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${brevo.api.key:disabled}")
    private String brevoApiKey;

    @Value("${app.email.from:contact@heasystock.com}")
    private String fromEmail;

    @Value("${app.email.support:contact@heasystock.com}")
    private String supportEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.backend.url:https://dijaboutique-production.up.railway.app}")
    private String backendUrl;

    /**
     * Envoie un email de vérification d'adresse email
     */
    @Async
    public void sendVerificationEmail(String toEmail, String token, String userName) {
        try {
            String verifyLink = backendUrl + "/api/auth/verify-email?token=" + token;

            String subject = "Confirmez votre adresse email - HeasyStock";
            String htmlContent = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px'>"
                    + "<h2 style='color:#2563eb'>Bienvenue sur HeasyStock, " + userName + " !</h2>"
                    + "<p>Merci de vous être inscrit. Veuillez confirmer votre adresse email en cliquant sur le bouton ci-dessous :</p>"
                    + "<div style='text-align:center;margin:30px 0'>"
                    + "<a href='" + verifyLink + "' style='background-color:#2563eb;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;font-size:16px'>Confirmer mon email</a>"
                    + "</div>"
                    + "<p style='color:#666;font-size:14px'>Ce lien est valable 24 heures.</p>"
                    + "<p style='color:#666;font-size:14px'>Si vous n'avez pas créé de compte sur HeasyStock, ignorez cet email.</p>"
                    + "</div>";
            sendHtmlEmail(toEmail, subject, htmlContent);
            log.info("Email de vérification envoyé à : {}", toEmail);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de vérification à {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Envoie un email de réinitialisation de mot de passe
     *
     * @param toEmail Email du destinataire
     * @param token Token de réinitialisation
     * @param userName Nom de l'utilisateur
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String token, String userName) {
        try {
            String resetLink = frontendUrl + "/reset-password/" + token;

            String subject = "Réinitialisation de votre mot de passe - Dija Saliou";

            String htmlContent = buildPasswordResetEmailContent(userName, resetLink);

            sendHtmlEmail(toEmail, subject, htmlContent);

            log.info("Email de réinitialisation envoyé à : {}", toEmail);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de réinitialisation à {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email de réinitialisation", e);
        }
    }

    /**
     * Construit le contenu HTML de l'email de réinitialisation
     */
    private String buildPasswordResetEmailContent(String userName, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .container {
                        background-color: #f9f9f9;
                        border-radius: 8px;
                        padding: 30px;
                        border: 1px solid #ddd;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    .header h1 {
                        color: #2563eb;
                        margin: 0;
                    }
                    .content {
                        background-color: white;
                        padding: 20px;
                        border-radius: 6px;
                        margin-bottom: 20px;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 30px;
                        background-color: #2563eb;
                        color: white !important;
                        text-decoration: none;
                        border-radius: 5px;
                        margin: 20px 0;
                        font-weight: bold;
                    }
                    .button:hover {
                        background-color: #1d4ed8;
                    }
                    .footer {
                        text-align: center;
                        color: #666;
                        font-size: 12px;
                        margin-top: 20px;
                    }
                    .warning {
                        background-color: #fef3c7;
                        border-left: 4px solid #f59e0b;
                        padding: 10px;
                        margin: 15px 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Dija Saliou</h1>
                        <p>Gestion de Boutique</p>
                    </div>

                    <div class="content">
                        <h2>Bonjour %s,</h2>

                        <p>Vous avez demandé la réinitialisation de votre mot de passe.</p>

                        <p>Cliquez sur le bouton ci-dessous pour créer un nouveau mot de passe :</p>

                        <div style="text-align: center;">
                            <a href="%s" class="button">Réinitialiser mon mot de passe</a>
                        </div>

                        <p>Ou copiez ce lien dans votre navigateur :</p>
                        <p style="word-break: break-all; color: #2563eb;">%s</p>

                        <div class="warning">
                            <strong>Important :</strong> Ce lien expirera dans <strong>1 heure</strong> pour des raisons de sécurité.
                        </div>

                        <p><strong>Vous n'avez pas demandé cette réinitialisation ?</strong><br>
                        Ignorez simplement cet email. Votre mot de passe actuel reste inchangé.</p>
                    </div>

                    <div class="footer">
                        <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                        <p>&copy; 2025 Dija Saliou - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetLink, resetLink);
    }

    /**
     * Envoie un email de contact à l'équipe de support
     *
     * @param request Les informations du formulaire de contact
     */
    @Async
    public void sendContactEmail(ContactRequest request) {
        try {
            String subject = "Contact depuis l'application - " + request.getSujet();

            String htmlContent = buildContactEmailContent(request);

            // Envoyer l'email avec Reply-To configuré sur l'email de l'expéditeur
            sendHtmlEmailWithReplyTo(supportEmail, subject, htmlContent, request.getEmail());

            log.info("Email de contact envoyé à l'équipe de support depuis : {}", request.getEmail());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de contact depuis {}: {}", request.getEmail(), e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email de contact", e);
        }
    }

    /**
     * Construit le contenu HTML de l'email de contact
     */
    private String buildContactEmailContent(ContactRequest request) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 700px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .container {
                        background-color: #f9f9f9;
                        border-radius: 8px;
                        padding: 30px;
                        border: 1px solid #ddd;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        background-color: #2563eb;
                        color: white;
                        padding: 20px;
                        border-radius: 6px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24px;
                    }
                    .content {
                        background-color: white;
                        padding: 25px;
                        border-radius: 6px;
                        margin-bottom: 20px;
                    }
                    .info-section {
                        background-color: #f0f9ff;
                        border-left: 4px solid #2563eb;
                        padding: 15px;
                        margin: 20px 0;
                    }
                    .info-row {
                        margin: 10px 0;
                    }
                    .label {
                        font-weight: bold;
                        color: #1e40af;
                        display: inline-block;
                        min-width: 120px;
                    }
                    .message-box {
                        background-color: #fafafa;
                        border: 1px solid #e5e7eb;
                        padding: 15px;
                        border-radius: 6px;
                        margin: 15px 0;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                    }
                    .footer {
                        text-align: center;
                        color: #666;
                        font-size: 12px;
                        margin-top: 20px;
                        padding-top: 20px;
                        border-top: 1px solid #ddd;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📧 Nouveau Message de Contact</h1>
                        <p style="margin: 5px 0;">Dija Saliou - Application de Gestion</p>
                    </div>

                    <div class="content">
                        <h2 style="color: #1e40af; margin-top: 0;">Informations de l'expéditeur</h2>

                        <div class="info-section">
                            <div class="info-row">
                                <span class="label">Nom :</span>
                                <span>%s</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Email :</span>
                                <span><a href="mailto:%s">%s</a></span>
                            </div>
                            <div class="info-row">
                                <span class="label">Téléphone :</span>
                                <span>%s</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Entreprise :</span>
                                <span>%s</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Sujet :</span>
                                <span>%s</span>
                            </div>
                        </div>

                        <h3 style="color: #1e40af;">Message :</h3>
                        <div class="message-box">%s</div>

                        <p style="margin-top: 20px; padding: 10px; background-color: #fef3c7; border-radius: 4px;">
                            💡 <strong>Astuce :</strong> Vous pouvez répondre directement en cliquant sur l'adresse email ci-dessus.
                        </p>
                    </div>

                    <div class="footer">
                        <p>Cet email a été envoyé automatiquement depuis l'application Dija Saliou.</p>
                        <p>&copy; 2025 Dija Saliou - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                    request.getNom(),
                    request.getEmail(),
                    request.getEmail(),
                    request.getTelephone() != null ? request.getTelephone() : "Non renseigné",
                    request.getEntreprise(),
                    request.getSujet(),
                    request.getMessage()
            );
    }

    /**
     * Envoie un email de confirmation de paiement
     *
     * @param toEmail Email du client
     * @param userName Nom complet de l'utilisateur
     * @param nomEntreprise Nom de l'entreprise
     * @param plan Plan souscrit
     * @param montant Montant payé
     * @param devise Devise (EUR ou CFA)
     * @param dateExpiration Date d'expiration de l'abonnement
     */
    @Async
    public void sendPaymentConfirmationEmail(String toEmail, String userName, String nomEntreprise,
                                             String plan, double montant, String devise,
                                             String dateExpiration) {
        try {
            String subject = "Confirmation de paiement - Abonnement " + plan + " activé";

            String htmlContent = buildPaymentConfirmationEmailContent(
                    userName, nomEntreprise, plan, montant, devise, dateExpiration);

            sendHtmlEmail(toEmail, subject, htmlContent);

            log.info("Email de confirmation de paiement envoyé à : {} pour le plan {}", toEmail, plan);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de confirmation de paiement à {}: {}",
                    toEmail, e.getMessage());
            // On ne throw pas pour ne pas bloquer le processus de paiement
        }
    }

    /**
     * Construit le contenu HTML de l'email de confirmation de paiement
     */
    private String buildPaymentConfirmationEmailContent(String userName, String nomEntreprise,
                                                        String plan, double montant, String devise,
                                                        String dateExpiration) {
        String montantFormate = String.format("%.2f %s", montant, devise);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 700px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .container {
                        background-color: #f9f9f9;
                        border-radius: 8px;
                        padding: 30px;
                        border: 1px solid #ddd;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        background: linear-gradient(135deg, #10b981 0%%, #059669 100%%);
                        color: white;
                        padding: 30px;
                        border-radius: 6px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                    }
                    .success-icon {
                        font-size: 60px;
                        margin-bottom: 10px;
                    }
                    .content {
                        background-color: white;
                        padding: 30px;
                        border-radius: 6px;
                        margin-bottom: 20px;
                    }
                    .info-box {
                        background: linear-gradient(135deg, #f0f9ff 0%%, #e0f2fe 100%%);
                        border-left: 4px solid #2563eb;
                        padding: 20px;
                        margin: 20px 0;
                        border-radius: 6px;
                    }
                    .info-row {
                        display: flex;
                        justify-content: space-between;
                        padding: 10px 0;
                        border-bottom: 1px solid #e5e7eb;
                    }
                    .info-row:last-child {
                        border-bottom: none;
                    }
                    .label {
                        font-weight: bold;
                        color: #1e40af;
                    }
                    .value {
                        color: #333;
                        text-align: right;
                    }
                    .plan-details {
                        background-color: #fef3c7;
                        border: 2px solid #f59e0b;
                        padding: 20px;
                        border-radius: 8px;
                        margin: 25px 0;
                    }
                    .plan-details h3 {
                        color: #d97706;
                        margin-top: 0;
                    }
                    .plan-features {
                        list-style: none;
                        padding: 0;
                    }
                    .plan-features li {
                        padding: 8px 0;
                        padding-left: 25px;
                        position: relative;
                    }
                    .plan-features li:before {
                        content: "✓";
                        position: absolute;
                        left: 0;
                        color: #059669;
                        font-weight: bold;
                        font-size: 18px;
                    }
                    .button {
                        display: inline-block;
                        padding: 15px 40px;
                        background: linear-gradient(135deg, #2563eb 0%%, #1d4ed8 100%%);
                        color: white !important;
                        text-decoration: none;
                        border-radius: 6px;
                        margin: 20px 0;
                        font-weight: bold;
                        font-size: 16px;
                        text-align: center;
                    }
                    .button:hover {
                        background: linear-gradient(135deg, #1d4ed8 0%%, #1e40af 100%%);
                    }
                    .footer {
                        text-align: center;
                        color: #666;
                        font-size: 12px;
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 2px solid #e5e7eb;
                    }
                    .support-box {
                        background-color: #f3f4f6;
                        padding: 15px;
                        border-radius: 6px;
                        margin-top: 20px;
                        text-align: center;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="success-icon">✓</div>
                        <h1>Paiement Confirmé !</h1>
                        <p style="margin: 5px 0; font-size: 16px;">Votre abonnement est maintenant actif</p>
                    </div>

                    <div class="content">
                        <h2 style="color: #1e40af;">Bonjour %s,</h2>

                        <p style="font-size: 16px;">
                            Nous avons bien reçu votre paiement. Votre abonnement <strong>%s</strong>
                            est maintenant actif et vous pouvez profiter pleinement de toutes les fonctionnalités
                            de Dija Saliou !
                        </p>

                        <div class="info-box">
                            <h3 style="margin-top: 0; color: #1e40af;">📋 Détails de la transaction</h3>
                            <div class="info-row">
                                <span class="label">Entreprise :</span>
                                <span class="value">%s</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Plan souscrit :</span>
                                <span class="value">%s</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Montant payé :</span>
                                <span class="value"><strong>%s</strong></span>
                            </div>
                            <div class="info-row">
                                <span class="label">Date d'expiration :</span>
                                <span class="value">%s</span>
                            </div>
                        </div>

                        <div class="plan-details">
                            <h3>🎉 Votre plan %s inclut :</h3>
                            <ul class="plan-features">
                                <li>Gestion complète des ventes, achats et stock</li>
                                <li>Gestion des dépenses et des contacts</li>
                                <li>Dashboard de suivi en temps réel</li>
                                <li>Rapports et statistiques détaillés</li>
                                <li>Export PDF des données</li>
                                <li>Support technique prioritaire</li>
                                <li>Sauvegardes automatiques quotidiennes</li>
                            </ul>
                        </div>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s/dashboard" class="button">Accéder à mon espace</a>
                        </div>

                        <div class="support-box">
                            <p style="margin: 5px 0;">
                                <strong>Besoin d'aide ?</strong><br>
                                Notre équipe support est à votre disposition :
                                <a href="mailto:%s">%s</a>
                            </p>
                        </div>

                        <p style="margin-top: 25px; font-size: 14px; color: #666;">
                            💡 <strong>Astuce :</strong> Pensez à compléter les informations de votre entreprise
                            dans les paramètres pour profiter pleinement de toutes les fonctionnalités.
                        </p>
                    </div>

                    <div class="footer">
                        <p><strong>Merci de votre confiance !</strong></p>
                        <p>Cet email a été envoyé automatiquement suite à votre paiement.</p>
                        <p>&copy; 2025 Dija Saliou - Gestion de Boutique - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                    userName,
                    plan,
                    nomEntreprise,
                    plan,
                    montantFormate,
                    dateExpiration,
                    plan,
                    frontendUrl,
                    supportEmail,
                    supportEmail
            );
    }

    /**
     * Envoie un email d'alerte de stock faible
     *
     * @param toEmail Email de l'admin
     * @param userName Nom de l'admin
     * @param nomEntreprise Nom de l'entreprise
     * @param nomProduit Nom du produit en rupture/stock faible
     * @param stockActuel Stock actuel
     * @param seuilAlerte Seuil qui a déclenché l'alerte (15, 10, 5, ou 0)
     */
    @Async
    public void sendStockAlertEmail(String toEmail, String userName, String nomEntreprise,
                                    String nomProduit, int stockActuel, int seuilAlerte) {
        try {
            String subject = getStockAlertSubject(stockActuel, nomProduit);

            String htmlContent = buildStockAlertEmailContent(
                    userName, nomEntreprise, nomProduit, stockActuel, seuilAlerte);

            sendHtmlEmail(toEmail, subject, htmlContent);

            log.info("Email d'alerte de stock envoyé à : {} pour le produit {} (stock: {})",
                    toEmail, nomProduit, stockActuel);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email d'alerte de stock à {}: {}",
                    toEmail, e.getMessage());
            // On ne throw pas pour ne pas bloquer le processus de vente
        }
    }

    /**
     * Génère le sujet de l'email selon le niveau de stock
     */
    private String getStockAlertSubject(int stockActuel, String nomProduit) {
        if (stockActuel == 0) {
            return "RUPTURE DE STOCK - " + nomProduit;
        } else if (stockActuel <= 5) {
            return "STOCK CRITIQUE - " + nomProduit + " (" + stockActuel + " restants)";
        } else if (stockActuel <= 10) {
            return "Stock faible - " + nomProduit + " (" + stockActuel + " restants)";
        } else {
            return "Alerte stock - " + nomProduit + " (" + stockActuel + " restants)";
        }
    }

    /**
     * Construit le contenu HTML de l'email d'alerte de stock
     */
    private String buildStockAlertEmailContent(String userName, String nomEntreprise,
                                               String nomProduit, int stockActuel, int seuilAlerte) {
        // Déterminer le niveau de criticité et le message
        String criticite;
        String couleurAlerte;
        String icone;
        String message;
        String actionRecommandee;

        if (stockActuel == 0) {
            criticite = "RUPTURE DE STOCK";
            couleurAlerte = "#dc2626"; // Rouge
            icone = "ALERTE";
            message = "Le produit <strong>" + nomProduit + "</strong> est en <strong>RUPTURE DE STOCK</strong>.";
            actionRecommandee = "Réapprovisionner <strong>IMMÉDIATEMENT</strong> ce produit pour éviter de perdre des ventes.";
        } else if (stockActuel <= 5) {
            criticite = "STOCK CRITIQUE";
            couleurAlerte = "#ea580c"; // Orange foncé
            icone = "ATTENTION";
            message = "Le produit <strong>" + nomProduit + "</strong> est en <strong>STOCK CRITIQUE</strong>.";
            actionRecommandee = "Réapprovisionner ce produit <strong>dès que possible</strong> avant la rupture.";
        } else if (stockActuel <= 10) {
            criticite = "STOCK FAIBLE";
            couleurAlerte = "#f59e0b"; // Orange
            icone = "ATTENTION";
            message = "Le produit <strong>" + nomProduit + "</strong> a un <strong>stock faible</strong>.";
            actionRecommandee = "Prévoir un réapprovisionnement dans les prochains jours.";
        } else {
            criticite = "ALERTE STOCK";
            couleurAlerte = "#3b82f6"; // Bleu
            icone = "INFO";
            message = "Le produit <strong>" + nomProduit + "</strong> approche du seuil de stock minimal.";
            actionRecommandee = "Surveiller le stock et prévoir un réapprovisionnement.";
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 700px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .container {
                        background-color: #f9f9f9;
                        border-radius: 8px;
                        padding: 30px;
                        border: 1px solid #ddd;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        background: %s;
                        color: white;
                        padding: 30px;
                        border-radius: 6px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                    }
                    .alert-icon {
                        font-size: 20px;
                        font-weight: bold;
                        margin-bottom: 10px;
                    }
                    .content {
                        background-color: white;
                        padding: 30px;
                        border-radius: 6px;
                        margin-bottom: 20px;
                    }
                    .alert-box {
                        background: linear-gradient(135deg, #fef2f2 0%%, #fee2e2 100%%);
                        border-left: 4px solid %s;
                        padding: 20px;
                        margin: 20px 0;
                        border-radius: 6px;
                    }
                    .stock-info {
                        background-color: #f3f4f6;
                        padding: 20px;
                        border-radius: 6px;
                        margin: 20px 0;
                    }
                    .info-row {
                        display: flex;
                        justify-content: space-between;
                        padding: 10px 0;
                        border-bottom: 1px solid #e5e7eb;
                    }
                    .info-row:last-child {
                        border-bottom: none;
                    }
                    .label {
                        font-weight: bold;
                        color: #374151;
                    }
                    .value {
                        color: #111827;
                        text-align: right;
                    }
                    .stock-value {
                        font-size: 36px;
                        font-weight: bold;
                        color: %s;
                        text-align: center;
                        margin: 20px 0;
                    }
                    .action-box {
                        background: linear-gradient(135deg, #dbeafe 0%%, #bfdbfe 100%%);
                        border-left: 4px solid #2563eb;
                        padding: 20px;
                        border-radius: 6px;
                        margin: 25px 0;
                    }
                    .action-box h3 {
                        color: #1e40af;
                        margin-top: 0;
                    }
                    .button {
                        display: inline-block;
                        padding: 15px 40px;
                        background: linear-gradient(135deg, #2563eb 0%%, #1d4ed8 100%%);
                        color: white !important;
                        text-decoration: none;
                        border-radius: 6px;
                        margin: 20px 0;
                        font-weight: bold;
                        font-size: 16px;
                        text-align: center;
                    }
                    .footer {
                        text-align: center;
                        color: #666;
                        font-size: 12px;
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 2px solid #e5e7eb;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="alert-icon">%s</div>
                        <h1>%s</h1>
                        <p style="margin: 5px 0; font-size: 16px;">%s</p>
                    </div>

                    <div class="content">
                        <h2 style="color: #1e40af;">Bonjour %s,</h2>

                        <p style="font-size: 16px;">
                            %s
                        </p>

                        <div class="stock-value">
                            %d unité%s en stock
                        </div>

                        <div class="stock-info">
                            <h3 style="margin-top: 0; color: #374151;">Informations du produit</h3>
                            <div class="info-row">
                                <span class="label">Entreprise :</span>
                                <span class="value">%s</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Produit :</span>
                                <span class="value">%s</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Stock actuel :</span>
                                <span class="value" style="color: %s; font-weight: bold;">%d unité%s</span>
                            </div>
                            <div class="info-row">
                                <span class="label">Seuil d'alerte :</span>
                                <span class="value">%d unités</span>
                            </div>
                        </div>

                        <div class="alert-box">
                            <p style="margin: 0; font-size: 15px;">
                                <strong>Attention :</strong> Les ventes de ce produit risquent d'être bloquées
                                si le stock n'est pas réapprovisionné rapidement.
                            </p>
                        </div>

                        <div class="action-box">
                            <h3>Action recommandée</h3>
                            <p style="margin: 10px 0;">%s</p>
                        </div>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s/achats" class="button">Créer un achat</a>
                        </div>

                        <p style="margin-top: 25px; font-size: 14px; color: #666; text-align: center;">
                            Cette alerte a été générée automatiquement par le système de gestion de stock.
                        </p>
                    </div>

                    <div class="footer">
                        <p>Cet email a été envoyé automatiquement par le système d'alerte de stock.</p>
                        <p>&copy; 2025 Dija Saliou - Gestion de Boutique - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                    couleurAlerte,  // Header background
                    couleurAlerte,  // Border alert-box
                    couleurAlerte,  // Stock value color
                    icone,  // Icône
                    criticite,  // Titre
                    nomEntreprise,  // Sous-titre
                    userName,  // Bonjour
                    message,  // Message principal
                    stockActuel, stockActuel > 1 ? "s" : "",  // Stock actuel (avec pluriel)
                    nomEntreprise,  // Info entreprise
                    nomProduit,  // Info produit
                    couleurAlerte, stockActuel, stockActuel > 1 ? "s" : "",  // Stock actuel dans le tableau
                    seuilAlerte,  // Seuil d'alerte
                    actionRecommandee,  // Action recommandée
                    frontendUrl  // Lien vers achats
            );
    }

    /**
     * Envoie une facture d'abonnement par email au client
     *
     * @param facture La facture à envoyer
     */
    @Async
    public void sendFactureEmail(FactureEntity facture) {
        try {
            String subject = "Facture " + facture.getNumeroFacture() + " - Abonnement HeasyStock";
            String htmlContent = buildFactureEmailContent(facture);
            sendHtmlEmail(facture.getAdminEmail(), subject, htmlContent);
            log.info("Facture {} envoyée par email à {}", facture.getNumeroFacture(), facture.getAdminEmail());
        } catch (Exception e) {
            log.error("Erreur envoi facture {} à {}: {}", facture.getNumeroFacture(), facture.getAdminEmail(), e.getMessage());
            throw new RuntimeException("Impossible d'envoyer la facture par email", e);
        }
    }

    /**
     * Construit le contenu HTML de la facture
     */
    private String buildFactureEmailContent(FactureEntity facture) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateFacture = facture.getDateFacture() != null ? facture.getDateFacture().format(fmt) : "—";
        String dateDebut   = facture.getDateDebutPeriode() != null ? facture.getDateDebutPeriode().format(fmt) : "—";
        String dateFin     = facture.getDateFinPeriode() != null ? facture.getDateFinPeriode().format(fmt) : "—";
        String montantCFA  = facture.getMontantCFA() != null ? String.format("%.0f FCFA", facture.getMontantCFA()) : "—";
        String montantEur  = facture.getMontantEuro() != null ? String.format("%.2f €", facture.getMontantEuro()) : "—";
        String adminNomComplet = ((facture.getAdminPrenom() != null ? facture.getAdminPrenom() : "") + " "
                + (facture.getAdminNom() != null ? facture.getAdminNom() : "")).trim();
        String statutLabel = "PAYEE".equals(facture.getStatut() != null ? facture.getStatut().name() : "") ? "Payée" : "Réglée manuellement";

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
            <style>
              body { font-family: Arial, sans-serif; color: #333; max-width: 700px; margin: 0 auto; padding: 20px; }
              .header { background: linear-gradient(135deg, #1a1a2e 0%%, #16213e 100%%); color: white; padding: 30px; border-radius: 8px 8px 0 0; }
              .header h1 { margin: 0; font-size: 24px; }
              .header p { margin: 5px 0 0; opacity: 0.8; font-size: 14px; }
              .badge { background: #e74c3c; color: white; padding: 3px 10px; border-radius: 4px; font-size: 11px; font-weight: bold; display: inline-block; margin-bottom: 8px; }
              .body { background: white; border: 1px solid #ddd; border-top: none; padding: 30px; }
              .facture-num { font-size: 20px; font-weight: bold; color: #1a1a2e; margin-bottom: 5px; }
              .facture-date { color: #666; font-size: 14px; margin-bottom: 25px; }
              .two-col { display: flex; gap: 20px; margin-bottom: 25px; }
              .col { flex: 1; background: #f8f9fa; padding: 15px; border-radius: 6px; }
              .col h3 { margin: 0 0 10px; font-size: 13px; color: #888; text-transform: uppercase; letter-spacing: 0.5px; }
              .col p { margin: 4px 0; font-size: 14px; }
              .col .company { font-size: 16px; font-weight: bold; color: #1a1a2e; }
              table.detail { width: 100%%; border-collapse: collapse; margin: 20px 0; }
              table.detail th { background: #1a1a2e; color: white; padding: 10px 15px; text-align: left; font-size: 13px; }
              table.detail td { padding: 12px 15px; border-bottom: 1px solid #eee; font-size: 14px; }
              table.detail tr:last-child td { border-bottom: none; }
              .total-row td { font-weight: bold; font-size: 16px; background: #f0f9ff; color: #1a1a2e; }
              .statut-badge { background: #d4edda; color: #155724; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: bold; }
              .footer { background: #f8f9fa; padding: 15px 30px; border-radius: 0 0 8px 8px; border: 1px solid #ddd; border-top: none; font-size: 12px; color: #888; text-align: center; }
            </style>
            </head>
            <body>
              <div class="header">
                <div class="badge">SUPER ADMIN</div>
                <h1>HeasyStock</h1>
                <p>Plateforme de gestion de boutique</p>
              </div>
              <div class="body">
                <div class="facture-num">FACTURE N° %s</div>
                <div class="facture-date">Date : %s &nbsp;|&nbsp; Statut : <span class="statut-badge">%s</span></div>

                <div class="two-col">
                  <div class="col">
                    <h3>Émetteur</h3>
                    <p class="company">HeasyStock</p>
                    <p>Plateforme SaaS de gestion de boutique</p>
                    <p>support@heasystock.com</p>
                  </div>
                  <div class="col">
                    <h3>Client</h3>
                    <p class="company">%s</p>
                    <p>%s</p>
                    <p>%s%s</p>
                    %s
                    <p>%s</p>
                  </div>
                </div>

                <table class="detail">
                  <thead>
                    <tr>
                      <th>Description</th>
                      <th>Période</th>
                      <th>Montant CFA</th>
                      <th>Montant EUR</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>Abonnement <strong>%s</strong></td>
                      <td>%s → %s</td>
                      <td>%s</td>
                      <td>%s</td>
                    </tr>
                    <tr class="total-row">
                      <td colspan="2">TOTAL</td>
                      <td>%s</td>
                      <td>%s</td>
                    </tr>
                  </tbody>
                </table>

                <p style="font-size:13px;color:#666;margin-top:20px;">
                  Merci pour votre confiance. Pour toute question concernant cette facture,
                  contactez-nous à <a href="mailto:support@heasystock.com">support@heasystock.com</a>
                </p>
              </div>
              <div class="footer">
                &copy; 2025 HeasyStock · Tous droits réservés · Cette facture a été générée automatiquement
              </div>
            </body>
            </html>
            """.formatted(
                facture.getNumeroFacture(),
                dateFacture,
                statutLabel,
                // Client
                facture.getNomEntreprise() != null ? facture.getNomEntreprise() : "—",
                adminNomComplet.isBlank() ? "—" : adminNomComplet,
                facture.getVille() != null ? facture.getVille() : "",
                facture.getPays() != null ? (", " + facture.getPays()) : "",
                facture.getNineaSiret() != null ? "<p>NINEA/SIRET : " + facture.getNineaSiret() + "</p>" : "",
                facture.getAdminEmail() != null ? facture.getAdminEmail() : "—",
                // Tableau
                facture.getPlan() != null ? facture.getPlan() : "—",
                dateDebut, dateFin,
                montantCFA, montantEur,
                montantCFA, montantEur
        );
    }

    /**
     * Méthode générique pour envoyer un email HTML via l'API Brevo (HTTPS port 443)
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        sendViaBrevo(to, subject, htmlContent, null);
    }

    /**
     * Méthode publique pour envoyer un email HTML — utilisée par NotificationService
     */
    public void sendHtmlEmailPublic(String to, String subject, String htmlContent) {
        sendViaBrevo(to, subject, htmlContent, null);
    }

    /**
     * Méthode pour envoyer un email HTML avec Reply-To personnalisé via Brevo
     */
    private void sendHtmlEmailWithReplyTo(String to, String subject, String htmlContent, String replyTo) {
        sendViaBrevo(to, subject, htmlContent, replyTo);
    }

    /**
     * Appel HTTP à l'API Brevo pour l'envoi d'email
     * Utilise HTTPS port 443 — non bloqué par Railway
     */
    private void sendViaBrevo(String to, String subject, String htmlContent, String replyTo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("sender", Map.of("name", "HeasyStock", "email", fromEmail));
        body.put("to", List.of(Map.of("email", to)));
        body.put("subject", subject);
        body.put("htmlContent", htmlContent);
        if (replyTo != null) {
            body.put("replyTo", Map.of("email", replyTo));
        }
        body.put("trackClicks", false);
        body.put("trackOpens", false);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(BREVO_API_URL, request, String.class);
    }
}
