package cz.tacr.elza.service;

import cz.tacr.elza.service.exception.XmlImportException;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 1. 2016
 */
public class LevelImportException extends XmlImportException {

    public LevelImportException(String message) {
        super(message);
    }
}
