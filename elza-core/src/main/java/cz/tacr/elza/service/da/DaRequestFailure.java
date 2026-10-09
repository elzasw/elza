package cz.tacr.elza.service.da;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.da.ApiException;

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

    private DaRequestFailure() {
    }

    static boolean isPermanent(Throwable failure) {
        ApiException apiException = apiException(failure);
        return apiException != null && PERMANENT_CODES.contains(apiException.getCode());
    }

    /**
     * @return what the DA said about the failure (ErrorInfo of its answer), or the reason of the
     *         failure when the DA did not answer with one
     */
    static String describe(Throwable failure) {
        ApiException apiException = apiException(failure);
        if (apiException != null) {
            String errorInfo = errorInfo(apiException.getResponseBody());
            if (errorInfo != null) {
                return errorInfo + " (HTTP " + apiException.getCode() + ")";
            }
            if (apiException.getCode() > 0) {
                return "HTTP " + apiException.getCode() + " " + StringUtils.defaultString(apiException.getMessage());
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
