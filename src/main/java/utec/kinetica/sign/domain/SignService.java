package utec.kinetica.sign.domain;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utec.kinetica.sign.infrastructure.SignRepository;

import java.util.List;



@Service
@RequiredArgsConstructor
public class SignService {
    private final SignRepository repository;

    public List<Sign> list() {
        return repository.findAll();
    }

    public void save(Sign sign) {
        repository.save(sign);
    }


}
