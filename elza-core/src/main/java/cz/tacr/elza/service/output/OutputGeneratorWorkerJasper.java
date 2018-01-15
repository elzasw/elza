package cz.tacr.elza.service.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tacr.elza.controller.vo.OutputSettingsVO;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.print.JRAttPagePlaceHolder;
import cz.tacr.elza.print.OutputImpl;
import cz.tacr.elza.service.attachment.AttachmentService;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfReportConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

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

    @Autowired
    private AttachmentService attachmentService;

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

    /**
     * Provede vložení PDF příloh do výstupního streamu.
     *
     * @param inDocStream Vstupní PDF, generované jasperem
     * @param outDocStream finální výstupní PDF (předávané dál do DMS)
     * @param attachements seznam PDF příloh
     * @throws IOException
     */
    private static void includeAttachementsToOutput(InputStream inDocStream, OutputStream outDocStream, List<PDDocument> attachements) throws IOException {
        PDDocument inDoc = PDDocument.load(inDocStream);
        final PDDocument outDoc = new PDDocument();
        final List<PDDocument> attDoc = new ArrayList<>();
        boolean included = false;
        for (PDPage pdPage : inDoc.getPages()) {
            PDFTextStripper pdfStripper = null;
            pdfStripper = new PDFTextStripper();
            try (final PDDocument tmpDoc = new PDDocument()) {
                tmpDoc.addPage(pdPage);
                String text = pdfStripper.getText(tmpDoc);
                if (StringUtils.containsIgnoreCase(text, JRAttPagePlaceHolder.INCL_PATTERN)) {
                    if (!included) { // nahradit značku za přílohy, další výskyty značky jen vyhodit
                        attDoc.addAll(includeAttachements(outDoc, attachements));
                        included = true;
                    }
                } else {
                    outDoc.addPage(pdPage);
                }
            }
        }

        // na závěr vložit pokud nebyla značka v dokumentu
        if (!included) {
            attDoc.addAll(includeAttachements(outDoc, attachements));
        }

        outDoc.save(outDocStream);

        outDoc.close();
        inDoc.close();
        for (PDDocument pdDocument : attDoc) {
            pdDocument.close();
        }
    }

    /**
     *
     * @param outDoc Objekt výstupního dokumentu
     * @param attachements seznam příloh
     * @return seznam otevřených dokumentů příloh (je potřeba je zavřít až po dokončení generování)
     * @throws IOException
     */
    private static List<PDDocument> includeAttachements(PDDocument outDoc, List<PDDocument> attachements) throws IOException {
        List<PDDocument> attList = new ArrayList<>();
        for (PDDocument attDoc : attachements) {
            attDoc.getPages().forEach(outDoc::addPage);
            attList.add(attDoc); // nezavírat attDoc! - až po dokončení exportu outdoc
        }
        return attList;
    }

    /**
     * Zajistí připojení příloh ve formátu PDF do výstupu.
     *
     * @param output definice obsahu výstupu
     * @param in vstup z jasperu
     * @param outm výstup tisku
     */
    private void mergePdfJasperAndAttachements(final OutputImpl output, final PipedInputStream in, final PipedOutputStream outm) {
        try {
            includeAttachementsToOutput(in, outm, output.getAttDocs());
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
