package utec.kinetica.auth.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import utec.kinetica.auth.domain.RegistrationNotifier;
import utec.kinetica.auth.domain.UserRegisteredEvent;

@Component
public class UserRegisteredEmailListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegisteredEmailListener.class);

    private final RegistrationNotifier registrationNotifier;

    public UserRegisteredEmailListener(RegistrationNotifier registrationNotifier) {
        this.registrationNotifier = registrationNotifier;
    }

    @Async("appTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            registrationNotifier.notifyWelcome(event.email());
        } catch (Exception ex) {
            LOGGER.warn("Welcome email notification failed for {}: {}", event.email(), ex.getMessage());
        }
    }
}
