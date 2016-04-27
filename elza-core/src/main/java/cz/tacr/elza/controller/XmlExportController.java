package cz.tacr.elza.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
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

    @RequestMapping(value = "/api/xmlExportManagerV2/export", method = RequestMethod.GET)
    public void importData(final HttpServletResponse response,
            @RequestParam(required = true, value = "versionId") final Integer versionId,
            @RequestParam(required = false, value = "transformationName") final String transformationName) throws IOException {
        XmlExportConfig config = new XmlExportConfig(versionId);
        if (StringUtils.isNotBlank(transformationName)) {
            config.setTransformationName(transformationName);
        }

        XmlExportResult exportResult = xmlExportService.exportData(config);

        String contentType;
        String fileName;
        ServletOutputStream outputStream = response.getOutputStream();
        if (exportResult.getTransformedData() != null) {
            ZipOutputStream zos = new ZipOutputStream(outputStream);
            ZipEntry zipEntry = new ZipEntry(exportResult.getFundName() + ".xml");
            zos.putNextEntry(zipEntry);

            byte[] xmlData = exportResult.getXmlData();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlData);
            IOUtils.copy(inputStream, zos);

            zipEntry = new ZipEntry(exportResult.getFundName() + ".transformed");
            zos.putNextEntry(zipEntry);

            byte[] transformedData = exportResult.getTransformedData();
            inputStream = new ByteArrayInputStream(transformedData);
            IOUtils.copy(inputStream, zos);

            contentType = "application/zip";
            fileName = exportResult.getFundName() + ".zip";

            outputStream.close();
            zos.close();
        } else {
            byte[] xmlData = exportResult.getXmlData();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlData);
            FileCopyUtils.copy(inputStream, outputStream);

            inputStream = new ByteArrayInputStream(xmlData);
            File export = new File("D:\\export.xml");
            FileOutputStream fos = new FileOutputStream(export);
            FileCopyUtils.copy(inputStream, fos);
            fos.close();

            contentType = "application/xml";
            fileName = exportResult.getFundName() + ".xml";
            response.setContentLength(xmlData.length);

            outputStream.close();
        }

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=" + fileName);
        response.flushBuffer();
    }

    @RequestMapping(value = "/api/xmlExportManagerV2/transformations", method = RequestMethod.GET)
    public List<String> getTransformations() {
        return xmlExportService.getTransformationNames();
    }
}
