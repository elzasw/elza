package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cz.tacr.da.ApiException;
import cz.tacr.elza.domain.ArrDigitalRepository;

/** Retry delays, the idle wait of the processors and how a failure of the DA is classified. */
public class DaQueueTimingTest {

    private static ArrDigitalRepository repository(int pollSeconds) {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setStatusPollInterval(pollSeconds);
        return repository;
    }

    @Test
    void theRetryDelayDoublesFromTheStatusIntervalUpToTheCap() {
        ArrDigitalRepository repository = repository(2);
        assertEquals(Duration.ofSeconds(2), DaService.retryDelay(repository, 1));
        assertEquals(Duration.ofSeconds(4), DaService.retryDelay(repository, 2));
        assertEquals(Duration.ofSeconds(8), DaService.retryDelay(repository, 3));
        assertEquals(DaService.MAX_RETRY_DELAY, DaService.retryDelay(repository, 9));
        assertEquals(DaService.MAX_RETRY_DELAY, DaService.retryDelay(repository, 1000));
    }

    @Test
    void anIdleProcessorSleepsUntilTheEarliestItemIsDue() {
        assertEquals(10_000, DaService.idleMillis(null, 10_000));
        assertEquals(10_000, DaService.idleMillis(OffsetDateTime.now().plusMinutes(1), 10_000));
        long untilDue = DaService.idleMillis(OffsetDateTime.now().plusSeconds(2), 10_000);
        assertTrue(untilDue > 1_500 && untilDue <= 2_000, String.valueOf(untilDue));
        assertEquals(50, DaService.idleMillis(OffsetDateTime.now().minusSeconds(5), 10_000));
    }

    @Test
    void only403And404AreAPermanentFailureOfABatch() {
        assertTrue(DaRequestFailure.isPermanent(wrapped(404, null)));
        assertTrue(DaRequestFailure.isPermanent(wrapped(403, null)));
        assertFalse(DaRequestFailure.isPermanent(wrapped(503, null)));
        assertFalse(DaRequestFailure.isPermanent(wrapped(500, null)));
        assertFalse(DaRequestFailure.isPermanent(new IllegalStateException("Connection refused")));
    }

    @Test
    void theDescriptionIsWhatTheDaSaid() {
        String body = "{\"errorTitle\":\"Balíček nenalezen\",\"errorDetail\":\"AIP 42 neexistuje\",\"errorCode\":\"E404\"}";
        assertEquals("Balíček nenalezen: AIP 42 neexistuje [E404] (HTTP 404)", DaRequestFailure.describe(wrapped(404, body)));
        assertEquals("Odmítnuto", DaRequestFailure.errorInfo("Odmítnuto", null, null));
    }

    /** What the connector throws: the ApiException of the client wrapped by DaConnector. */
    private static Exception wrapped(int code, String body) {
        return new IllegalStateException("Došlo k chybě při volání DA",
                                         new ApiException("HTTP " + code, code, Map.of(), body));
    }
}
