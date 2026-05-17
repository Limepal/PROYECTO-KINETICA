package utec.kinetica.translation.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MediaRetentionScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaRetentionScheduler.class);

    private final MediaAssetService mediaAssetService;

    public MediaRetentionScheduler(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    @Scheduled(fixedDelayString = "${kinetica.media.retention-scan-ms:3600000}")
    public void purgeExpired() {
        int removed = mediaAssetService.purgeExpiredAssets();
        if (removed > 0) {
            LOGGER.info("Purged {} expired media assets", removed);
        }
    }
}
