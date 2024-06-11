package cz.tacr.elza.exception;

import java.util.Collection;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.cam.client.ApiException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.ErrorCode;
import cz.tacr.elza.exception.codes.RegistryCode;

/**
 * Pomocná třída pro zjednodušené vytváření vyjímek.
 *
 * @since 24.01.2017
 */
public class ExceptionUtils {

    public static void notNullElseBusiness(final Object object, final ErrorCode code) {
        notNullElseBusiness(object, code.getType() + "-" + code.getCode(), code);
    }

    public static void notNullElseBusiness(final Object object, final String message, final ErrorCode code) {
        if (object == null) {
            throw new BusinessException(message, code);
        }
    }

    public static void nullElseBusiness(final Object object, final ErrorCode code) {
        nullElseBusiness(object, code.getType() + "-" + code.getCode(), code);
    }

    public static void nullElseBusiness(final Object object, final String message, final ErrorCode code) {
        if (object != null) {
            throw new BusinessException(message, code);
        }
    }

    public static void equalsElseBusiness(final Object o1, final Object o2, final ErrorCode code) {
        equalsElseBusiness(o1, o2, code.getType() + "-" + code.getCode(), code);
    }

    public static void equalsElseBusiness(final Object o1, final Object o2, final String message, final ErrorCode code) {
        if (!Objects.equals(o1, o2)) {
            throw new BusinessException(message, code);
        }
    }

    public static void notEqualsElseBusiness(final Object o1, final Object o2, final ErrorCode code) {
        notEqualsElseBusiness(o1, o2, code.getType() + "-" + code.getCode(), code);
    }

    public static void notEqualsElseBusiness(final Object o1, final Object o2, final String message, final ErrorCode code) {
        if (Objects.equals(o1, o2)) {
            throw new BusinessException(message, code);
        }
    }

    public static void notEmptyElseBusiness(final Collection object, final ErrorCode code) {
        notEmptyElseBusiness(object, code.getType() + "-" + code.getCode(), code);
    }
    
    public static void notEmptyElseBusiness(final Collection object, final String message, final ErrorCode code) {
        if (CollectionUtils.isEmpty(object)) {
            throw new BusinessException(message, code);
        }
    }
    
    public static void isEmptyElseBusiness(final Collection object, final ErrorCode code) {
        isEmptyElseBusiness(object, code.getType() + "-" + code.getCode(), code);
    }
    
    public static void isEmptyElseBusiness(final Collection object, final String message, final ErrorCode code) {
        if (CollectionUtils.isNotEmpty(object)) {
            throw new BusinessException(message, code);
        }
    }

    public static String getApiExceptionInfo(ApiException e) {
        StringBuilder sb = new StringBuilder(e.getMessage());
        sb.append(", code: ").append(e.getCode());
        String body = e.getResponseBody();
        if (StringUtils.isNotEmpty(body)) {
            sb.append(", response: ").append(body);
        }
        return sb.toString();
    }

    public static ErrorCode getErrorCodeEnum(String type, String code) {
        switch (type) {
        case "ArrangementCode":
        	return ArrangementCode.valueOf(code);
        case "RegistryCode":
        	return RegistryCode.valueOf(code);
        default:
        	throw new RuntimeException("Undefined error type: " + type + ", code: " + code);
        }
    }
}