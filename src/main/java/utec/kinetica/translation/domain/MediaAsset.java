package utec.kinetica.translation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "media_assets")
public class MediaAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private TranslationRequest request;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaAssetKind kind;

    @Column(nullable = false)
    private String storageUrl;

    @Column(nullable = false)
    private String mimeType;

    private Long durationMs;
    private Long sizeBytes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TranslationRequest getRequest() { return request; }
    public void setRequest(TranslationRequest request) { this.request = request; }
    public MediaAssetKind getKind() { return kind; }
    public void setKind(MediaAssetKind kind) { this.kind = kind; }
    public String getStorageUrl() { return storageUrl; }
    public void setStorageUrl(String storageUrl) { this.storageUrl = storageUrl; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
}
