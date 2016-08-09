package cz.tacr.elza.service.output;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.item.ItemFile;
import cz.tacr.elza.print.party.DummyDetail;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

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
    protected InputStream getContent(final ArrOutputDefinition arrOutputDefinition, final RulTemplate rulTemplate, final Output output) {
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
            addSubreports(templateDir, parameters);

            // DataSource
//            JRDataSource dataSource = new JREmptyDataSource();
            JRDataSource dataSource = new JRBeanCollectionDataSource(Collections.singletonList(new DummyDetail()));
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // Export to PDF - pouze příprava procesu pro renderování - reálně proběhne až při čtení z in v dms
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream out = new PipedOutputStream(in);
            new Thread(() -> renderJasperPdf(mainJasperTemplate, jasperPrint, out)).start();

            // připojení PDF příloh
            PipedInputStream inm = new PipedInputStream();
            PipedOutputStream outm = new PipedOutputStream(inm);
            new Thread(() -> mergePdfJasperAndAttachements(output, in, outm)).start();

            return inm;
        } catch (JRException e) {
            throw new IllegalStateException("Nepodařilo se vytisknout report.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se uložit výstup.", e);
        }
    }

    private void addSubreports(final File templateDir, final Map<String, Object> parameters) {
        final File[] files = templateDir.listFiles((dir, name) -> name.endsWith(JASPER_TEMPLATE_SUFFIX) && !name.equals(JASPER_MAIN_TEMPLATE));
        Arrays.stream(files).forEach(file -> {
            try {
                parameters.put(FilenameUtils.getBaseName(file.getAbsolutePath()), JasperCompileManager.compileReport(file.getAbsolutePath()));
            } catch (JRException e) {
                throw new IllegalStateException("Chyba kompilace subšablony reportu " + file.getAbsolutePath(), e);
            }
        });
    }

    private void mergePdfJasperAndAttachements(final Output output, final PipedInputStream in, final PipedOutputStream outm) {
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
    }

    private void renderJasperPdf(final File mainJasperTemplate, final JasperPrint jasperPrint, final PipedOutputStream out) {
        try {
            JasperExportManager.exportReportToPdfStream(jasperPrint, out);
            out.close();
        } catch (JRException | IOException e) {
            throw new IllegalStateException("Nepodařilo se vyrenderovat PDF ze šablony " + mainJasperTemplate.getAbsolutePath() + ".", e);
        }
    }


}
