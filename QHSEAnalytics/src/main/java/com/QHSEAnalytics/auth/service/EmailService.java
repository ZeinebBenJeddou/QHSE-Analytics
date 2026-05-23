package com.QHSEAnalytics.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    @Value("${app.frontend.url}")
    private String frontendUrl;


    @Async
    public void sendVerificationEmail(String to, String nom, String token) {
        String link = frontendUrl + "/auth/verify?token=" + token;
        String subject = "Vérification de votre compte QHSE Analytics";
        String body = buildHtml(
                "Bonjour " + nom + ",",
                "Cliquez sur le bouton ci-dessous pour vérifier votre compte.",
                "Vérifier mon compte",
                link,
                "Ce lien expire dans 24 heures."
        );
        sendEmail(to, subject, body);
    }


    @Async
    public void sendPasswordResetEmail(String to, String nom, String token) {
        String link = frontendUrl + "/auth/reset-password?token=" + token;
        String subject = "Réinitialisation de votre mot de passe QHSE Analytics";
        String body = buildHtml(
                "Bonjour " + nom + ",",
                "Vous avez demandé la réinitialisation de votre mot de passe.",
                "Réinitialiser mon mot de passe",
                link,
                "Ce lien expire dans 1 heure. Si vous n'avez pas fait cette demande, ignorez cet email."
        );
        sendEmail(to, subject, body);
    }


    @Async
    public void sendOtpEmail(String to, String nom, String code) {
        String subject = "Votre code de connexion QHSE Analytics";
        String body = buildOtpHtml(nom, code);
        sendEmail(to, subject, body);
    }

    @Async
    public void sendWelcomeEmail(String to, String prenom, String tempPassword) {
        String link = frontendUrl + "/auth/login";
        String subject = "Bienvenue sur QHSE Analytics";
        String body = buildWelcomeHtml(prenom, to, tempPassword, link);
        sendEmail(to, subject, body);
    }


    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email envoyé avec succès");
        } catch (MessagingException e) {
            log.error("Erreur envoi email : {}", e.getMessage());
        }
    }


    private String buildHtml(String greeting, String message, String btnText, String btnLink, String footer) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;">
                  <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;padding:32px;">
                    <h2 style="color:#1a3c5e;">QHSE Analytics</h2>
                    <p style="color:#333;">%s</p>
                    <p style="color:#555;">%s</p>
                    <div style="text-align:center;margin:32px 0;">
                      <a href="%s"
                         style="background:#1976d2;color:#fff;padding:12px 28px;border-radius:6px;
                                text-decoration:none;font-weight:bold;">
                        %s
                      </a>
                    </div>
                    <p style="color:#888;font-size:12px;">%s</p>
                  </div>
                </body>
                </html>
                """.formatted(greeting, message, btnLink, btnText, footer);
    }

    private String buildOtpHtml(String nom, String code) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;">
                  <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;padding:32px;">
                    <h2 style="color:#1a3c5e;">QHSE Analytics</h2>
                    <p style="color:#333;">Bonjour %s,</p>
                    <p style="color:#555;">Votre code de connexion est :</p>
                    <div style="text-align:center;margin:32px 0;">
                      <span style="font-size:36px;font-weight:bold;letter-spacing:12px;
                                   color:#1976d2;background:#e3f2fd;padding:16px 24px;
                                   border-radius:8px;">%s</span>
                    </div>
                    <p style="color:#888;font-size:12px;">
                      Ce code expire dans 10 minutes. Ne le partagez jamais.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(nom, code);
    }

    private String buildWelcomeHtml(String prenom, String email, String tempPassword, String loginLink) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;">
                  <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;padding:32px;">
                    <h2 style="color:#1a3c5e;">QHSE Analytics</h2>
                    <p style="color:#333;">Bonjour %s,</p>
                    <p style="color:#555;">Votre compte analyste a été créé par un administrateur.</p>
                    <p style="color:#555;">Identifiant : <strong>%s</strong></p>
                    <p style="color:#555;">Mot de passe temporaire : <strong>%s</strong></p>
                    <p style="color:#555;">Connectez-vous puis changez immédiatement votre mot de passe.</p>
                    <div style="text-align:center;margin:32px 0;">
                      <a href="%s"
                         style="background:#1976d2;color:#fff;padding:12px 28px;border-radius:6px;
                                text-decoration:none;font-weight:bold;">
                        Accéder à la connexion
                      </a>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(prenom, email, tempPassword, loginLink);
    }
}
