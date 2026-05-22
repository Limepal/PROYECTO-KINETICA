package utec.kinetica;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import utec.kinetica.support.PostgresContainerSupport;

@SpringBootTest
class KineticaApplicationTests extends PostgresContainerSupport {

    @Test
    void shouldLoadContextWhenApplicationStarts() {
    }

}
