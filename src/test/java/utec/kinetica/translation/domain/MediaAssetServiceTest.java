package utec.kinetica.translation.domain;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import utec.kinetica.translation.infrastructure.MediaAssetRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaAssetServiceTest {

    @Test
    void shouldCreateManagedMediaAndSetExpiryWhenUploadSucceeds() {
        MediaAssetRepository mediaAssetRepository = mock(MediaAssetRepository.class);
        TranslationRequestRepository requestRepository = mock(TranslationRequestRepository.class);
        ManagedMediaStorage managedMediaStorage = mock(ManagedMediaStorage.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        TranslationRequest request = new TranslationRequest();
        request.setId(10L);
        when(requestRepository.findByIdAndUser_Id(10L, 5L)).thenReturn(Optional.of(request));
        when(managedMediaStorage.store(any(), any(), any(), any(), any()))
                .thenReturn(new StoredMediaFile("media://local/request-10/video/a.mp4", 123L));
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaAssetService service = new MediaAssetService(mediaAssetRepository, requestRepository, managedMediaStorage, eventPublisher, 30);

        MediaAsset created = service.createManagedUpload(
                10L,
                5L,
                MediaAssetKind.VIDEO,
                "video/mp4",
                new byte[]{1, 2, 3},
                "clip.mp4",
                2500L
        );

        assertEquals("media://local/request-10/video/a.mp4", created.getStorageUrl());
        assertEquals(123L, created.getSizeBytes());
        assertNotNull(created.getExpiresAt());
        assertTrue(created.getExpiresAt().isAfter(Instant.now().plusSeconds(29L * 86400L)));
    }

    @Test
    void shouldPurgeExpiredManagedAssetsWhenPurgeRuns() {
        MediaAssetRepository mediaAssetRepository = mock(MediaAssetRepository.class);
        TranslationRequestRepository requestRepository = mock(TranslationRequestRepository.class);
        ManagedMediaStorage managedMediaStorage = mock(ManagedMediaStorage.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        MediaAsset first = new MediaAsset();
        first.setStorageUrl("media://local/request-1/video/one.mp4");
        MediaAsset second = new MediaAsset();
        second.setStorageUrl("https://external.cdn/file.mp4");

        when(mediaAssetRepository.findTop200ByExpiresAtBeforeOrderByExpiresAtAsc(any(Instant.class)))
                .thenReturn(List.of(first, second));

        MediaAssetService service = new MediaAssetService(mediaAssetRepository, requestRepository, managedMediaStorage, eventPublisher, 30);
        int removed = service.purgeExpiredAssets();

        assertEquals(2, removed);
        verify(mediaAssetRepository, times(2)).delete(any(MediaAsset.class));
        verify(managedMediaStorage).deleteIfManaged("media://local/request-1/video/one.mp4");
        verify(managedMediaStorage).deleteIfManaged("https://external.cdn/file.mp4");
    }
}
