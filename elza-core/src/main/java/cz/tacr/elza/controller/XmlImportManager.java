package cz.tacr.elza.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.api.vo.XmlImportConfig;
import cz.tacr.elza.service.XmlImportService;


/**
 * Implementace API pro import dat z xml.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 11. 2015
 */
@RestController
@RequestMapping("/api/xmlImportManager")
public class XmlImportManager implements cz.tacr.elza.api.controller.XmlImportManager {

    @Autowired
    private XmlImportService xmlImportService;

    @Override
    public void importData(XmlImportConfig config) {
        Assert.notNull(config);

//        try {
//            xmlImportService.importData(config);
//        } catch (XmlImportException e) {
//            throw new IllegalStateException(e);
//        }
    }
}
