package cz.tacr.elza.service;

import cz.tacr.elza.service.exception.XmlImportException;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 21. 12. 2015
 */
public class PartyImportException extends XmlImportException {

    public PartyImportException(String message) {
        super(message);
    }
}
