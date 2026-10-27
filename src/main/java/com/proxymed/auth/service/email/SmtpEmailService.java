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
        envoyer(destinataire, "PROXYMED - Votre code de vérification", """
                Bonjour %s,

                Votre code de vérification PROXYMED est : %s

                Ce code est valable %d minutes.

                Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.
                """.formatted(nomComplet, code, validite.toMinutes()));
    }

    @Override
    public void envoyerMotDePasseTemporaire(String destinataire, String nomComplet, String motDePasseTemporaire) {
        envoyer(destinataire, "PROXYMED - Votre mot de passe temporaire", """
                Bonjour %s,

                Un administrateur PROXYMED a préparé votre accès.

                Identifiant : %s
                Mot de passe temporaire : %s

                À votre première connexion, remplacez ce mot de passe temporaire par un mot de passe
                personnel. Ne le communiquez à personne.
                """.formatted(nomComplet, destinataire, motDePasseTemporaire));
    }

    private void envoyer(String destinataire, String sujet, String texte) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(expediteur);
        message.setTo(destinataire);
        message.setSubject(sujet);
        message.setText(texte);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Ni le destinataire ni le contenu (code, mot de passe) ne sont logges.
            log.error("Echec de l'envoi de l'email \"{}\" : {}", sujet, e.getClass().getSimpleName());
            throw new EnvoiEmailException(e);
        }
    }
}
