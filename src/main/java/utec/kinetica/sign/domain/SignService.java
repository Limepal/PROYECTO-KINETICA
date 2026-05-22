package utec.kinetica.sign.domain;

import org.springframework.stereotype.Service;
import utec.kinetica.common.domain.exception.ResourceNotFoundException;
import utec.kinetica.sign.infrastructure.SignRepository;

import java.util.List;
import java.util.Locale;

@Service
public class SignService {
    private final SignRepository repository;

    public SignService(SignRepository repository) {
        this.repository = repository;
    }

    public List<Sign> list() {
        return repository.findAll();
    }

    public Sign create(String label, String mediaRef, String locale, Boolean active) {
        Sign sign = new Sign();
        sign.setLabel(label);
        sign.setNormalizedLabel(normalizeLabel(label));
        sign.setMediaRef(mediaRef);
        sign.setLocale(locale);
        sign.setActive(active == null || active);
        return repository.save(sign);
    }

    public Sign getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sign not found: " + id));
    }

    public Sign update(Long id, String label, String mediaRef, String locale, Boolean active) {
        Sign sign = getById(id);
        sign.setLabel(label);
        sign.setNormalizedLabel(normalizeLabel(label));
        sign.setMediaRef(mediaRef);
        sign.setLocale(locale);
        sign.setActive(active == null || active);
        return repository.save(sign);
    }

    public void delete(Long id) {
        Sign sign = getById(id);
        repository.delete(sign);
    }

    private String normalizeLabel(String label) {
        return label.trim().toLowerCase(Locale.ROOT);
    }

}
