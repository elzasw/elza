package cz.tacr.elza.service.exception;


/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 21. 12. 2015
 */
public class PartyImportException extends NonFatalXmlImportException {

    public PartyImportException(String message) {
        super(message);
    }
}
