package cz.tacr.elza.service.output;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tacr.elza.controller.vo.OutputSettingsVO;
import cz.tacr.elza.exception.SystemException;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.print.OutputImpl;
import cz.tacr.elza.print.item.ItemFile;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.export.SimplePdfReportConfiguration;

/**
 * Zajišťuje generování výstupu a jeho uložení do dms na základě vstupní definice - část generování specifická pro jasper.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 24.6.16
 */

@Component
@Scope("prototype")
class OutputGeneratorWorkerJasper extends OutputGeneratorWorkerAbstract {

    private static final String JASPER_TEMPLATE_SUFFIX = ".jrxml";
    private static final String JASPER_MAIN_TEMPLATE = MAIN_TEMPLATE_BASE_NAME + JASPER_TEMPLATE_SUFFIX;

    private static final int MAX_MERGE_MAIN_MEMORY_BYTES = 100 * 1024 * 1024;

    @Override
    protected InputStream getContent(final ArrOutputDefinition arrOutputDefinition, final RulTemplate rulTemplate, final OutputImpl output) {
        try {
            // dohledání šablony
            final File mainJasperTemplate = getTemplate(rulTemplate, JASPER_MAIN_TEMPLATE);

            JasperReport jasperReport = JasperCompileManager.compileReport(mainJasperTemplate.getAbsolutePath());

            // Parameters for report
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("output", output);
            parameters.put("fund", output.getFund());

            // subreporty
            addSubreports(rulTemplate, parameters);

            // DataSource
            JRDataSource dataSource = new JREmptyDataSource();
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // Export to PDF - pouze příprava procesu pro renderování - reálně proběhne až při čtení z in v dms
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in);

            // připojení PDF příloh
            PipedInputStream inm = new PipedInputStream();
            PipedOutputStream outm = new PipedOutputStream(inm);

            generatorThread = new Thread(() -> {
                new Thread(() -> {
                    try {
                        renderJasperPdf(mainJasperTemplate, jasperPrint, out, getReportConfig(arrOutputDefinition.getOutputSettings()));
                    } catch (IOException e) {
                        setException(new IllegalStateException("Nepodařilo se vyrenderovat výstup ze šablony"));
                    }
                }).start();
                mergePdfJasperAndAttachements(output, in, outm);
            });
            generatorThread.start();

            return inm;
        } catch (JRException e) {
            throw new SystemException("Nepodařilo se vytisknout report.", e);
        } catch (IOException e) {
            throw new SystemException("Nepodařilo se uložit výstup.", e);
        }
    }

    private SimplePdfReportConfiguration getReportConfig(String outputSettings) throws IOException {
        SimplePdfReportConfiguration reportConfig = new SimplePdfReportConfiguration();
        if (outputSettings == null) {
            return reportConfig;
        }
        ObjectMapper mapper = new ObjectMapper();
        OutputSettingsVO settingsVO = mapper.readValue(outputSettings, OutputSettingsVO.class);
        reportConfig.setEvenPageOffsetX(settingsVO.getEvenPageOffsetX());
        reportConfig.setEvenPageOffsetY(settingsVO.getEvenPageOffsetY());
        reportConfig.setOddPageOffsetX(settingsVO.getOddPageOffsetX());
        reportConfig.setOddPageOffsetY(settingsVO.getOddPageOffsetY());
        return reportConfig;
    }

    private void addSubreports(final RulTemplate rulTemplate, final Map<String, Object> parameters) {
        File templateDir = getTemplatesDir(rulTemplate);
        final File[] files = templateDir.listFiles((dir, name) -> name.endsWith(JASPER_TEMPLATE_SUFFIX) && !name.equals(JASPER_MAIN_TEMPLATE));
        if (files != null) {
            Arrays.stream(files).forEach(file -> {
                try {
                    parameters.put(FilenameUtils.getBaseName(file.getAbsolutePath()), JasperCompileManager.compileReport(file.getAbsolutePath()));
                } catch (JRException e) {
                    throw new SystemException("Chyba kompilace subšablony reportu " + file.getAbsolutePath(), e);
                }
            });
        }
    }

    private void mergePdfJasperAndAttachements(final OutputImpl output, final PipedInputStream in, final PipedOutputStream outm) {
        PDFMergerUtility ut = new PDFMergerUtility();
        ut.addSource(in);
        final List<ItemFile> attachements = output.getAttachements();
        attachements.forEach(itemFile -> {
                    File file = itemFile.getFile();
                    if (file == null) { // obezlička, protože arrFile ten file nevrací
                        final DmsFile dmsFile = dmsService.getFile(itemFile.getFileId());
                        final String filePath = dmsService.getFilePath(dmsFile);
                        file = new File(filePath);
                    }

                    try {
                        ut.addSource(file);
                    } catch (FileNotFoundException e) {
                        throw new SystemException(e);
                    }
                });
        ut.setDestinationStream(outm);
        try {
            ut.mergeDocuments(MemoryUsageSetting.setupMixed(MAX_MERGE_MAIN_MEMORY_BYTES));
            outm.close();
        } catch (IOException e) {
            setException(new SystemException(e));
        }
    }

    private void renderJasperPdf(final File mainJasperTemplate, final JasperPrint jasperPrint, final PipedOutputStream out, SimplePdfReportConfiguration reportConfig) {
        try {
            exportToPdfStream(jasperPrint, out, reportConfig);
            out.close();
        } catch (JRRuntimeException | JRException | IOException e) {
            setException(new SystemException("Nepodařilo se vyrenderovat PDF ze šablony " + mainJasperTemplate.getAbsolutePath() + ".", e));
        }
    }

    private void exportToPdfStream(
            JasperPrint jasperPrint,
            OutputStream outputStream,
            SimplePdfReportConfiguration reportConfig) throws JRException
    {
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.setConfiguration(reportConfig);

        exporter.exportReport();
    }
}
