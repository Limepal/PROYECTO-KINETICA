package utec.kinetica.translation.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.common.domain.exception.ResourceNotFoundException;
import utec.kinetica.translation.infrastructure.MediaAssetRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;

import java.time.Instant;

@Service
public class MediaAssetService {
    private final MediaAssetRepository mediaAssetRepository;
    private final TranslationRequestRepository requestRepository;
    private final ManagedMediaStorage managedMediaStorage;
    private final ApplicationEventPublisher eventPublisher;
    private final int retentionDays;

    public MediaAssetService(
            MediaAssetRepository mediaAssetRepository,
            TranslationRequestRepository requestRepository,
            ManagedMediaStorage managedMediaStorage,
            ApplicationEventPublisher eventPublisher,
            @Value("${kinetica.media.retention-days:30}") int retentionDays
    ) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.requestRepository = requestRepository;
        this.managedMediaStorage = managedMediaStorage;
        this.eventPublisher = eventPublisher;
        this.retentionDays = retentionDays;
    }

    @Transactional
    public MediaAsset create(Long requestId, Long userId, MediaAssetKind kind, String storageUrl, String mimeType, Long durationMs, Long sizeBytes) {
        TranslationRequest request = requestRepository.findByIdAndUser_Id(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Translation request not found: " + requestId));
        return createAsset(request, kind, storageUrl, mimeType, durationMs, sizeBytes);
    }

    @Transactional
    public MediaAsset createManagedUpload(
            Long requestId,
            Long userId,
            MediaAssetKind kind,
            String mimeType,
            byte[] content,
            String originalFilename,
            Long durationMs
    ) {
        TranslationRequest request = requestRepository.findByIdAndUser_Id(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Translation request not found: " + requestId));
        StoredMediaFile storedFile = managedMediaStorage.store(requestId, kind, mimeType, content, originalFilename);
        return createAsset(request, kind, storedFile.storageUrl(), mimeType, durationMs, storedFile.sizeBytes());
    }

    @Transactional(readOnly = true)
    public java.util.List<MediaAsset> listByRequest(Long requestId, Long userId) {
        requestRepository.findByIdAndUser_Id(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Translation request not found: " + requestId));
        return mediaAssetRepository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public MediaAsset getById(Long requestId, Long userId, Long id) {
        requestRepository.findByIdAndUser_Id(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Translation request not found: " + requestId));
        MediaAsset mediaAsset = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + id));
        if (!mediaAsset.getRequest().getId().equals(requestId)) {
            throw new ResourceNotFoundException("Media asset not found for request: " + requestId);
        }
        return mediaAsset;
    }

    @Transactional
    public void delete(Long requestId, Long userId, Long id) {
        MediaAsset mediaAsset = getById(requestId, userId, id);
        mediaAssetRepository.delete(mediaAsset);
        managedMediaStorage.deleteIfManaged(mediaAsset.getStorageUrl());
    }

    @Transactional
    public int purgeExpiredAssets() {
        java.util.List<MediaAsset> expired = mediaAssetRepository.findTop200ByExpiresAtBeforeOrderByExpiresAtAsc(Instant.now());
        for (MediaAsset asset : expired) {
            mediaAssetRepository.delete(asset);
            managedMediaStorage.deleteIfManaged(asset.getStorageUrl());
        }
        return expired.size();
    }

    private MediaAsset createAsset(
            TranslationRequest request,
            MediaAssetKind kind,
            String storageUrl,
            String mimeType,
            Long durationMs,
            Long sizeBytes
    ) {
        MediaAsset mediaAsset = new MediaAsset();
        mediaAsset.setRequest(request);
        mediaAsset.setKind(kind);
        mediaAsset.setStorageUrl(storageUrl);
        mediaAsset.setMimeType(mimeType);
        mediaAsset.setDurationMs(durationMs);
        mediaAsset.setSizeBytes(sizeBytes);
        mediaAsset.setExpiresAt(Instant.now().plusSeconds(retentionDays * 86400L));
        MediaAsset saved = mediaAssetRepository.save(mediaAsset);
        eventPublisher.publishEvent(new MediaAssetUploadedEvent(request.getId(), saved.getId()));
        return saved;
    }
}
