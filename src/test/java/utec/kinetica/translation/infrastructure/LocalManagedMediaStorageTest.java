package utec.kinetica.translation.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import utec.kinetica.translation.domain.MediaAssetKind;
import utec.kinetica.translation.domain.StoredMediaFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalManagedMediaStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreAndDeleteManagedFileWhenLifecycleCompletes() {
        LocalManagedMediaStorage storage = new LocalManagedMediaStorage(tempDir.toString());

        byte[] content = new byte[]{9, 8, 7, 6};
        StoredMediaFile stored = storage.store(22L, MediaAssetKind.IMAGE, "image/png", content, "photo.png");

        assertTrue(stored.storageUrl().startsWith("media://local/"));
        assertEquals(4L, stored.sizeBytes());

        String relative = stored.storageUrl().substring("media://local/".length());
        Path storedPath = tempDir.resolve(relative);
        assertTrue(Files.exists(storedPath));

        storage.deleteIfManaged(stored.storageUrl());
        assertTrue(Files.notExists(storedPath));
    }
}
