package cz.tacr.elza.service.output;

import com.google.common.collect.Sets;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.exception.ProcessException;
import cz.tacr.elza.print.OutputImpl;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DmsService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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
    private NodeOutputRepository nodeOutputRepository;

    @Autowired
    private OutputGeneratorService outputGeneratorService;

    @Autowired
    protected DmsService dmsService;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    @Autowired
    private ArrangementService arrangementService;

    protected Thread generatorThread;

    private Throwable exception;

    private Integer arrOutputId;
    private Integer userId;
    private ArrChange change;
    private String extension;
    private String mimeType;

    public void init(final Integer outputInProgress, final Integer userId, final RulTemplate rulTemplate) {
        Assert.notNull(rulTemplate, "Šablona musí být vyplněna");

        this.arrOutputId = outputInProgress;
        this.userId = userId;
        this.extension = rulTemplate.getExtension();
        this.mimeType = rulTemplate.getMimeType();
    }

    @Override
    public OutputGeneratorWorkerAbstract call() throws Exception {
        (new TransactionTemplate(txManager)).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                generateOutput();
            }
        });

        (new TransactionTemplate(txManager)).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                try {
                    ArrOutput arrOutput = outputRepository.findOne(arrOutputId);
                    ArrOutputDefinition arrOutputDefinition = outputDefinitionRepository.findByOutputId(arrOutput.getOutputId());
                    List<ArrNodeOutput> nodesList = nodeOutputRepository.findByOutputDefinition(arrOutputDefinition);
                    ArrOutputDefinition.OutputState outputState;
                    if (change != null) {
                        Map<ArrChange, Boolean> arrChangeBooleanMap = arrangementService.detectChangeNodes(
                                nodesList.stream().
                                        map(ArrNodeOutput::getNode).
                                        collect(Collectors.toSet()),
                                Sets.newHashSet(change), false, true);

                        if (arrChangeBooleanMap.containsKey(change) && arrChangeBooleanMap.get(change)) {
                            outputState = ArrOutputDefinition.OutputState.OUTDATED;
                        } else {
                            outputState = ArrOutputDefinition.OutputState.FINISHED;
                        }
                        arrOutputDefinition.setError(null);
                    } else {
                        outputState = ArrOutputDefinition.OutputState.OPEN;
                    }

                    arrOutputDefinition.setState(outputState);
                    outputDefinitionRepository.save(arrOutputDefinition);

                    outputGeneratorService.publishOutputStateEvent(arrOutputDefinition, null);

                    logger.info("Generování výstupu pro arr_output id=" + arrOutputId + " dokončeno úspěšně.", arrOutputId);
                } catch (Throwable ex) {
                    throw new ProcessException(arrOutputId, ex);
                }
            }
        });
        return this;
    }

    /**
     * Společná část generování výstupu.
     */
    private void generateOutput() {
        logger.info("Spuštěno generování výstupu pro arr_output id={}", arrOutputId);

        final ArrOutput arrOutput = outputRepository.findOne(arrOutputId);
        final ArrOutputDefinition arrOutputDefinition = outputDefinitionRepository.findByOutputId(arrOutput.getOutputId());
        change = createChange(userId);

        try {
            final RulTemplate rulTemplate = arrOutputDefinition.getTemplate();
            Assert.notNull(rulTemplate, "Výstup nemá definovanou šablonu (ArrOutputDefinition.template je null).");

            // sestavení outputu
            logger.info("Sestavování modelu výstupu výstupu pro arr_output id={} spuštěno", arrOutputId);
            final OutputImpl output = outputFactoryService.createOutput(arrOutput);
            logger.info("Sestavování modelu výstupu výstupu pro arr_output id={} dokončeno", arrOutputId);

            // skutečné vytvoření výstupného souboru na základě definice
            logger.info("Spuštěno generování souboru pro arr_output id={}", arrOutputId);
            final InputStream content = getContent(arrOutputDefinition, rulTemplate, output);

            // Uložení do výstupní struktury a DMS
            storeOutputInDms(arrOutputDefinition, rulTemplate, content);

            content.close();

            waitForGeneratorThread();

            if (exception != null) {
                throw exception;
            }

            arrOutputDefinition.setError(null);
        } catch (Throwable ex) {
            throw new ProcessException(arrOutputId, ex);
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
    protected abstract InputStream getContent(ArrOutputDefinition arrOutputDefinition, RulTemplate rulTemplate, OutputImpl output);

    /**
     * zajistí uložení výstupu do DB a DMS.
     */
    private void storeOutputInDms(final ArrOutputDefinition arrOutputDefinition, final RulTemplate rulTemplate,
                                  final InputStream in) throws IOException {
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
        final File templateDir = Paths.get(outputGeneratorService.getTemplatesDir(rulTemplate.getPackage().getCode(), rulTemplate.getOutputType().getRuleSet().getCode()), rulTemplateDirectory).toFile();
        Assert.isTrue(templateDir.exists() && templateDir.isDirectory(), "Nepodařilo se najít adresář s definicí šablony: " + templateDir.getAbsolutePath());
        return templateDir;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public void waitForGeneratorThread() throws InterruptedException {
        if (generatorThread != null) {
            generatorThread.join();
        }
    }
}
