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
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

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
    protected DmsService dmsService;

    @Value("${elza.templates.templatesDir}")
    protected String templatesDir;

    private Integer arrOutputId;
    private Integer userId;
    private ArrChange change;

    public void init(Integer outputInProgress, Integer userId) {
        this.arrOutputId = outputInProgress;
        this.userId = userId;
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
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ArrOutput arrOutput = outputRepository.findOne(arrOutputId);
        final ArrOutputDefinition arrOutputDefinition = arrOutput.getOutputDefinition();
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
        try {
            storeOutputInDms(arrOutputDefinition, rulTemplate, content, getOutfileSuffix(), getMimeType());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return příponu vygenerovaného souboru na základě zvolené šablony
     */
    protected abstract String getOutfileSuffix();

    /**
     * @return mimetype vygenerovaného souboru na základě zvolené šablony
     */
    protected abstract String getMimeType();

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
    private void storeOutputInDms(ArrOutputDefinition arrOutputDefinition, RulTemplate rulTemplate,
                                  InputStream in, final String outfileSuffix, final String mimeType) throws IOException {
        change = createChange(userId);

        ArrOutputResult outputResult = new ArrOutputResult();
        outputResult.setChange(change);
        outputResult.setOutputDefinition(arrOutputDefinition);
        outputResult.setTemplate(rulTemplate);
        outputResultRepository.save(outputResult);

        ArrOutputFile dmsFile = new ArrOutputFile();
        dmsFile.setOutputResult(outputResult);
        dmsFile.setFileName(arrOutputDefinition.getName() + outfileSuffix);
        dmsFile.setName(arrOutputDefinition.getName());
        dmsFile.setMimeType(mimeType);
        dmsFile.setFileSize(0); // 0 - zajistí refresh po skutečném uložení do souboru na disk
        dmsService.createFile(dmsFile, in); // zajistí prezentaci výstupu na klienta
    }

    /**
     * @deprecated method only for debug template - DO NOT USE IN PRODUCTION CODE
     */
    @Deprecated
    private void storeOutputOnDisk(InputStream in) {
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

    Integer getArrOutputId() {
        return arrOutputId;
    }

    public ArrChange getChange() {
        return change;
    }
}
