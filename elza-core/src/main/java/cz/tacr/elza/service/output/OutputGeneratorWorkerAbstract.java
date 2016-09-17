package cz.tacr.elza.service.output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import cz.tacr.elza.repository.OutputDefinitionRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 5.8.16
 */
abstract class OutputGeneratorWorkerAbstract implements Callable<OutputGeneratorWorkerAbstract> {
    // konstanty pro pojmenování souborů šablon
    static final String MAIN_TEMPLATE_BASE_NAME = "index";

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
    private OutputDefinitionRepository outputDefinitionRepository;
    @Autowired
    private OutputGeneratorService outputGeneratorService;

    @Autowired
    protected DmsService dmsService;

    @Value("${elza.templates.templatesDir}")
    protected String templatesDir;

    private Integer arrOutputId;
    private Integer userId;
    private ArrChange change;
    private String extension;
    private String mimeType;

    public void init(final Integer outputInProgress, final Integer userId, final RulTemplate rulTemplate) {
        Assert.notNull(rulTemplate);

        this.arrOutputId = outputInProgress;
        this.userId = userId;
        this.extension = rulTemplate.getExtension();
        this.mimeType = rulTemplate.getMimeType();
    }

    @Override
    @Transactional
    public OutputGeneratorWorkerAbstract call() throws Exception {
        generateOutput();
        return this;
    }

    /**
     * Společná část generování výstupu.
     */
    private void generateOutput() {
        logger.info("Spuštěno generování výstupu pro arr_output id={}", arrOutputId);

        ArrOutput arrOutput = outputRepository.findOne(arrOutputId);
        final ArrOutputDefinition arrOutputDefinition = arrOutput.getOutputDefinition();

        try {

            final RulTemplate rulTemplate = arrOutputDefinition.getTemplate();
            Assert.notNull(rulTemplate, "Výstup nemá definovanou šablonu (ArrOutputDefinition.template je null).");

            // sestavení outputu
            logger.info("Sestavování modelu výstupu výstupu pro arr_output id={} spuštěno", arrOutputId);
            final Output output = outputFactoryService.createOutput(arrOutput);
            logger.info("Sestavování modelu výstupu výstupu pro arr_output id={} dokončeno", arrOutputId);

            // skutečné vytvoření výstupného souboru na základě definice
            logger.info("Spuštěno generování souboru pro arr_output id={}", arrOutputId);
            final InputStream content = getContent(arrOutputDefinition, rulTemplate, output);

            // Uložení do výstupní struktury a DMS
            storeOutputInDms(arrOutputDefinition, rulTemplate, content);
            arrOutputDefinition.setError(null);
        } catch (Exception ex) {
            arrOutputDefinition.setError(ex.getLocalizedMessage());
            StringBuilder stringBuffer = new StringBuilder();
            stringBuffer.append(ex.getMessage()).append("\n");
            Throwable cause = ex.getCause();
            while(cause != null && stringBuffer.length() < 1000) {
                stringBuffer.append(cause.getMessage()).append("\n");
                cause = cause.getCause();
            }
            arrOutputDefinition.setError(stringBuffer.length() > 1000 ? stringBuffer.substring(0, 1000) : stringBuffer.toString());

            arrOutputDefinition.setState(ArrOutputDefinition.OutputState.OPEN);
            outputDefinitionRepository.save(arrOutputDefinition);
            outputGeneratorService.publishOutputStateEvent(arrOutputDefinition, OutputGeneratorService.OUTPUT_WEBSOCKET_ERROR_STATE);

            logger.error("Generování výstupu pro arr_output id=" + arrOutputId + " dokončeno s chybou.", ex);
        }
    }

    /**
     * Provede vygenerování výstupného souboru.
     * Na  výstupu předá stream a vlastní generování se provádí až při čtení obsahu.
     *
     * @param arrOutputDefinition vybraná definice výstupu
     * @param rulTemplate šablona výstupu
     * @param output výstup
     * @return generovaný obsah výstupního souboru
     */
    protected abstract InputStream getContent(ArrOutputDefinition arrOutputDefinition, RulTemplate rulTemplate, Output output);

    /**
     * zajistí uložení výstupu do DB a DMS.
     */
    private void storeOutputInDms(final ArrOutputDefinition arrOutputDefinition, final RulTemplate rulTemplate,
                                  final InputStream in) throws IOException {
        change = createChange(userId);

        ArrOutputResult outputResult = createOutputResult(arrOutputDefinition, rulTemplate);
        outputResultRepository.save(outputResult);

        ArrOutputFile dmsFile = createDmsFile(arrOutputDefinition, outputResult);
        dmsService.createFile(dmsFile, in); // zajistí prezentaci výstupu na klienta
    }

    private ArrOutputFile createDmsFile(final ArrOutputDefinition arrOutputDefinition, final ArrOutputResult outputResult) {
        ArrOutputFile dmsFile = new ArrOutputFile();
        dmsFile.setOutputResult(outputResult);
        dmsFile.setFileName(arrOutputDefinition.getName() + "." + extension);
        dmsFile.setName(arrOutputDefinition.getName());
        dmsFile.setMimeType(mimeType);
        dmsFile.setFileSize(0); // 0 - zajistí refresh po skutečném uložení do souboru na disk
        return dmsFile;
    }

    private ArrOutputResult createOutputResult(final ArrOutputDefinition arrOutputDefinition,
            final RulTemplate rulTemplate) {
        ArrOutputResult outputResult = new ArrOutputResult();

        outputResult.setChange(change);
        outputResult.setOutputDefinition(arrOutputDefinition);
        outputResult.setTemplate(rulTemplate);

        return outputResult;
    }

    /**
     * @deprecated method only for debug template - DO NOT USE IN PRODUCTION CODE
     */
    @Deprecated
    private void storeOutputOnDisk(final InputStream in) {
        try {
            final FileOutputStream fos = new FileOutputStream("/tmp/output.pdf");
            IOUtils.copy(in, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * vytvoří změnu potřebnou pro uložení výstupu
     *
     * @param userId uživatel který spustil generování výstupu
     * @return vytvořená změna
     */
    protected ArrChange createChange(final Integer userId) {
        return bulkActionService.createChange(userId);
    }

    public Integer getArrOutputId() {
        return arrOutputId;
    }

    public ArrChange getChange() {
        return change;
    }

    protected File getTemplate(final RulTemplate rulTemplate, final String templateName) {
        File templateDir = getTemplatesDir(rulTemplate);

        final File mainTemplate = Paths.get(templateDir.getAbsolutePath(), templateName).toFile();
        Assert.isTrue(mainTemplate.exists(), "Nepodařilo se najít definici hlavní šablony.");

        return mainTemplate;
    }

    protected File getTemplatesDir(final RulTemplate rulTemplate) {
        final String rulTemplateDirectory = rulTemplate.getDirectory();
        final File templateDir = Paths.get(templatesDir, rulTemplateDirectory).toFile();
        Assert.isTrue(templateDir.exists() && templateDir.isDirectory(), "Nepodařilo se najít adresář s definicí šablony: " + templateDir.getAbsolutePath());
        return templateDir;
    }
}
