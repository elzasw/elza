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

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.OutputSettingsVO;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.fund.FundTreeProvider;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ProcessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.File;
import cz.tacr.elza.print.OutputModel;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemFileRef;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.output.OutputParams;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfReportConfiguration;

public class JasperOutputGenerator extends DmsOutputGenerator {

    private static final String TEMPLATE_EXTENSION = ".jrxml";
    private static final String MAIN_TEMPLATE_NAME = "index" + TEMPLATE_EXTENSION;
    private static final int MAX_MERGE_MAIN_MEMORY_BYTES = 100 * 1024 * 1024;

    private final OutputModel outputModel;

    SimplePdfReportConfiguration pdfExpConfig = new SimplePdfReportConfiguration();

    JasperOutputGenerator(StaticDataService staticDataService,
                          FundTreeProvider fundTreeProvider,
                          NodeCacheService nodeCacheService,
                          InstitutionRepository institutionRepository,
                          ApDescriptionRepository apDescRepository, 
                          ApNameRepository apNameRepository,
                          ApExternalIdRepository apEidRepository,
                          EntityManager em,
                          DmsService dmsService) {
        super(em, dmsService);
        outputModel = new OutputModel(staticDataService, fundTreeProvider, nodeCacheService, institutionRepository,
                apDescRepository, apNameRepository, apEidRepository);
    }

    @Override
    public void init(OutputParams params) {
        super.init(params);
        outputModel.init(params);

        // Parse PDF settings
        String outputSettings = params.getDefinition().getOutputSettings();
        if (StringUtils.isNotBlank(outputSettings)) {
            ObjectMapper mapper = new ObjectMapper();
            OutputSettingsVO settingsVO;
            try {
                settingsVO = mapper.readValue(outputSettings, OutputSettingsVO.class);
            } catch (Exception e) {
                throw new BusinessException("Failed to parse configuration", e, BaseCode.JSON_PARSE)
                        .set("settings", outputSettings);
            }
            pdfExpConfig.setEvenPageOffsetX(settingsVO.getEvenPageOffsetX());
            pdfExpConfig.setEvenPageOffsetY(settingsVO.getEvenPageOffsetY());
            pdfExpConfig.setOddPageOffsetX(settingsVO.getOddPageOffsetX());
            pdfExpConfig.setOddPageOffsetY(settingsVO.getOddPageOffsetY());
        }
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

        DefaultJasperReportsContext jasperContext = DefaultJasperReportsContext.getInstance();
        JasperFillManager fillManager = JasperFillManager.getInstance(jasperContext);

        JasperPrint jasperPrint;
        try {
            jasperPrint = fillManager.fill(report, parameters, new JREmptyDataSource());
        } catch (JRException e) {
            throw new ProcessException(params.getDefinitionId(), "Failed to create Jasper document", e);
        }

        Path pdfFile = tempFileProvider.createTempFile();

        try (OutputStream os = Files.newOutputStream(pdfFile, StandardOpenOption.WRITE)) {

            // Generate output to PDF
            JRPdfExporter exporter = new JRPdfExporter(jasperContext);

            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(os));
            exporter.setConfiguration(pdfExpConfig);

            exporter.exportReport();

            // JasperExportManager.exportReportToPdfStream(print, os);
        } catch (IOException | JRException e) {
            throw new ProcessException(params.getDefinitionId(), "Failed to generate PDF from Jasper document", e);
        }

        return pdfFile;
    }

    private void mergePDFAndAttachments(Path pdfFile, OutputStream os) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationStream(os);

        merger.addSource(pdfFile.toAbsolutePath().toFile());

        List<Path> pdfAttachments = getPDFAttachments();
        for (Path path : pdfAttachments) {
            merger.addSource(path.toAbsolutePath().toFile());
        }

        merger.mergeDocuments(MemoryUsageSetting.setupMixed(MAX_MERGE_MAIN_MEMORY_BYTES));
    }

    private List<Path> getPDFAttachments() {
        List<Path> pdfAttachments = new ArrayList<>();
        for (Item item : outputModel.getItems()) {
            if (item instanceof ItemFileRef) {
                File file = item.getValue(File.class);
                if (file.getMimeType().equals(MediaType.APPLICATION_PDF_VALUE)) {
                    Path filePath = dmsService.getFilePath(file.getFileId());
                    pdfAttachments.add(filePath);
                }
            } ;
        }
        return pdfAttachments;
    }
}
