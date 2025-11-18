package com.example.dijasaliou.service;

import com.example.dijasaliou.dto.ContactRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@dijasaliou.com}")
    private String fromEmail;

    @Value("${app.email.support:support@dijasaliou.com}")
    private String supportEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envoie un email de réinitialisation de mot de passe
     *
     * @param toEmail Email du destinataire
     * @param token Token de réinitialisation
     * @param userName Nom de l'utilisateur
     */
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
     * Méthode générique pour envoyer un email HTML
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML

        mailSender.send(message);
    }

    /**
     * Méthode pour envoyer un email HTML avec Reply-To personnalisé
     *
     * @param to Destinataire
     * @param subject Sujet de l'email
     * @param htmlContent Contenu HTML
     * @param replyTo Adresse email pour les réponses
     */
    private void sendHtmlEmailWithReplyTo(String to, String subject, String htmlContent, String replyTo) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML
        helper.setReplyTo(replyTo); // Adresse pour les réponses

        mailSender.send(message);
    }
}
