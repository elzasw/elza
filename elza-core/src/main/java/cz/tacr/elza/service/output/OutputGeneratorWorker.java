package cz.tacr.elza.service.output;

import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.service.DmsService;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Zajišťuje generování výstupu a jeho uložení do dms na základě vstupní definice.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 24.6.16
 */

@Component
@Scope("prototype")
class OutputGeneratorWorker implements Callable<OutputGeneratorWorker> {

    private static final String JASPER_MAIN_TEMPLATE_BASE_NAME = "index";
    private static final String JASPER_TEMPLATE_SUFFIX = ".jrxml";
    private static final String JASPER_MAIN_TEMPLATE = JASPER_MAIN_TEMPLATE_BASE_NAME + JASPER_TEMPLATE_SUFFIX;
    private static final String OUTFILE_SUFFIX_PDF = ".pdf";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OutputFactoryService outputFactoryService;

    @Autowired
    private BulkActionService bulkActionService;

    @Autowired
    private OutputRepository outputRepository;

    @Autowired
    private OutputResultRepository outputResultRepository;

    @Autowired
    private DmsService dmsService;

    @Value("${elza.templates.templatesDir}")
    private String templatesDir;

    private Integer arrOutputId;

    public void init(Integer outputInProgress) {
        arrOutputId = outputInProgress;
    }

    @Override
    @Transactional
    public OutputGeneratorWorker call() throws Exception {
        generateOutput();
        return this;
    }

    // TODO - JavaDoc - Lebeda
    private String generateOutput() {
        logger.info("Spuštěno generování výstupu pro arr_output id={}", arrOutputId);
        ArrOutput arrOutput = outputRepository.findOne(arrOutputId);
        final ArrOutputDefinition arrOutputDefinition = arrOutput.getOutputDefinition();
        final RulTemplate rulTemplate = arrOutputDefinition.getTemplate();
        Assert.notNull(rulTemplate, "Nepodařilo se najít definici šablony.");

        // sestavení outputu
        logger.info("Sestavování modelu výstupu výstupu pro arr_output id={} spuštěno", arrOutputId);
        final Output output = outputFactoryService.createOutput(arrOutput);
        logger.info("Sestavování modelu výstupu výstupu pro arr_output id={} dokončeno", arrOutputId);

        // skutečné vytvoření výstupného souboru na základě definice
        if (RulTemplate.Engine.JASPER.equals(rulTemplate.getEngine())) {
            logger.info("Spuštěno generování PDF výstupu pro arr_output id={}", arrOutputId);
            generatePdfByJasper(arrOutputDefinition, rulTemplate, output);
            // dokončení generování je logováno v service onSucces/onFailure
        } else if (RulTemplate.Engine.JASPER.equals(rulTemplate.getEngine())) {
            // TODO Lebeda - implementovat podporu pro FreeMarker
        }

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
            Map<String, Object> parameters = new HashMap<>();
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

            // Export to PDF - pouze příprava procesu pro renderování - reálně proběhne až při čtení z in v dms
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in);
            new Thread(
                    new Runnable() {
                        public void run() {
                            try {
                                JasperExportManager.exportReportToPdfStream(jasperPrint, out);
                                out.close();
                            } catch (JRException | IOException e) {
                                throw new IllegalStateException("Nepodařilo se vyrenderovat PDF ze šablony " + mainJasperTemplate.getAbsolutePath() + ".", e);
                            }
                        }
                    }
            ).start();

            // TODO Lebeda - null = admin
            final ArrChange change = createChange(null);

            // Uložení do výstupní struktury
            ArrOutputResult outputResult = new ArrOutputResult();
            outputResult.setChange(change);
            outputResult.setOutputDefinition(arrOutputDefinition);
            outputResult.setTemplate(rulTemplate);
            outputResultRepository.save(outputResult);

            ArrOutputFile dmsFile = new ArrOutputFile();
            dmsFile.setOutputResult(outputResult);
            dmsFile.setFileName(arrOutputDefinition.getName() + OUTFILE_SUFFIX_PDF);
            dmsFile.setName(arrOutputDefinition.getName());
            dmsFile.setMimeType(DmsService.MIME_TYPE_APPLICATION_PDF);
            dmsFile.setFileSize(0); // 0 - zajistí refresh po skutečném uložení do souboru na disk

            dmsService.createFile(dmsFile, in); // zajistí prezentaci výstupu na klienta


        } catch (JRException e) {
            throw new IllegalStateException("Nepodařilo se vytisknout report.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se uložit výstup.", e);
        }
    }

    protected ArrChange createChange(final Integer userId) {
            // review Lebeda - je použití cizí service OK????
            return bulkActionService.createChange(userId);
        }

    public Integer getArrOutputId() {
        return arrOutputId;
    }
}
