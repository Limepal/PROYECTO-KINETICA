package utec.kinetica.translation.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utec.kinetica.translation.domain.ManagedMediaStorage;
import utec.kinetica.translation.domain.MediaAssetKind;
import utec.kinetica.translation.domain.StoredMediaFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Component
public class LocalManagedMediaStorage implements ManagedMediaStorage {
    private static final String MANAGED_PREFIX = "media://local/";
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalManagedMediaStorage.class);

    private final Path rootPath;

    public LocalManagedMediaStorage(@Value("${kinetica.media.storage.local-root:./storage/media}") String rootPath) {
        this.rootPath = Path.of(rootPath).toAbsolutePath().normalize();
    }

    @Override
    public StoredMediaFile store(Long requestId, MediaAssetKind kind, String mimeType, byte[] content, String originalFilename) {
        try {
            String extension = resolveExtension(mimeType, originalFilename);
            String fileName = Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + extension;
            String kindFolder = kind.name().toLowerCase(Locale.ROOT);
            Path relativePath = Path.of("request-" + requestId, kindFolder, fileName);
            Path absolutePath = rootPath.resolve(relativePath).normalize();
            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, content);
            return new StoredMediaFile(MANAGED_PREFIX + toStoragePath(relativePath), content.length);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store media file", ex);
        }
    }

    @Override
    public void deleteIfManaged(String storageUrl) {
        if (storageUrl == null || !storageUrl.startsWith(MANAGED_PREFIX)) {
            return;
        }
        String relative = storageUrl.substring(MANAGED_PREFIX.length());
        Path path = rootPath.resolve(Path.of(relative)).normalize();
        if (!path.startsWith(rootPath)) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            LOGGER.warn("Unable to delete managed media file {}", storageUrl);
        }
    }

    private String toStoragePath(Path relativePath) {
        return relativePath.toString().replace('\\', '/');
    }

    private String resolveExtension(String mimeType, String originalFilename) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                return originalFilename.substring(dot);
            }
        }
        return switch (mimeType) {
            case "video/mp4" -> ".mp4";
            case "audio/mpeg" -> ".mp3";
            case "audio/wav" -> ".wav";
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            default -> ".bin";
        };
    }
}
