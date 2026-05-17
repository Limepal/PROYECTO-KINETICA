package utec.kinetica.translation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "translation_results")
public class TranslationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private TranslationRequest request;

    @Column(columnDefinition = "TEXT")
    private String textOutput;

    @Column(columnDefinition = "TEXT")
    private String glossOutput;

    private String signOutputRef;

    private Double confidence;

    private Long latencyMs;

    @Column(nullable = false)
    private String modelVersion;

    private String warning;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TranslationRequest getRequest() { return request; }
    public void setRequest(TranslationRequest request) { this.request = request; }
    public String getTextOutput() { return textOutput; }
    public void setTextOutput(String textOutput) { this.textOutput = textOutput; }
    public String getGlossOutput() { return glossOutput; }
    public void setGlossOutput(String glossOutput) { this.glossOutput = glossOutput; }
    public String getSignOutputRef() { return signOutputRef; }
    public void setSignOutputRef(String signOutputRef) { this.signOutputRef = signOutputRef; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getWarning() { return warning; }
    public void setWarning(String warning) { this.warning = warning; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
