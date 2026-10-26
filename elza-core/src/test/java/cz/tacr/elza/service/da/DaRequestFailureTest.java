package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cz.tacr.da.ApiException;
import cz.tacr.elza.api.AipProblemType;

/**
 * A failure of an exchange with the DA is described by what the DA answered, never by the
 * dump of the HTTP response its client puts into the exception message.
 */
class DaRequestFailureTest {

    private static final Map<String, List<String>> HEADERS = Map.of("via", List.of("1.1 Caddy"));

    @Test
    void anErrorInfoAnswerIsDescribedByItsTitleDetailAndCode() {
        ApiException answer = new ApiException("Internal Server Error", 500, HEADERS,
                "{\"errorTitle\":\"Batch preparation failed\",\"errorCode\":\"INTERNAL_ERROR\","
                        + "\"errorDetail\":\"Package p1: chybí content i FONDS_ID.\"}");

        assertEquals("Batch preparation failed: Package p1: chybí content i FONDS_ID. [INTERNAL_ERROR] (HTTP 500)",
                     DaRequestFailure.describe(new IllegalStateException("Došlo k chybě při volání DA", answer)));
        assertTrue(DaRequestFailure.isHttpAnswer(answer));
    }

    @Test
    void anotherAnswerIsDescribedByItsStatusAndBeginningWithoutHeaders() {
        ApiException answer = new ApiException("Bad Gateway", 502, HEADERS, "<html>\n<body>upstream down</body>\n</html>");

        String description = DaRequestFailure.describe(answer);

        assertEquals("HTTP 502 Bad Gateway: <html> <body>upstream down</body> </html>", description);
        assertFalse(description.contains("Caddy"));
    }

    /** The client's own "Response Code: ... Response Body: ..." text would only repeat the answer. */
    @Test
    void theAnswerIsNotRepeatedByTheTextTheClientMadeOfIt() {
        ApiException answer = new ApiException(500, HEADERS, "boom");

        assertEquals("HTTP 500: boom", DaRequestFailure.describe(answer));
    }

    @Test
    void aFailureWithoutAnswerIsNotAnHttpAnswer() {
        ApiException noConnection = new ApiException(new java.net.ConnectException("Connection refused"));

        assertFalse(DaRequestFailure.isHttpAnswer(noConnection));
        assertTrue(DaRequestFailure.describe(noConnection).contains("Connection refused"));
    }

    /** What ELZA wrote for the user (e.g. switch to File Transfer) wins over the answer behind it. */
    @Test
    void aFailureDescribedForTheUserKeepsItsDescription() {
        ApiException tooLarge = new ApiException("Payload Too Large", 413, HEADERS, null);
        AipProblemException described = new AipProblemException(AipProblemType.DOWNLOAD_ERROR,
                "přepněte způsob stahování na File Transfer", null, tooLarge);

        assertEquals("přepněte způsob stahování na File Transfer", DaRequestFailure.describe(described));
    }

    @Test
    void aDownloadFailureSaysWhatTheDaAnswered() {
        ApiException answer = new ApiException("Internal Server Error", 500, HEADERS,
                "{\"errorTitle\":\"Batch preparation failed\",\"errorCode\":\"INTERNAL_ERROR\"}");

        AipProblem problem = AipProblem.downloadFailure(answer);

        assertEquals("Balíček se nepodařilo stáhnout z digitálního archivu: Batch preparation failed"
                + " [INTERNAL_ERROR] (HTTP 500). Stažení bude opakováno.", problem.description());
    }
}
