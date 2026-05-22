package utec.kinetica.translation.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import utec.kinetica.translation.domain.TranslationKpiService;
import utec.kinetica.translation.domain.TranslationKpiSummary;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TranslationKpiControllerTest {

    @Test
    void shouldReturnKpiSummaryWhenRequested() {
        TranslationKpiService service = mock(TranslationKpiService.class);
        TranslationKpiController controller = new TranslationKpiController(service);

        TranslationKpiSummary summary = new TranslationKpiSummary(
                Instant.now().minusSeconds(3600),
                Instant.now(),
                10,
                8,
                2,
                80.0,
                120.0,
                180L,
                0.88,
                10.0,
                1
        );

        when(service.summarizeLastDays(7)).thenReturn(summary);

        var response = controller.getSummary(7);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10, response.getBody().totalRequests());
        assertEquals(8, response.getBody().completedRequests());
    }
}
