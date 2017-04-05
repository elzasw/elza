package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;

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
        if (!ObjectUtils.equals(o1, o2)) {
            throw new BusinessException(message, code);
        }
    }

    public static void notEqualsElseBusiness(final Object o1, final Object o2, final ErrorCode code) {
        notEqualsElseBusiness(o1, o2, code.getType() + "-" + code.getCode(), code);
    }

    public static void notEqualsElseBusiness(final Object o1, final Object o2, final String message, final ErrorCode code) {
        if (ObjectUtils.equals(o1, o2)) {
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

}
