package cz.tacr.elza.controller;

import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.XmlImportService;
import cz.tacr.elza.service.exception.XmlImportException;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.List;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 1. 2. 2016
 */
@RestController
public class XmlImportController {

    @Autowired
    private XmlImportService xmlImportService;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private ScopeRepository scopeRepository;

    @RequestMapping(value = "/api/xmlImportManagerV2/import", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void importData(
            @RequestParam(required = false, value = "transformationName") final String transformationName,
            @RequestParam(required = false, value = "stopOnError") final Boolean stopOnError,
            @RequestParam(required = false, value = "importDataFormat") final XmlImportType type,
            @RequestParam(required = false, value = "scopeName") final String scopeName,
            @RequestParam(required = false, value = "scopeId") final Integer scopeId,
            @RequestParam(required = true, value = "xmlFile") final MultipartFile xmlFile) {
        XmlImportConfig config = new XmlImportConfig();
        config.setXmlFile(xmlFile);

        config.setXmlImportType(type);
        config.setTransformationName(transformationName);
        config.setStopOnError(stopOnError == null ? false : stopOnError);
        RegScope regScope;
        if (scopeId == null) {
            regScope = new RegScope();
            Assert.notNull(scopeName);
            regScope.setName(scopeName);
            regScope.setCode(StringUtils.upperCase(Normalizer.normalize(StringUtils.replace(StringUtils.substring(scopeName, 0, 50).trim(), " ", "_"), Normalizer.Form.NFD)));
            scopeRepository.save(regScope);
        } else {
            regScope = scopeRepository.findOne(scopeId);
        }
        config.setRegScope(regScope);

        try {
            xmlImportService.importData(config);
        } catch (XmlImportException e) {
            throw new IllegalStateException(e);
        }
    }

    @RequestMapping(value = "/api/xmlImportManagerV2/transformations", method = RequestMethod.GET)
    public List<String> getTransformations() {
        return xmlImportService.getTransformationNames();
    }
}
