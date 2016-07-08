package cz.tacr.elza.service.exception;

/**
 * Výjimka reprezentující špatná vstupní data.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 1. 2016
 */
public class InvalidDataException extends NonFatalXmlImportException {

    public InvalidDataException(String message) {
        super(message);
    }
}
