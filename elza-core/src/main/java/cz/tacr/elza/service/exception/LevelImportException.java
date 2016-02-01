package cz.tacr.elza.service.exception;


/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 1. 2016
 */
public class LevelImportException extends NonFatalXmlImportException {

    public LevelImportException(String message) {
        super(message);
    }
}
