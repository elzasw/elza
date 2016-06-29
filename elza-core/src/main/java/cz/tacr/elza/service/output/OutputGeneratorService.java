package cz.tacr.elza.service.output;

import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Zajišťuje generování výstupu a jeho uložení do dms na základě vstupní definice.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 24.6.16
 */

@Service
public class OutputGeneratorService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String JASPER_MAIN_TEMPLATE_BASE_NAME = "index";
    private static final String JASPER_TEMPLATE_SUFFIX = ".jrxml";
    private static final String JASPER_MAIN_TEMPLATE = JASPER_MAIN_TEMPLATE_BASE_NAME + JASPER_TEMPLATE_SUFFIX;
    private static final String OUTFILE_SUFFIX_PDF = ".pdf";


    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private OutputFactoryService outputFactoryService;

    @Autowired
    private DmsService dmsService;

    @Value("${elza.templates.templatesDir}")
    private String templatesDir;

    // TODO - JavaDoc - Lebeda
    public String generateOutput(ArrOutput arrOutput) {
        final ArrOutputDefinition arrOutputDefinition = arrOutput.getOutputDefinition();
//        final ArrOutputDefinition outputDefinition = arrOutput.getOutputDefinition();
        final RulTemplate rulTemplate = arrOutputDefinition.getTemplate();
        Assert.notNull(rulTemplate, "Nepodařilo se najít definici šablony.");

        // sestavení outputu
        final Output output = outputFactoryService.createOutput(arrOutput);

        // skutečné vytvoření výstupného souboru na základě definice
        if (RulTemplate.Engine.JASPER.equals(rulTemplate.getEngine())) {
            generatePdfByJasper(arrOutputDefinition, rulTemplate, output);
        } else if (RulTemplate.Engine.JASPER.equals(rulTemplate.getEngine())) {
            // TODO Lebeda - implementovat podporu pro FreeMarker
        }

        eventNotificationService.publishEvent(EventFactory.createIdInVersionEvent(EventType.OUTPUT_GENERATED, arrangementService.getOpenVersionByFundId(arrOutput.getOutputDefinition().getFund().getFundId()), arrOutput.getOutputId()));

        return new Date().toString();
    }

    // TODO - JavaDoc - Lebeda
    private void generatePdfByJasper(ArrOutputDefinition arrOutputDefinition, RulTemplate rulTemplate, Output output) {
        try {
            // tisk do PDF
            final String rulTemplateDirectory = rulTemplate.getDirectory();
            final File templateDir = Paths.get(templatesDir, rulTemplateDirectory).toFile();
            Assert.isTrue(templateDir.exists() && templateDir.isDirectory(), "Nepodařilo se najít adresář s definicí šablony: " + templateDir.getAbsolutePath());

            final File mainJasperTemplate = Paths.get(templateDir.getAbsolutePath(), JASPER_MAIN_TEMPLATE).toFile();
            Assert.isTrue(mainJasperTemplate.exists(), "Nepodařilo se najít definici hlavní šablony.");

            JasperReport jasperReport = JasperCompileManager.compileReport(mainJasperTemplate.getAbsolutePath());

            // Parameters for report
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("output", output);

            // subreporty
            final File[] files = templateDir.listFiles((dir, name) -> name.endsWith(JASPER_TEMPLATE_SUFFIX) && !name.equals(JASPER_MAIN_TEMPLATE));
            Arrays.stream(files).forEach(file -> {
                try {
                    parameters.put(FilenameUtils.getBaseName(file.getAbsolutePath()), JasperCompileManager.compileReport(file.getAbsolutePath()));
                } catch (JRException e) {
                    throw new IllegalStateException("Chyba kompilace subšablony reportu " + file.getAbsolutePath(), e);
                }
            });

            // DataSource
            JRDataSource dataSource = new JREmptyDataSource();
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

                // TODO Lebeda - smazat - jen pro ladění
                OutputStream fileOutputStream = new FileOutputStream("/tmp/testPage.pdf"); // TODO Lebeda - jen pro ladění
                JasperExportManager.exportReportToPdfStream(jasperPrint, fileOutputStream);
                fileOutputStream.close();

            // Export to PDF - pouze příprava procesu pro renderování - reálně proběhne až při čtení z in v dms
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in);
            new Thread(
                    new Runnable() {
                        public void run() {
                            try {
                                JasperExportManager.exportReportToPdfStream(jasperPrint, out);
                            } catch (JRException e) {
                                throw new IllegalStateException("Nepodařilo se vyrenderovat PDF ze šablony " + mainJasperTemplate.getAbsolutePath() + ".");
                            }
                        }
                    }
            ).start();

            // Uložení do výstupní struktury
            ArrOutputResult outputResult = new ArrOutputResult();
            outputResult.setChange(output.getFund().getArrFundVersion().getLockChange());
            outputResult.setOutputDefinition(arrOutputDefinition);
            outputResult.setTemplate(rulTemplate);

            ArrOutputFile dmsFile = new ArrOutputFile();
            dmsFile.setOutputResult(outputResult);
            dmsFile.setFileName(arrOutputDefinition.getName() + OUTFILE_SUFFIX_PDF);
            dmsFile.setName(arrOutputDefinition.getName());
            dmsFile.setMimeType(DmsService.MIME_TYPE_APPLICATION_PDF);
            dmsFile.setFileSize(0); // 0 - zajistí refresh po skutečném uložení do souboru na disk

            // TODO Lebeda - odkomentovat  - jen pro ladění
//            dmsService.createFile(dmsFile, in); // zajistí prezentaci výstupu na klienta
            // TODO Lebeda - smazat  - jen pro ladění
            in.close();

        } catch (JRException e) {
            throw new IllegalStateException("Nepodařilo se vytisknout report.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se uložit výstup.", e);
        }
    }

}
