package cz.tacr.elza.api.controller;

import cz.tacr.elza.api.vo.XmlImportConfig;


/**
 * Rozhraní pro import z xml.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 11. 2015
 */
public interface XmlImportManager {

    void importData(XmlImportConfig config);
}
