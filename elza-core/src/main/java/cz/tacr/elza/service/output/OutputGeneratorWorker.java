package cz.tacr.elza.service.output;

import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.item.ItemFile;
import cz.tacr.elza.print.party.DummyDetail;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.service.DmsService;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    // konstanty pro pojmenování souborů šablon
    private static final String MAIN_TEMPLATE_BASE_NAME = "index";
    private static final String JASPER_TEMPLATE_SUFFIX = ".jrxml";
    private static final String FREEMARKER_TEMPLATE_SUFFIX = ".ftl";
    private static final String JASPER_MAIN_TEMPLATE = MAIN_TEMPLATE_BASE_NAME + JASPER_TEMPLATE_SUFFIX;
    private static final String FREEMARKER_MAIN_TEMPLATE = MAIN_TEMPLATE_BASE_NAME + FREEMARKER_TEMPLATE_SUFFIX;
    private static final String OUTFILE_SUFFIX_PDF = ".pdf";
    private static final String OUTFILE_SUFFIX_CVS = ".cvs";
    public static final int MAX_MERGE_MAIN_MEMORY_BYTES = 100 * 1024 * 1024;

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
    private Integer userId;

    private ArrChange change;

    public void init(Integer outputInProgress, Integer userId) {
        this.arrOutputId = outputInProgress;
        this.userId = userId;
    }

    @Override
    @Transactional
    public OutputGeneratorWorker call() throws Exception {
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
        } else if (RulTemplate.Engine.FREEMARKER.equals(rulTemplate.getEngine())) {
            generateCvsByFreemarker(arrOutputDefinition, rulTemplate, output);
            // dokončení generování je logováno v service onSucces/onFailure
        }
    }

    /**
     * část generování specifická pro freemarker
     */
    private void generateCvsByFreemarker(ArrOutputDefinition arrOutputDefinition, RulTemplate rulTemplate, Output output) {
        try {
            // dohledání šablony
            final String rulTemplateDirectory = rulTemplate.getDirectory();
            final File templateDir = Paths.get(templatesDir, rulTemplateDirectory).toFile();
            Assert.isTrue(templateDir.exists() && templateDir.isDirectory(), "Nepodařilo se najít adresář s definicí šablony: " + templateDir.getAbsolutePath());

            final File mainFreemarkerTemplate = Paths.get(templateDir.getAbsolutePath(), FREEMARKER_MAIN_TEMPLATE).toFile();
            Assert.isTrue(mainFreemarkerTemplate.exists(), "Nepodařilo se najít definici hlavní šablony.");

            Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
            // Export do výstupu - pouze příprava procesu pro renderování - reálně proběhne až při čtení z in v dms
            PipedReader in = new PipedReader();
            PipedWriter out = new PipedWriter(in);

            // inicializace
            FileTemplateLoader templateLoader = new FileTemplateLoader(mainFreemarkerTemplate.getParentFile());
            cfg.setTemplateLoader(templateLoader);
            Template template = cfg.getTemplate(mainFreemarkerTemplate.getName());

            // příparava dat
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("output", output);

            new Thread(
                    new Runnable() {
                        public void run() {
                            try {
                                template.process(parameters, out);
                                out.close();
                            } catch (TemplateException | IOException e) {
                                throw new IllegalStateException("Nepodařilo se vyrenderovat výstup ze šablony " + mainFreemarkerTemplate.getAbsolutePath() + ".", e);
                            }
                        }
                    }
            ).start();

            // Uložení do výstupní struktury a DMS
            storeOutputInDms(arrOutputDefinition, rulTemplate, new ReaderInputStream(in, Charset.defaultCharset()), OUTFILE_SUFFIX_CVS, DmsService.MIME_TYPE_TEXT_CVS);

        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se uložit výstup.", e);
        }
    }

    /**
     * část generování specifická pro jasper.
     */
    private void generatePdfByJasper(ArrOutputDefinition arrOutputDefinition, RulTemplate rulTemplate, Output output) {
        try {
            // dohledání šablony
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
//            JRDataSource dataSource = new JREmptyDataSource();
            JRDataSource dataSource = new JRBeanCollectionDataSource(Collections.singletonList(new DummyDetail()));
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // Export to PDF - pouze příprava procesu pro renderování - reálně proběhne až při čtení z in v dms
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in);
            new Thread(() -> {
                try {
                    JasperExportManager.exportReportToPdfStream(jasperPrint, out);
                    out.close();
                } catch (JRException | IOException e) {
                    throw new IllegalStateException("Nepodařilo se vyrenderovat PDF ze šablony " + mainJasperTemplate.getAbsolutePath() + ".", e);
                }
            }).start();

            // připojení PDF příloh
            PipedInputStream inm = new PipedInputStream();
            PipedOutputStream outm = new PipedOutputStream(inm);
            new Thread(() -> {
                PDFMergerUtility ut = new PDFMergerUtility();
                ut.addSource(in);
                final List<ItemFile> attachements = output.getAttachements();
                attachements.stream()
                        .forEach(itemFile -> {
                            File file = itemFile.getFile();
                            if (file == null) { // obezlička, protože arrFile ten file nevrací
                                final DmsFile dmsFile = dmsService.getFile(itemFile.getFileId());
                                final String filePath = dmsService.getFilePath(dmsFile);
                                file = new File(filePath);
                            }

                            try {
                                ut.addSource(file);
                            } catch (FileNotFoundException e) {
                                throw new IllegalStateException(e);
                            }
                        });
                ut.setDestinationStream(outm);
                try {
                    ut.mergeDocuments(MemoryUsageSetting.setupMixed(MAX_MERGE_MAIN_MEMORY_BYTES));
                    outm.close();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }).start();

            // Uložení do výstupní struktury a DMS
            storeOutputInDms(arrOutputDefinition, rulTemplate, in, OUTFILE_SUFFIX_PDF, DmsService.MIME_TYPE_APPLICATION_PDF);

        } catch (JRException e) {
            throw new IllegalStateException("Nepodařilo se vytisknout report.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se uložit výstup.", e);
        }
    }

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
