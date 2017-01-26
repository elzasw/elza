package cz.tacr.elza.service.exception;

import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 21. 12. 2015
 */
public abstract class XmlImportException extends AbstractException {

    public XmlImportException(String message) {
        super(message, BaseCode.SYSTEM_ERROR);
    }
}
