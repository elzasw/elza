package cz.tacr.elza.service.exception;

/**
 * Výjimka reprezentující chybu při která nemá smysl pokračovat v behu importu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 1. 2016
 */
public class FatalXmlImportException extends XmlImportException {

    public FatalXmlImportException(String message) {
        super(message);
    }
}
