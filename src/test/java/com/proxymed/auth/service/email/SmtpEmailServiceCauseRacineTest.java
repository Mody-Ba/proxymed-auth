package com.proxymed.auth.service.email;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;

import java.net.SocketTimeoutException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpEmailServiceCauseRacineTest {

    @Test
    void causeRacine_authentificationRefusee_messageDuServeur() {
        MailAuthenticationException e = new MailAuthenticationException(
                new AuthenticationFailedException("535 Username and Password not accepted"));

        assertThat(SmtpEmailService.causeRacine(e))
                .isEqualTo("AuthenticationFailedException: 535 Username and Password not accepted");
    }

    @Test
    void causeRacine_connexionImpossible_exceptionLaPlusProfonde() {
        MailSendException e = new MailSendException("Mail server connection failed",
                new MessagingException("Could not connect to SMTP host", new SocketTimeoutException("Connection timed out")));

        assertThat(SmtpEmailService.causeRacine(e)).isEqualTo("SocketTimeoutException: Connection timed out");
    }

    @Test
    void causeRacine_echecParMessage_adressesEmailMasquees() {
        MailSendException e = new MailSendException(Map.of(new Object(),
                new MessagingException("550 5.1.1 <fatou.sow@proxymed.test> User unknown")));

        assertThat(SmtpEmailService.causeRacine(e))
                .isEqualTo("MessagingException: 550 5.1.1 <***@***> User unknown")
                .doesNotContain("fatou.sow");
    }

    @Test
    void causeRacine_sansMessage_nomDeLaClasse() {
        assertThat(SmtpEmailService.causeRacine(new MailSendException("envoi", new SocketTimeoutException())))
                .isEqualTo("SocketTimeoutException");
    }
}
