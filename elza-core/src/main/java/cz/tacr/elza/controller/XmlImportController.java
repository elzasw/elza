package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.service.XmlImportService;
import cz.tacr.elza.service.exception.XmlImportException;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 1. 2. 2016
 */
@RestController
@RequestMapping("/api/xmlImportManagerV2")
public class XmlImportController {

    @Autowired
    private XmlImportService xmlImportService;

    @RequestMapping(value = "/import", method = RequestMethod.POST)
    public void importData(XmlImportConfig config) {
        Assert.notNull(config);

        try {
            xmlImportService.importData(config);
        } catch (XmlImportException e) {
            throw new IllegalStateException(e);
        }
    }

    @RequestMapping(value = "/transformations", method = RequestMethod.GET)
    public List<String> getTransformations() {
        return xmlImportService.getTransformationNames();
    }
}
