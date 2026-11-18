package cz.tacr.elza.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.codes.ErrorCode;

/**
 * Why a value cannot be stored in an item, returned by a check instead of
 * thrown, so that a caller that only shows whether a value is usable can
 * report it without failing its transaction.
 *
 * <p>Carries the same error code and properties as the
 * {@link BusinessException} the write path throws, so the client renders the
 * same message whatever the source of the value.
 *
 * @param message
 *            technical message (log, exception message)
 * @param errorCode
 *            error code rendered by the client
 * @param properties
 *            message parameters
 */
public record ItemValueViolation(String message, ErrorCode errorCode, Map<String, Object> properties) {

    public ItemValueViolation {
        // LinkedHashMap, not Map.copyOf: a property may be null (no spec)
        properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    public BusinessException toException() {
        BusinessException e = new BusinessException(message, errorCode);
        properties.forEach(e::set);
        e.level(Level.WARNING);
        return e;
    }
}
