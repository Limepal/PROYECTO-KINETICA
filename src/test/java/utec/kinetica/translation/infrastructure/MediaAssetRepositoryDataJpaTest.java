package utec.kinetica.translation.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import utec.kinetica.auth.domain.User;
import utec.kinetica.auth.infrastructure.UserRepository;
import utec.kinetica.support.PostgresContainerSupport;
import utec.kinetica.translation.domain.MediaAsset;
import utec.kinetica.translation.domain.MediaAssetKind;
import utec.kinetica.translation.domain.TranslationDirection;
import utec.kinetica.translation.domain.TranslationRequest;
import utec.kinetica.translation.domain.TranslationStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MediaAssetRepositoryDataJpaTest extends PostgresContainerSupport {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TranslationRequestRepository translationRequestRepository;
    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Test
    void shouldFindByRequestIdAndExpiredWindowWhenAssetsMatch() {
        User user = new User();
        user.setEmail("media-user@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        TranslationRequest request = new TranslationRequest();
        request.setUser(user);
        request.setDirection(TranslationDirection.SIGN_TO_TEXT);
        request.setStatus(TranslationStatus.DONE);
        request.setSourceText("hola");
        request = translationRequestRepository.save(request);

        MediaAsset expired = new MediaAsset();
        expired.setRequest(request);
        expired.setKind(MediaAssetKind.VIDEO);
        expired.setStorageUrl("/tmp/one.mp4");
        expired.setMimeType("video/mp4");
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        mediaAssetRepository.save(expired);

        assertEquals(1, mediaAssetRepository.findByRequestId(request.getId()).size());
        assertEquals(1, mediaAssetRepository.findTop200ByExpiresAtBeforeOrderByExpiresAtAsc(Instant.now()).size());
    }

    @Test
    void shouldReturnEmptyExpiredWindowWhenAssetHasFutureExpiry() {
        User user = new User();
        user.setEmail("media-future@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        TranslationRequest request = new TranslationRequest();
        request.setUser(user);
        request.setDirection(TranslationDirection.SIGN_TO_TEXT);
        request.setStatus(TranslationStatus.DONE);
        request.setSourceText("hola");
        request = translationRequestRepository.save(request);

        MediaAsset future = new MediaAsset();
        future.setRequest(request);
        future.setKind(MediaAssetKind.VIDEO);
        future.setStorageUrl("/tmp/future.mp4");
        future.setMimeType("video/mp4");
        future.setExpiresAt(Instant.now().plusSeconds(600));
        mediaAssetRepository.save(future);

        assertEquals(0, mediaAssetRepository.findTop200ByExpiresAtBeforeOrderByExpiresAtAsc(Instant.now()).size());
    }
}
