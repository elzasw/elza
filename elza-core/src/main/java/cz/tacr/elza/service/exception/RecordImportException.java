package cz.tacr.elza.service.exception;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 21. 12. 2015
 */
public class RecordImportException extends NonFatalXmlImportException {

    public RecordImportException(String message) {
        super(message);
    }
}
