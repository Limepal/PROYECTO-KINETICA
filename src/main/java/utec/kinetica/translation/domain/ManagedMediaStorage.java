package utec.kinetica.translation.domain;

public interface ManagedMediaStorage {
    StoredMediaFile store(Long requestId, MediaAssetKind kind, String mimeType, byte[] content, String originalFilename);
    void deleteIfManaged(String storageUrl);
}
