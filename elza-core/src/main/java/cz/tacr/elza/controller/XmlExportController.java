package cz.tacr.elza.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.service.XmlExportService;
import cz.tacr.elza.service.vo.XmlExportResult;
import cz.tacr.elza.xmlexport.v1.XmlExportConfig;

/**
 * Kontroler pro xml export.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 4. 2016
 */
@RestController
public class XmlExportController {

    @Autowired
    private XmlExportService xmlExportService;

    @RequestMapping(value = "/api/xmlExportManagerV2/fund/{versionId}", method = RequestMethod.GET, produces = "application/*")
    public void exportFund(final HttpServletResponse response,
                           @PathVariable(value = "versionId") final Integer versionId,
                           @RequestParam(value = "transformationName") final String transformationName
    ) throws IOException {
        Assert.notNull(versionId);

        XmlExportConfig config = new XmlExportConfig(versionId);
        if (StringUtils.isNotBlank(transformationName)) {
            config.setTransformationName(transformationName);
        }

        XmlExportResult exportResult = xmlExportService.exportData(config);

        String contentType;
        if (exportResult.isCompressed()) {
            contentType = "application/zip";
        } else {
            contentType = "application/xml";
        }

        response.setCharacterEncoding("UTF-8");
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + exportResult.getFileName());
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.

        File exportedData = exportResult.getExportedData();
        try (InputStream inputStream = new FileInputStream(exportedData)) {
            int contentLength = FileCopyUtils.copy(inputStream, response.getOutputStream());
            response.setContentLength(contentLength);
        }

        response.flushBuffer();
    }

    @RequestMapping(value = "/api/xmlExportManagerV2/transformations", method = RequestMethod.GET)
    public List<String> getTransformations() {
        return xmlExportService.getTransformationNames();
    }
}
