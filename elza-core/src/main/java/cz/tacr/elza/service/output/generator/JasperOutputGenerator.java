package cz.tacr.elza.service.output.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.http.MediaType;

import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.tree.FundTreeProvider;
import cz.tacr.elza.exception.ProcessException;
import cz.tacr.elza.print.File;
import cz.tacr.elza.print.OutputModel;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemFileRef;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.output.OutputParams;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

public class JasperOutputGenerator extends DmsOutputGenerator {

    private static final String TEMPLATE_EXTENSION = ".jrxml";
    private static final String MAIN_TEMPLATE_NAME = "index" + TEMPLATE_EXTENSION;
    private static final int MAX_MERGE_MAIN_MEMORY_BYTES = 100 * 1024 * 1024;

    private final OutputModel outputModel;

    JasperOutputGenerator(StaticDataService staticDataService,
                          FundTreeProvider fundTreeProvider,
                          NodeCacheService nodeCacheService,
                          EntityManager em,
                          DmsService dmsService) {
        super(em, dmsService);
        outputModel = new OutputModel(staticDataService, fundTreeProvider, nodeCacheService);
    }

    @Override
    public void init(OutputParams params) {
        super.init(params);
        outputModel.init(params);
    }
    @Override
    protected void generate(OutputStream os) throws IOException {
        Path templateFile = params.getTemplateDir().resolve(MAIN_TEMPLATE_NAME);
        JasperReport report = loadTemplate(templateFile);

        // prepare parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("output", outputModel);
        parameters.put("fund", outputModel.getFund());

        prepareSubreports(parameters);

        Path partialResult = generatePdfFile(report, parameters);

        mergePDFAndAttachments(partialResult, os);
    }

    private void prepareSubreports(Map<String, Object> parameters) throws IOException {
        Files.list(params.getTemplateDir()).filter(path -> {
            String name = path.getFileName().toString();
            return name.endsWith(TEMPLATE_EXTENSION) && !name.equals(MAIN_TEMPLATE_NAME);
        }).forEach(path -> {
            String subreportName = path.getFileName().toString();
            subreportName = subreportName.substring(0, subreportName.length() - TEMPLATE_EXTENSION.length());
            JasperReport subreport = loadTemplate(path);
            parameters.put(subreportName, subreport);
        });
    }

    private JasperReport loadTemplate(Path templateFile) {
        try (InputStream is = Files.newInputStream(templateFile, StandardOpenOption.READ)) {
            return JasperCompileManager.compileReport(is);
        } catch (IOException | JRException e) {
            throw new ProcessException(params.getDefinitionId(), "Failed to parse Jasper template", e);
        }
    }

    private Path generatePdfFile(JasperReport report, Map<String, Object> parameters) {
        JasperPrint print;
        try {
            print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
        } catch (JRException e) {
            throw new ProcessException(params.getDefinitionId(), "Failed to create Jasper document", e);
        }

        Path pdfFile = tempFileProvider.createTempFile();

        try (OutputStream os = Files.newOutputStream(pdfFile, StandardOpenOption.WRITE)) {
            JasperExportManager.exportReportToPdfStream(print, os);
        } catch (IOException | JRException e) {
            throw new ProcessException(params.getDefinitionId(), "Failed to generate PDF from Jasper document", e);
        }

        return pdfFile;
    }

    private void mergePDFAndAttachments(Path pdfFile, OutputStream os) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationStream(os);

        merger.addSource(pdfFile.toAbsolutePath().toString());

        List<String> pdfAttachPaths = getPDFAttachmentPaths();
        for (String path : pdfAttachPaths) {
            merger.addSource(path);
        }

        merger.mergeDocuments(MemoryUsageSetting.setupMixed(MAX_MERGE_MAIN_MEMORY_BYTES));
    }

    private List<String> getPDFAttachmentPaths() {
        List<String> pdfAttachPaths = new ArrayList<>();
        for (Item item : outputModel.getItems()) {
            if (item instanceof ItemFileRef) {
                File file = item.getValue(File.class);
                if (file.getMimeType().equals(MediaType.APPLICATION_PDF_VALUE)) {
                    String filePath = dmsService.getFilePath(file.getFileId());
                    pdfAttachPaths.add(filePath);
                }
            } ;
        }
        return pdfAttachPaths;
    }
}
