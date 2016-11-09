package cz.tacr.elza.exception.codes;

/**
 * Rozhraní chybového kódu.
 *
 * @author Martin Šlapa
 * @since 09.11.2016
 */
public interface ErrorCode {

    default String getType() {
        return getClass().getSimpleName();
    }

    default String getCode() {
        return toString();
    }

}
