package cz.tacr.elza.service.da;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.da.ApiException;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaSyncQueueItem;

/**
 * How a failed exchange with the DA is to be treated.
 *
 * The DA API defines 403 and 404 returned for a batch (its status or result) as a permanent
 * failure of the request - the DA stopped working on it. Anything else (no connection, a server
 * error, 503 for a result asked for too early) may pass, so the exchange is tried again later.
 */
final class DaRequestFailure {

    private static final List<Integer> PERMANENT_CODES = List.of(403, 404);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** How much of an answer that is not an ErrorInfo is quoted in the description. */
    private static final int MAX_BODY_LENGTH = 300;

    private static final int MAX_LOGGED_CODES = 5;

    private DaRequestFailure() {
    }

    static boolean isPermanent(Throwable failure) {
        ApiException apiException = apiException(failure);
        return apiException != null && PERMANENT_CODES.contains(apiException.getCode());
    }

    /**
     * Whether the DA answered with an HTTP status. Such a failure is fully described by the
     * answer; a stack trace of it says nothing more.
     */
    static boolean isHttpAnswer(Throwable failure) {
        ApiException apiException = apiException(failure);
        return apiException != null && apiException.getCode() > 0;
    }

    /**
     * Logs a failed exchange about the queue items after the failure was recorded on them, so
     * the line carries the attempt and when the next one is due. A failure the DA answered is
     * described by its answer alone; the stack trace is logged only for one it did not answer.
     *
     * @param what what failed, e.g. "download batch 1234 could not be obtained"
     */
    static void log(Logger logger, ArrDigitalRepository repository, String what, List<DaSyncQueueItem> items,
                    Throwable failure) {
        DaSyncQueueItem first = items.get(0);
        OffsetDateTime next = first.getNextAttemptAt();
        String message = "DA " + repository.getCode() + ": " + what + " (" + codes(items) + "), attempt "
                + first.getAttemptCount()
                + (next == null ? "" : ", next attempt at " + next.toLocalTime().truncatedTo(ChronoUnit.SECONDS))
                + ": " + describe(failure);
        if (isHttpAnswer(failure)) {
            logger.error(message);
        } else {
            logger.error(message, failure);
        }
    }

    /** AIP codes of the items, the first few of a long list. */
    private static String codes(List<DaSyncQueueItem> items) {
        String codes = items.stream().limit(MAX_LOGGED_CODES).map(DaSyncQueueItem::getCode)
                .collect(Collectors.joining(", "));
        return items.size() > MAX_LOGGED_CODES
                ? codes + " and " + (items.size() - MAX_LOGGED_CODES) + " more"
                : codes;
    }

    /**
     * @return the failure as ELZA described it for the user when it did, else what the DA said
     *         about it (ErrorInfo of its answer, or the status and the beginning of the answer),
     *         else the reason of the failure
     */
    static String describe(Throwable failure) {
        AipProblemException described = AipProblem.describedCause(failure);
        if (described != null) {
            return described.getMessage();
        }
        ApiException apiException = apiException(failure);
        if (apiException != null) {
            String errorInfo = errorInfo(apiException.getResponseBody());
            if (errorInfo != null) {
                return errorInfo + " (HTTP " + apiException.getCode() + ")";
            }
            if (apiException.getCode() > 0) {
                StringBuilder sb = new StringBuilder("HTTP ").append(apiException.getCode());
                String status = statusText(apiException);
                if (StringUtils.isNotBlank(status)) {
                    sb.append(' ').append(status);
                }
                if (StringUtils.isNotBlank(apiException.getResponseBody())) {
                    sb.append(": ").append(StringUtils.abbreviate(
                            StringUtils.normalizeSpace(apiException.getResponseBody()), MAX_BODY_LENGTH));
                }
                return sb.toString();
            }
        }
        return AipProblem.reason(failure);
    }

    /** "errorTitle: errorDetail [errorCode]" of an ErrorInfo of the DA API, or null. */
    @Nullable
    static String errorInfo(@Nullable String body) {
        if (StringUtils.isBlank(body)) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(body);
            return errorInfo(text(node, "errorTitle"), text(node, "errorDetail"), text(node, "errorCode"));
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    static String errorInfo(@Nullable String title, @Nullable String detail, @Nullable String code) {
        if (StringUtils.isAllBlank(title, detail)) {
            return null;
        }
        StringBuilder sb = new StringBuilder(StringUtils.defaultIfBlank(title, detail));
        if (StringUtils.isNotBlank(title) && StringUtils.isNotBlank(detail)) {
            sb.append(": ").append(detail);
        }
        if (StringUtils.isNotBlank(code)) {
            sb.append(" [").append(code).append("]");
        }
        return sb.toString();
    }

    @Nullable
    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * The HTTP status text. The generated client formats the whole response (body and headers)
     * into {@link ApiException#getMessage()}; the text it was created with is its first line.
     * Some constructors of the client fill that text with the status code and the body itself
     * ("Response Code: 500 Response Body: ...") or leave it null; such a text says nothing the
     * description does not already say and is left out.
     */
    @Nullable
    private static String statusText(ApiException apiException) {
        String message = apiException.getMessage();
        if (message == null) {
            return null;
        }
        String firstLine = message.lines().findFirst().orElse("");
        String status = StringUtils.removeStart(firstLine, "Message: ").strip();
        if ("null".equals(status) || status.startsWith("Response Code:")
                || status.equals("HTTP " + apiException.getCode())) {
            return null;
        }
        return status;
    }

    @Nullable
    private static ApiException apiException(Throwable failure) {
        for (Throwable cause : ExceptionUtils.getThrowableList(failure)) {
            if (cause instanceof ApiException apiException) {
                return apiException;
            }
        }
        return null;
    }
}
