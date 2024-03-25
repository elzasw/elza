package cz.tacr.elza.service.output.generator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.context.ApplicationContext;

import cz.tacr.elza.config.export.ExportConfig;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.fund.FundTreeProvider;
import cz.tacr.elza.print.AttPagePlaceHolder;
import cz.tacr.elza.print.OutputContext;
import cz.tacr.elza.print.OutputModel;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.StructObjService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.output.OutputParams;
import cz.tacr.elza.service.output.generator.PdfAttProvider.Attachments;
import jakarta.persistence.EntityManager;
// import net.sf.jasperreports.engine.DefaultJasperReportsContext;
// import net.sf.jasperreports.engine.JREmptyDataSource;
// import net.sf.jasperreports.engine.JRException;
// import net.sf.jasperreports.engine.JasperCompileManager;
// import net.sf.jasperreports.engine.JasperFillManager;
// import net.sf.jasperreports.engine.JasperPrint;
// import net.sf.jasperreports.engine.JasperReport;
// import net.sf.jasperreports.engine.export.JRPdfExporter;
// import net.sf.jasperreports.export.SimpleExporterInput;
// import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
// import net.sf.jasperreports.export.SimplePdfReportConfiguration;

public class JasperOutputGenerator extends DmsOutputGenerator {

    private static final String TEMPLATE_EXTENSION = ".jrxml";
    private static final String MAIN_TEMPLATE_NAME = "index" + TEMPLATE_EXTENSION;

    /**
     * Maximum memory per PDF file
     */
    public static final int MAX_PDF_MAIN_MEMORY_BYTES = 10 * 1024 * 1024;

    private final OutputModel outputModel;

    // SimplePdfReportConfiguration pdfExpConfig = new SimplePdfReportConfiguration();
    private PdfAttProvider pdfAttProvider;

    JasperOutputGenerator(ApplicationContext applicationContext, StaticDataService staticDataService,
                          ElzaLocale elzaLocale,
                          FundRepository fundRepository,
                          FundTreeProvider fundTreeProvider,
                          NodeCacheService nodeCacheService,
                          InstitutionRepository institutionRepository,
                          ApStateRepository apStateRepository,
                          ApBindingRepository bindingRepository,
                          ApItemRepository itemRepository,
                          ApBindingStateRepository bindingStateRepository,
                          ApIndexRepository indexRepository,
                          EntityManager em,
                          DmsService dmsService,
                          DaoLinkRepository daoLinkRepository,
                          ExportConfig exportConfig,
                          StructObjService structObjService) {
        super(em, dmsService);

        StructuredObjectRepository structObjRepos = applicationContext.getBean(StructuredObjectRepository.class);
        StructuredItemRepository structItemRepos = applicationContext.getBean(StructuredItemRepository.class);
        OutputContext outputContext = applicationContext.getBean(OutputContext.class);

        pdfAttProvider = new PdfAttProvider(applicationContext);
        outputModel = new OutputModel(outputContext, staticDataService, elzaLocale,
                fundRepository, fundTreeProvider,
                nodeCacheService, institutionRepository,
                apStateRepository, bindingRepository,
                pdfAttProvider, structObjRepos, structItemRepos, itemRepository, bindingStateRepository,
                indexRepository, daoLinkRepository, exportConfig, structObjService, em);
        pdfAttProvider.setOutput(outputModel);
    }

    @Override
    public void init(OutputParams params) {
        super.init(params);
        outputModel.init(params);

        // Parse PDF settings
        //        String outputSettings = params.getOutput().getOutputSettings();
        //        if (StringUtils.isNotBlank(outputSettings)) {
        //            ObjectMapper mapper = new ObjectMapper();
        //            OutputSettingsVO settingsVO;
        //            try {
        //                settingsVO = mapper.readValue(outputSettings, OutputSettingsVO.class);
        //            } catch (Exception e) {
        //                throw new BusinessException("Failed to parse configuration", e, BaseCode.JSON_PARSE)
        //                        .set("settings", outputSettings);
        //            }
        //            pdfExpConfig.setEvenPageOffsetX(settingsVO.getEvenPageOffsetX());
        //            pdfExpConfig.setEvenPageOffsetY(settingsVO.getEvenPageOffsetY());
        //            pdfExpConfig.setOddPageOffsetX(settingsVO.getOddPageOffsetX());
        //            pdfExpConfig.setOddPageOffsetY(settingsVO.getOddPageOffsetY());
        //        }
        //
        //        // switch line break algorithm - problem with missing last row
        //        pdfExpConfig.setForceLineBreakPolicy(true);

    }
    @Override
    protected void generate(OutputStream os) throws IOException {
        Path templateFile = params.getTemplateDir().resolve(MAIN_TEMPLATE_NAME);

        // prepare parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("output", outputModel);
        parameters.put("fund", outputModel.getFund());

        // JasperReport report = loadTemplate(templateFile);

        // prepareSubreports(parameters);

        // Path partialResult = generatePdfFile(report, parameters);

        // mergePDFAndAttachments(partialResult, os);
    }

    private void prepareSubreports(Map<String, Object> parameters) throws IOException {
        //        try(Stream<Path> filePathStream = Files.list(params.getTemplateDir())) {
        //            // Filter templates
        //            filePathStream.filter(path -> {
        //            String name = path.getFileName().toString();
        //            return name.endsWith(TEMPLATE_EXTENSION) && !name.equals(MAIN_TEMPLATE_NAME);
        //
        //            }).forEach(path -> {
        //                // load templates
        //                String subreportName = path.getFileName().toString();
        //                subreportName = subreportName.substring(0, subreportName.length() - TEMPLATE_EXTENSION.length());
        //                JasperReport subreport = loadTemplate(path);
        //                parameters.put(subreportName, subreport);
        //        });
        //        }

    }

    //    private JasperReport loadTemplate(Path templateFile) {
    //                try (InputStream is = Files.newInputStream(templateFile, StandardOpenOption.READ)) {
    //                    return JasperCompileManager.compileReport(is);
    //                } catch (IOException | JRException e) {
    //                    throw new ProcessException(params.getOutputId(), "Failed to parse Jasper template: " + templateFile, e)
    //                            .set("template", templateFile);
    //                }
    //        return null;
    //    }

    //    private Path generatePdfFile(JasperReport report, Map<String, Object> parameters) {
    //
    //        DefaultJasperReportsContext jasperContext = DefaultJasperReportsContext.getInstance();
    //        JasperFillManager fillManager = JasperFillManager.getInstance(jasperContext);
    //
    //        JasperPrint jasperPrint;
    //        try {
    //            jasperPrint = fillManager.fill(report, parameters, new JREmptyDataSource());
    //        } catch (JRException e) {
    //            throw new ProcessException(params.getOutputId(), "Failed to create Jasper document", e);
    //        }
    //
    //        Path pdfFile = tempFileProvider.createTempFile();
    //
    //        try (OutputStream os = Files.newOutputStream(pdfFile, StandardOpenOption.WRITE)) {
    //
    //            // Generate output to PDF
    //            JRPdfExporter exporter = new JRPdfExporter(jasperContext);
    //
    //            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
    //            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(os));
    //            exporter.setConfiguration(pdfExpConfig);
    //
    //            exporter.exportReport();
    //
    //            // JasperExportManager.exportReportToPdfStream(print, os);
    //        } catch (IOException | JRException e) {
    //            throw new ProcessException(params.getOutputId(), "Failed to generate PDF from Jasper document", e);
    //        }
    //
    //        return pdfFile;
    //    }

    //    private void mergePDFAndAttachments(Path pdfFile, OutputStream os) throws IOException {
    //        // check if has attachments
    //        int pageCnt = this.pdfAttProvider.getTotalPageCnt();
    //        if (pageCnt == 0) {
    //            Files.copy(pdfFile, os);
    //            return;
    //        }
    //        // Collection of attachments to be replaced
    //        List<Attachments> attachments = new ArrayList<>(pdfAttProvider.getAttachments());
    //
    //        try (
    //                // Load generated PDF
    //                final PDDocument inDoc = PDDocument.load(pdfFile.toFile(),
    //                                                         MemoryUsageSetting.setupMixed(MAX_PDF_MAIN_MEMORY_BYTES));
    //                // Create new final PDF
    //                final PDDocument outDoc = new PDDocument(MemoryUsageSetting.setupMixed(MAX_PDF_MAIN_MEMORY_BYTES));) {
    //
    //            // Merge attachments
    //            mergeOutput(inDoc, outDoc, attachments);
    //
    //            // Copy bookmarks
    //            copyBookmarks(inDoc, outDoc);
    //
    //            outDoc.save(os);
    //        }
    //
    //    }

    private void copyBookmarks(PDDocument inDoc, PDDocument outDoc) throws IOException {
        PDDocumentOutline inOutline = inDoc.getDocumentCatalog().getDocumentOutline();
        if (inOutline == null) {
            // noting to cpoy
            return;
        }
        PDDocumentOutline outOutline = new PDDocumentOutline();
        outDoc.getDocumentCatalog().setDocumentOutline(outOutline);

        copyBookmarks(inOutline, outOutline);
    }

    private void copyBookmarks(PDDocumentOutline inOutline, PDDocumentOutline outOutline) throws IOException {
        // iterate source outline
        for (PDOutlineItem inItem : inOutline.children()) {
            copyBookmark(inItem, outOutline);
        }

    }

    PDOutlineItem copyBookmark(PDOutlineItem inItem) throws IOException {
        PDOutlineItem outItem = new PDOutlineItem();
        outItem.setTitle(inItem.getTitle());
        PDDestination inDest = inItem.getDestination();
        outItem.setDestination(inDest);

        // Copy child items
        if (inItem.hasChildren()) {
            PDOutlineItem child = inItem.getFirstChild();
            while (child != null) {
                PDOutlineItem copiedChild = copyBookmark(child);
                outItem.addLast(copiedChild);
                child = child.getNextSibling();
            }
        }

        return outItem;
    }

    private void copyBookmark(PDOutlineItem inItem, PDDocumentOutline outOutline) throws IOException {
        PDOutlineItem outItem = copyBookmark(inItem);
        outOutline.addLast(outItem);
    }

    /**
     * Provede vložení PDF příloh do výstupního dokumentu
     *
     * @param inDoc
     *            Vstupní PDF, generované jasperem
     * @param outDoc
     *            finální výstupní PDF (předávané dál do DMS)
     * @param attachmentsIn
     *            seznam PDF příloh
     * @throws IOException
     */
    private void mergeOutput(PDDocument inDoc, PDDocument outDoc, final List<Attachments> attachmentsIn)
            throws IOException {

        // List of attachments - will be reduced
        List<Attachments> attachments = new ArrayList<>(attachmentsIn);

        int skipCnt = 0;

        for (PDPage pdPage : inDoc.getPages()) {
            // new page -> ?should be skipped
            if (skipCnt > 0) {
                skipCnt--;
                continue;
            }
            // we received other page -> check if some attachment
            if (attachments.size() > 0) {
                Attachments replaceWithAtts = findReplaceWith(pdPage, attachments);
                if (replaceWithAtts != null) {
                    attachments.remove(replaceWithAtts);

                    skipCnt = replaceWithAtts.getPagePlaceHolders().size() - 1;

                    // add all pages from attachments
                    replaceWithAtts.addAllPages(outDoc);
                    continue;
                }
            }
            // Nothing to replace -> copy page to output
            outDoc.addPage(pdPage);
        }

        // All attachment should be merged
        Validate.isTrue(attachments.size() == 0);

    }

    /**
     * Find attachment which should replace this page
     *
     * @param pdPage
     * @param attachmentsCol
     *            Collection of attachments
     * @return
     * @throws IOException
     */
    private Attachments findReplaceWith(PDPage pdPage, List<Attachments> attachmentsCol) throws IOException {
        PDFTextStripper pdfStripper = new PDFTextStripper();
        try (final PDDocument tmpDoc = new PDDocument()) {
            tmpDoc.addPage(pdPage);
            String text = pdfStripper.getText(tmpDoc);

            // check if some attachment is here
            for (Attachments atts : attachmentsCol) {
                List<AttPagePlaceHolder> placeHolders = atts.getPagePlaceHolders();
                if (placeHolders.size() > 0) {
                    // get first place holder and check
                    AttPagePlaceHolder placeHolder = placeHolders.get(0);

                    if (text.startsWith(placeHolder.getAttPage())) {
                        return atts;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        if (pdfAttProvider != null) {
            pdfAttProvider.close();
            pdfAttProvider = null;
        }

        // TODO:
        /*
        if (this.outputModel != null) {
            outputModel.close();
        }
        */
    }
}
