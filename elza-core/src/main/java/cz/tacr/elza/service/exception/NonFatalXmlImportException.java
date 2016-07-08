package cz.tacr.elza.service.exception;

/**
 * Výjimka která reprezentuje chybu při které se nemusí přeřušit běh importu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 1. 2016
 */
public class NonFatalXmlImportException extends XmlImportException {

    public NonFatalXmlImportException(String message) {
        super(message);
    }
}
