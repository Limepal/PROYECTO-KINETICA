package utec.kinetica.sign.domain;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import utec.kinetica.sign.infrastructure.SignRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignServiceTest {

    @Test
    void shouldNormalizeLabelWhenCreatingSign() {
        SignRepository repository = mock(SignRepository.class);
        SignService service = new SignService(repository);

        Sign persisted = new Sign();
        persisted.setId(10L);
        persisted.setLabel("Hola");
        persisted.setNormalizedLabel("hola");
        persisted.setMediaRef("sign://hola");
        persisted.setLocale("es-PE");
        persisted.setActive(true);
        when(repository.save(org.mockito.ArgumentMatchers.any(Sign.class))).thenReturn(persisted);

        service.create(" Hola ", "sign://hola", "es-PE", true);

        ArgumentCaptor<Sign> captor = ArgumentCaptor.forClass(Sign.class);
        verify(repository).save(captor.capture());
        assertEquals("hola", captor.getValue().getNormalizedLabel());
    }
}
