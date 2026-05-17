package utec.kinetica.auth.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import utec.kinetica.auth.domain.RegistrationNotifier;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailRegistrationNotifier implements RegistrationNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailRegistrationNotifier.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailRegistrationNotifier(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@kinetica.local}") String fromEmail
    ) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void notifyWelcome(String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Bienvenido a Kinetica");
            message.setText("Hola,\n\nTu cuenta en Kinetica fue creada correctamente.\n\n¡Bienvenido!\n");
            mailSender.send(message);
        } catch (Exception ex) {
            LOGGER.warn("Welcome email failed for {}: {}", email, ex.getMessage());
        }
    }
}
