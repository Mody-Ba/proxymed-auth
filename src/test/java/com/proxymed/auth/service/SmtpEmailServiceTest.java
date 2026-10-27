package com.proxymed.auth.service;

import com.proxymed.auth.exception.EnvoiEmailException;
import com.proxymed.auth.service.email.SmtpEmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * JavaMailSender est un mock : aucun email n'est envoye.
 */
class SmtpEmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpEmailService emailService = new SmtpEmailService(mailSender);

    SmtpEmailServiceTest() {
        ReflectionTestUtils.setField(emailService, "expediteur", "expediteur@proxymed.test");
    }

    @Test
    void envoyerCodeInscription_emailEnFrancaisAvecLeCodeEtSaDuree() {
        emailService.envoyerCodeInscription("fatou.sow@proxymed.test", "Fatou Sow", "042137", Duration.ofMinutes(10));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("expediteur@proxymed.test");
        assertThat(message.getTo()).containsExactly("fatou.sow@proxymed.test");
        assertThat(message.getSubject()).contains("code de vérification");
        assertThat(message.getText())
                .contains("Bonjour Fatou Sow")
                .contains("042137")
                .contains("valable 10 minutes");
    }

    @Test
    void envoyerCodeInscription_echecSmtp_leveEnvoiEmailException() {
        doThrow(new MailSendException("connexion refusee")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.envoyerCodeInscription("a@proxymed.test", "A", "123456", Duration.ofMinutes(10)))
                .isInstanceOf(EnvoiEmailException.class);
    }

    @Test
    void envoyerMotDePasseTemporaire_emailEnFrancaisAvecIdentifiantEtMotDePasse() {
        emailService.envoyerMotDePasseTemporaire("moussa.ba@proxymed.test", "Moussa Ba", "Kx7#pQ2wzR4m");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("expediteur@proxymed.test");
        assertThat(message.getTo()).containsExactly("moussa.ba@proxymed.test");
        assertThat(message.getSubject()).contains("mot de passe temporaire");
        assertThat(message.getText())
                .contains("Bonjour Moussa Ba")
                .contains("Identifiant : moussa.ba@proxymed.test")
                .contains("Mot de passe temporaire : Kx7#pQ2wzR4m")
                .contains("remplacez ce mot de passe temporaire");
    }

    @Test
    void envoyerMotDePasseTemporaire_echecSmtp_leveEnvoiEmailException() {
        doThrow(new MailSendException("connexion refusee")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.envoyerMotDePasseTemporaire("a@proxymed.test", "A", "Kx7#pQ2wzR4m"))
                .isInstanceOf(EnvoiEmailException.class);
    }
}
