package utec.kinetica.auth.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import utec.kinetica.auth.domain.RegistrationNotifier;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailRegistrationNotifier implements RegistrationNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailRegistrationNotifier.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;
    private final String welcomeSubject;

    public EmailRegistrationNotifier(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${app.mail.from:no-reply@kinetica.local}") String fromEmail,
            @Value("${app.mail.welcome-subject:Bienvenido a Kinetica}") String welcomeSubject
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
        this.welcomeSubject = welcomeSubject;
    }

    @Override
    @Async("appTaskExecutor")
    public void notifyWelcome(String email) {
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            Context context = new Context();
            context.setVariable("email", email);
            context.setVariable("supportEmail", fromEmail);
            String htmlBody = templateEngine.process("welcome-email", context);

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(welcomeSubject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception ex) {
            LOGGER.warn("Welcome email failed for {}: {}", email, ex.getMessage());
        }
    }
}
