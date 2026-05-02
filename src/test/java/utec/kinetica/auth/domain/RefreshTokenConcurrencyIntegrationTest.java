package utec.kinetica.auth.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import utec.kinetica.auth.application.dto.AuthResponse;
import utec.kinetica.auth.application.dto.RefreshRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RefreshTokenConcurrencyIntegrationTest {

    @Autowired
    private AuthService authService;

    @Test
    void twoConcurrentRefreshCallsShouldAllowSingleUseOnly() throws Exception {
        String email = "concurrent-" + System.nanoTime() + "@test.com";
        String password = "secret-123";

        authService.register(email, password);
        AuthResponse login = authService.login(email, password);
        String sameRefreshToken = login.refreshToken();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        authService.refresh(new RefreshRequest(sameRefreshToken));
                        return true;
                    } catch (IllegalArgumentException ex) {
                        return false;
                    }
                }));
            }

            ready.await();
            go.countDown();

            int successCount = 0;
            int failureCount = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }

            assertEquals(1, successCount);
            assertEquals(1, failureCount);
        } finally {
            executor.shutdownNow();
        }
    }
}
