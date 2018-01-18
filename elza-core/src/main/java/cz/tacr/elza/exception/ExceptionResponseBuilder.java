package cz.tacr.elza.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ErrorCode;

public class ExceptionResponseBuilder {

    private final Throwable source;

    private ErrorCode errorCode;

    private Level level;

    private Map<String, Object> properties;

    private ExceptionResponseBuilder(Throwable source) {
        this.source = Validate.notNull(source);
    }

    public ExceptionResponseBuilder setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public ExceptionResponseBuilder setLevel(Level level) {
        this.level = level;
        return this;
    }

    public ExceptionResponseBuilder setProperties(Map<String, Object> properties) {
        this.properties = properties;
        return this;
    }

    public ExceptionResponse build() {
        StringWriter sw = new StringWriter();
        source.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        ExceptionResponse er = new ExceptionResponse(
                errorCode.getType(),
                errorCode.getCode(),
                source.getMessage(),
                level.getLevel(),
                properties,
                stackTrace);

        return er;
    }

    public void logError(Logger logger) {
        StringBuilder sb = new StringBuilder("Exception type:")
                .append(errorCode.getType())
                .append(", code:")
                .append(errorCode.getCode())
                .append(", message:")
                .append(source.getMessage());

        if (level != null) {
            sb.append(", level:").append(level.getLevel());
        }

        if (properties != null && properties.size() > 0) {
            sb.append(",\nproperties:{");
            properties.forEach((k, v) -> {
                sb.append(k).append('=').append(v).append(';');
            });
            sb.setLength(sb.length() - 1); // remove last ;
            sb.append('}');
        }

        logger.error(sb.toString(), source);
    }

    public static ExceptionResponseBuilder createFrom(Throwable t) {
        if (t instanceof AbstractException) {
            return createFrom((AbstractException) t);
        }
        return new ExceptionResponseBuilder(t)
                .setErrorCode(BaseCode.SYSTEM_ERROR)
                .setLevel(Level.DANGER);
    }

    public static ExceptionResponseBuilder createFrom(AbstractException e) {
        return new ExceptionResponseBuilder(e)
                .setErrorCode(e.getErrorCode())
                .setLevel(e.getLevel())
                .setProperties(e.getProperties());
    }
}
