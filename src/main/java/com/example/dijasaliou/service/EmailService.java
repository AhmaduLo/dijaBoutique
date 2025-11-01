package com.example.dijasaliou.service;

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
}
