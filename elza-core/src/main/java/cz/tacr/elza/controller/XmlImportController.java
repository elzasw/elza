package cz.tacr.elza.controller;

import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.XmlImportConfigVO;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.XmlImportService;
import cz.tacr.elza.service.exception.XmlImportException;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 1. 2. 2016
 */
@Controller
@RequestMapping("/api/xmlImportManagerV2")
public class XmlImportController {

    @Autowired
    private XmlImportService xmlImportService;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private ScopeRepository scopeRepository;

    @RequestMapping(value = "/import", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void importData(MultipartHttpServletRequest request) { /// Mělo by být VO
        XmlImportConfigVO configVO = new XmlImportConfigVO();
        configVO.setXmlFile(request.getFile("xmlFile"));
        configVO.setRegScope(((RegScopeVO) request.getAttribute("regScope")));
        configVO.setImportDataFormat(XmlImportType.valueOf(request.getParameter("importDataFormat")));
        if (request.getAttribute("stopOnError") != null) {
            configVO.setStopOnError((Boolean) request.getAttribute("stopOnError"));
        } else {
            configVO.setStopOnError(false);
        }
        if (request.getAttribute("transformationName") != null) {
            configVO.setTransformationName(request.getParameter("transformationName"));
        }

        Assert.notNull(configVO);

        XmlImportConfig config = factoryDO.createXmlImportConfig(configVO);
        RegScope regScope = factoryDO.createScope(configVO.getRegScope());
        if (regScope.getScopeId() == null) {
            scopeRepository.save(regScope);
        }
        config.setRegScope(regScope);

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
