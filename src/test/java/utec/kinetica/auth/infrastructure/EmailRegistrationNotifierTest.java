package utec.kinetica.auth.infrastructure;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailRegistrationNotifierTest {

    @Test
    void shouldSendHtmlEmailWhenWelcomeNotificationIsRequested() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        TemplateEngine templateEngine = mock(TemplateEngine.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("welcome-email"), any())).thenReturn("<html>ok</html>");

        EmailRegistrationNotifier notifier = new EmailRegistrationNotifier(
                mailSender,
                templateEngine,
                "no-reply@test.com",
                "Bienvenido"
        );

        notifier.notifyWelcome("user@test.com");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldNotThrowWhenMailSenderFailsDuringWelcomeNotification() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        TemplateEngine templateEngine = mock(TemplateEngine.class);
        doThrow(new RuntimeException("smtp down")).when(mailSender).createMimeMessage();

        EmailRegistrationNotifier notifier = new EmailRegistrationNotifier(
                mailSender,
                templateEngine,
                "no-reply@test.com",
                "Bienvenido"
        );

        notifier.notifyWelcome("user@test.com");
    }
}
