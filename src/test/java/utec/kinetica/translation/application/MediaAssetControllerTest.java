package utec.kinetica.translation.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import utec.kinetica.translation.application.dto.CreateMediaAssetRequest;
import utec.kinetica.translation.domain.MediaAsset;
import utec.kinetica.translation.domain.MediaAssetKind;
import utec.kinetica.translation.domain.MediaAssetService;
import utec.kinetica.translation.domain.TranslationRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaAssetControllerTest {

    @Test
    void shouldHandleMediaEndpointsWhenCalled() throws Exception {
        MediaAssetService service = mock(MediaAssetService.class);
        MediaAssetController controller = new MediaAssetController(service);
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("8").build();

        MediaAsset media = buildMedia(31L, 12L, MediaAssetKind.VIDEO);
        when(service.create(12L, 8L, MediaAssetKind.VIDEO, "s3://video", "video/mp4", 1200L, 3333L)).thenReturn(media);
        when(service.createManagedUpload(12L, 8L, MediaAssetKind.VIDEO, "video/mp4", "abc".getBytes(), "clip.mp4", 1200L)).thenReturn(media);
        when(service.listByRequest(12L, 8L)).thenReturn(List.of(media));
        when(service.getById(12L, 8L, 31L)).thenReturn(media);

        var created = controller.create(jwt, 12L, new CreateMediaAssetRequest(MediaAssetKind.VIDEO, "s3://video", "video/mp4", 1200L, 3333L));
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getHeaders().getLocation());

        var file = new MockMultipartFile("file", "clip.mp4", "video/mp4", "abc".getBytes());
        var uploaded = controller.upload(jwt, 12L, MediaAssetKind.VIDEO, file, 1200L);
        assertEquals(HttpStatus.CREATED, uploaded.getStatusCode());

        var listed = controller.list(jwt, 12L);
        assertEquals(1, listed.getBody().size());

        var byId = controller.getById(jwt, 12L, 31L);
        assertEquals(31L, byId.getBody().id());

        var deleted = controller.delete(jwt, 12L, 31L);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        verify(service).delete(12L, 8L, 31L);
    }

    private static MediaAsset buildMedia(Long id, Long requestId, MediaAssetKind kind) {
        TranslationRequest req = new TranslationRequest();
        req.setId(requestId);
        MediaAsset media = new MediaAsset();
        media.setId(id);
        media.setRequest(req);
        media.setKind(kind);
        media.setStorageUrl("s3://video");
        media.setMimeType("video/mp4");
        return media;
    }
}
