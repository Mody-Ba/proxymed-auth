package com.proxymed.auth.service.email;

import com.proxymed.auth.exception.EnvoiEmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Envoi par SMTP (STARTTLS) via JavaMailSender, configure par les variables PROXYMED_MAIL_*.
 * L'expediteur est le compte SMTP (PROXYMED_MAIL_USERNAME). Le code n'est jamais logge.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String expediteur;

    @Override
    public void envoyerCodeInscription(String destinataire, String nomComplet, String code, Duration validite) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(expediteur);
        message.setTo(destinataire);
        message.setSubject("PROXYMED - Votre code de vérification");
        message.setText("""
                Bonjour %s,

                Votre code de vérification PROXYMED est : %s

                Ce code est valable %d minutes.

                Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.
                """.formatted(nomComplet, code, validite.toMinutes()));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Echec de l'envoi du code d'inscription : {}", e.getClass().getSimpleName());
            throw new EnvoiEmailException(e);
        }
    }
}
