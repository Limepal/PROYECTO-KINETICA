package utec.kinetica.auth.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import utec.kinetica.auth.domain.RegistrationNotifier;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpRegistrationNotifier implements RegistrationNotifier {
    @Override
    public void notifyWelcome(String email) {
    }
}
