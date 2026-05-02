package utec.kinetica.translation.domain;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utec.kinetica.translation.infrastructure.MediaAssetRepository;
import utec.kinetica.translation.infrastructure.TranslationRequestRepository;

@Service
public class MediaAssetService {
    private final MediaAssetRepository mediaAssetRepository;
    private final TranslationRequestRepository requestRepository;

    public MediaAssetService(MediaAssetRepository mediaAssetRepository, TranslationRequestRepository requestRepository) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional
    public MediaAsset create(Long requestId, Long userId, MediaAssetKind kind, String storageUrl, String mimeType, Long durationMs, Long sizeBytes) {
        TranslationRequest request = requestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Translation request not found: " + requestId));
        MediaAsset mediaAsset = new MediaAsset();
        mediaAsset.setRequest(request);
        mediaAsset.setKind(kind);
        mediaAsset.setStorageUrl(storageUrl);
        mediaAsset.setMimeType(mimeType);
        mediaAsset.setDurationMs(durationMs);
        mediaAsset.setSizeBytes(sizeBytes);
        return mediaAssetRepository.save(mediaAsset);
    }

    @Transactional(readOnly = true)
    public java.util.List<MediaAsset> listByRequest(Long requestId, Long userId) {
        requestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Translation request not found: " + requestId));
        return mediaAssetRepository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public MediaAsset getById(Long requestId, Long userId, Long id) {
        requestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Translation request not found: " + requestId));
        MediaAsset mediaAsset = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Media asset not found: " + id));
        if (!mediaAsset.getRequest().getId().equals(requestId)) {
            throw new EntityNotFoundException("Media asset not found for request: " + requestId);
        }
        return mediaAsset;
    }

    @Transactional
    public void delete(Long requestId, Long userId, Long id) {
        MediaAsset mediaAsset = getById(requestId, userId, id);
        mediaAssetRepository.delete(mediaAsset);
    }
}
