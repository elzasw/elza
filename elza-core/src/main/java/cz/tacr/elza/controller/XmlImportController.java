package cz.tacr.elza.controller;

import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.XmlImportService;
import cz.tacr.elza.service.exception.XmlImportException;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 1. 2. 2016
 */
@Controller
public class XmlImportController {

    @Autowired
    private XmlImportService xmlImportService;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private ScopeRepository scopeRepository;

    @RequestMapping(value = "/api/xmlImportManagerV2/import", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String importData(
            //@RequestParam(required = false, value = "transformationName") final String transformationName,
            //@RequestParam(required = false, value = "stopOnError") final Boolean stopOnError,
            @RequestParam(required = false, value = "importDataFormat") final XmlImportType type,
            //@RequestParam(required = true, value = "regScope") final RegScopeVO scope,
            @RequestParam(required = true, value = "xmlFile") final MultipartFile xmlFile) {
        //XmlImportConfigVO configVO = new XmlImportConfigVO();
        XmlImportConfig config = new XmlImportConfig();
        config.setXmlFile(xmlFile);
        //config.setRegScope(factoryDO.createScope(s));
        config.setXmlImportType(type);
        //config.setTransformationName(transformationName);
        //config.setStopOnError(stopOnError == null ? false : stopOnError);

        /*RegScope regScope = factoryDO.createScope(scope);
        if (regScope.getScopeId() == null) {
            scopeRepository.save(regScope);
        }*/
        config.setRegScope(scopeRepository.findOne(1));

        try {
            xmlImportService.importData(config);
        } catch (XmlImportException e) {
            throw new IllegalStateException(e);
        }
        return "OK";
    }

    @RequestMapping(value = "/transformations", method = RequestMethod.GET)
    public List<String> getTransformations() {
        return xmlImportService.getTransformationNames();
    }
}
