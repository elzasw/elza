package cz.tacr.elza.service.exception;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 21. 12. 2015
 */
public abstract class XmlImportException extends Exception {

    public XmlImportException(String message) {
        super(message);
    }
}
