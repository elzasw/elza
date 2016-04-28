package cz.tacr.elza.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.PathParam;

import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

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

        ServletOutputStream outputStream = response.getOutputStream();
        if (exportResult.getTransformedData() != null) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=" + exportResult.getFundName() + ".zip");
            ZipOutputStream zos = new ZipOutputStream(outputStream);
            ZipEntry zipEntry = new ZipEntry(exportResult.getFundName() + ".xml");
            zos.putNextEntry(zipEntry);

            File xmlData = exportResult.getXmlData();
            InputStream inputStream = new FileInputStream(xmlData);
            IOUtils.copy(inputStream, zos);
            inputStream.close();

            zipEntry = new ZipEntry(exportResult.getFundName() + ".transformed");
            zos.putNextEntry(zipEntry);

            File transformedData = exportResult.getTransformedData();
            inputStream = new FileInputStream(transformedData);
            IOUtils.copy(inputStream, zos);

            inputStream.close();
            outputStream.close();
            zos.close();
        } else {
            response.setContentType("application/xml");
            response.setHeader("Content-Disposition", "attachment; filename=" + exportResult.getFundName() + ".xml");

            File xmlData = exportResult.getXmlData();
            InputStream inputStream = new FileInputStream(xmlData);
            int contentLength = FileCopyUtils.copy(inputStream, outputStream);

//            File export = new File("D:\\export.xml");
//            try (InputStream fileInputStream = new FileInputStream(xmlData);
//                    FileOutputStream fos = new FileOutputStream(export)) {
//                FileCopyUtils.copy(fileInputStream, fos);
//            }
            response.setContentLength(contentLength);

            inputStream.close();
            outputStream.close();
        }
        response.flushBuffer();
    }

    @RequestMapping(value = "/api/xmlExportManagerV2/transformations", method = RequestMethod.GET)
    public List<String> getTransformations() {
        return xmlExportService.getTransformationNames();
    }

}
