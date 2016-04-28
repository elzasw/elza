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

import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

    @RequestMapping(value = "/api/xmlExportManagerV2/export", method = RequestMethod.POST, produces = "application/*")
    public void importData(final HttpServletResponse response,
            @RequestBody(required = true) final ExportConfig exportConfig) throws IOException {
        Assert.notNull(exportConfig);
        Assert.notNull(exportConfig.getVersionId());

        XmlExportConfig config = new XmlExportConfig(exportConfig.getVersionId());
        if (StringUtils.isNotBlank(exportConfig.getTransformationName())) {
            config.setTransformationName(exportConfig.getTransformationName());
        }

        XmlExportResult exportResult = xmlExportService.exportData(config);

        String contentType;
        String fileName;
        ServletOutputStream outputStream = response.getOutputStream();
        if (exportResult.getTransformedData() != null) {
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

            contentType = "application/zip";
            fileName = exportResult.getFundName() + ".zip";

            inputStream.close();
            outputStream.close();
            zos.close();
        } else {
            File xmlData = exportResult.getXmlData();
            InputStream inputStream = new FileInputStream(xmlData);
            int contentLength = FileCopyUtils.copy(inputStream, outputStream);

//            File export = new File("D:\\export.xml");
//            try (InputStream fileInputStream = new FileInputStream(xmlData);
//                    FileOutputStream fos = new FileOutputStream(export)) {
//                FileCopyUtils.copy(fileInputStream, fos);
//            }

            contentType = "application/xml";
            fileName = exportResult.getFundName() + ".xml";
            response.setContentLength(contentLength);

            inputStream.close();
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

    public static class ExportConfig {

        private Integer versionId;

        private String transformationName;

        public Integer getVersionId() {
            return versionId;
        }

        public void setVersionId(final Integer versionId) {
            this.versionId = versionId;
        }

        public String getTransformationName() {
            return transformationName;
        }

        public void setTransformationName(final String transformationName) {
            this.transformationName = transformationName;
        }
    }
}
