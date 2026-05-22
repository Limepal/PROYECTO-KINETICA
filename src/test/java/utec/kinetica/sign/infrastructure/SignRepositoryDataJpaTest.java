package utec.kinetica.sign.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import utec.kinetica.sign.domain.Sign;
import utec.kinetica.support.PostgresContainerSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SignRepositoryDataJpaTest extends PostgresContainerSupport {

    @Autowired
    private SignRepository signRepository;

    @Test
    void shouldPersistAndLoadSignWhenValidDataIsProvided() {
        Sign sign = new Sign();
        sign.setLabel("hola");
        sign.setNormalizedLabel("hola");
        sign.setMediaRef("media://hola");
        sign.setLocale("es-PE");
        sign.setActive(true);
        sign = signRepository.save(sign);

        assertEquals("hola", signRepository.findById(sign.getId()).orElseThrow().getLabel());
    }

    @Test
    void shouldDeleteSignWhenSignExists() {
        Sign sign = new Sign();
        sign.setLabel("chau");
        sign.setNormalizedLabel("chau");
        sign.setMediaRef("media://chau");
        sign.setLocale("es-PE");
        sign.setActive(true);
        sign = signRepository.save(sign);

        signRepository.deleteById(sign.getId());

        assertEquals(true, signRepository.findById(sign.getId()).isEmpty());
    }
}
