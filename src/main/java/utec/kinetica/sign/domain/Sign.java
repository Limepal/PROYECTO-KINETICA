package utec.kinetica.sign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Table(
        name = "signs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_signs_normalized_label_locale", columnNames = {"normalized_label", "locale"})
        },
        indexes = {
                @Index(name = "idx_signs_locale_active", columnList = "locale, active")
        }
)
@Entity
public class Sign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String label;

    @Column(name = "normalized_label", nullable = false, length = 150)
    private String normalizedLabel;

    @Column(nullable = false, length = 512)
    private String mediaRef;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false)
    private boolean active;

    public Sign() {
    }

    public Sign(Long id, String label, String normalizedLabel, String mediaRef, String locale, boolean active) {
        this.id = id;
        this.label = label;
        this.normalizedLabel = normalizedLabel;
        this.mediaRef = mediaRef;
        this.locale = locale;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getNormalizedLabel() {
        return normalizedLabel;
    }

    public void setNormalizedLabel(String normalizedLabel) {
        this.normalizedLabel = normalizedLabel;
    }

    public String getMediaRef() {
        return mediaRef;
    }

    public void setMediaRef(String mediaRef) {
        this.mediaRef = mediaRef;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

}
