package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.ErrorCode;

import java.util.HashMap;
import java.util.Map;

/**
 * Výchozí výjimka serveru.
 *
 * @author Martin Šlapa
 * @since 09.11.2016
 */
public abstract class AbstractException extends RuntimeException {

    private ErrorCode errorCode;

    private Map<String, Object> properties;

    public AbstractException(final Throwable cause, final ErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public AbstractException(final String message, final ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public AbstractException(final String message, final Throwable cause, final ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public AbstractException(final ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public AbstractException set(final String key, final Object value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
        return this;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(errorCode.getType()).append(":").append(errorCode.getCode());
        if (properties != null) {
            sb.append(", properties [");
            boolean first = true;
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            sb.append("]");
        }
        return sb.toString();
    }
}