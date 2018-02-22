package cz.tacr.elza.service.attachment;

import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.security.ApplicationSecurity;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.ProcessService;
import cz.tacr.elza.utils.NamedInputStreamResource;
import cz.tacr.elza.utils.TempDirectory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servisní třída pro práci s přílohami.
 *
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 09.11.2017
 */
@Service
public class AttachmentService {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationSecurity.class);

    @Autowired
    private DmsService dmsService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private AttachmentConfig attachmentConfig;

    /**
     * Seznam registrovaných editovatelných mime typů
     *
     * @return seznam registrovaných editovatelných mime typů
     */
    public List<String> getEditableMimeTypes() {
        final List<String> result = new ArrayList<>();
        for (AttachmentConfig.MimeDef mimeDef : attachmentConfig.getMimeDefs()) {
            if (mimeDef.getEditable()) {
                result.add(mimeDef.getMimeType());
            }
        }
        return result;
    }

    /**
     * Test, zda jde předný soubor vygenerovat do požadovaného typu výstupu.
     * @param dmsFile dms file
     * @param outputMimeType požadovaný výstup
     * @return true, poukd lze
     */
    public boolean supportGenerateTo(final DmsFile dmsFile, final String outputMimeType) {
        // Pokud není podpora, vrátí se null
        AttachmentConfig.MimeDef mimeDef = attachmentConfig.findMimeDef(dmsFile.getMimeType());
        if (mimeDef == null) {
            return false;
        }
        AttachmentConfig.Generator generator = mimeDef.findGenerator(outputMimeType);
        if (generator == null) {
            return false;
        }

        return true;
    }

    /**
     * Zjištění, zda daný typ je editovatelnýv aplikací - jako prostý editovatelný text.
     *
     * @param mimeType mime type
     * @return true, pokud je daný typ editovatelný
     */
    public boolean isEditable(final String mimeType) {
        return getEditableMimeTypes().contains(mimeType);
    }

    /**
     * Metadata pro parametrizované formátování command.
     */
    private static class FormatMeta {
        private String inputFileName;
        private String inputFileExtension;
        private String inputFileFilename;
        private String inputFileFullPath;
        private String outputFileName;
        private String outputFileExtension;
        private String outputFileFilename;
        private String outputFileFullPath;

        public FormatMeta(final String inputFileName, final String inputFileExtension, final String inputFileFilename, final String inputFileFullPath) {
            this.setInputFileName(inputFileName);
            this.setInputFileExtension(inputFileExtension);
            this.setInputFileFilename(inputFileFilename);
            this.setInputFileFullPath(inputFileFullPath);
        }

        public String getInputFileName() {
            return inputFileName;
        }

        public void setInputFileName(String inputFileName) {
            this.inputFileName = inputFileName;
        }

        public String getInputFileExtension() {
            return inputFileExtension;
        }

        public void setInputFileExtension(String inputFileExtension) {
            this.inputFileExtension = inputFileExtension;
        }

        public String getInputFileFilename() {
            return inputFileFilename;
        }

        public void setInputFileFilename(String inputFileFilename) {
            this.inputFileFilename = inputFileFilename;
        }

        public String getInputFileFullPath() {
            return inputFileFullPath;
        }

        public void setInputFileFullPath(String inputFileFullPath) {
            this.inputFileFullPath = inputFileFullPath;
        }

        public String getOutputFileName() {
            return outputFileName;
        }

        public void setOutputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
        }

        public String getOutputFileExtension() {
            return outputFileExtension;
        }

        public void setOutputFileExtension(String outputFileExtension) {
            this.outputFileExtension = outputFileExtension;
        }

        public String getOutputFileFilename() {
            return outputFileFilename;
        }

        public void setOutputFileFilename(String outputFileFilename) {
            this.outputFileFilename = outputFileFilename;
        }

        public String getOutputFileFullPath() {
            return outputFileFullPath;
        }

        public void setOutputFileFullPath(String outputFileFullPath) {
            this.outputFileFullPath = outputFileFullPath;
        }
    }

    /**
     * Formátování řetězce s podporou použití proměnných jako ja např. název input souboru apod.
     * @param text text
     * @param formatMeta metadata
     * @return výstupní text
     */
    private String formatString(final String text, final FormatMeta formatMeta) {
        return MessageFormat.format(
                text,
                formatMeta.getInputFileName(),
                formatMeta.getInputFileExtension(),
                formatMeta.getInputFileFilename(),
                formatMeta.getInputFileFullPath(),
                formatMeta.getOutputFileName(),
                formatMeta.getOutputFileExtension(),
                formatMeta.getOutputFileFilename(),
                formatMeta.getOutputFileFullPath()
        );
    }

    /**
     * Provede generování souboru do výstupu podle konfigurace (mime type).
     *
     * @param dmsFile vstupní soubor
     * @return vygenerovaný pdf soubor, pokud je null, není podpora pro převod vstupního souboru do požadovaného výstupu
     */
    public @Nullable
    Resource generate(final DmsFile dmsFile, final String outputMimeType) {
        logger.info("Generuje se pdf soubor pro dms id {}", dmsFile.getFileId());

        // Pokud je již pdf, jen se vrátí
        if (dmsFile.getMimeType().equalsIgnoreCase(outputMimeType)) {
            return new NamedInputStreamResource(dmsFile.getFileName(), dmsService.downloadFile(dmsFile));
        }

        // Pokud není podpora, vrátí se null
        AttachmentConfig.MimeDef mimeDef = attachmentConfig.findMimeDef(dmsFile.getMimeType());
        if (mimeDef == null) {
            return null;
        }
        AttachmentConfig.Generator generator = mimeDef.findGenerator(outputMimeType);
        if (generator == null) {
            return null;
        }

        // Vytvoření pracovního adresáře
        TempDirectory tempDir = new TempDirectory("elza-generate");
        File workingDir = tempDir.getPath().toFile();
        logger.info("Pracovní adresář: {}", workingDir.getAbsolutePath());

        try {
            final String inputFileName = "input";
            final String inputFileExtension = "dat";
            final String inputFileFilename = inputFileName + "." + inputFileExtension;
            File inputFile = new File(workingDir, inputFileFilename);
            FormatMeta formatMeta = new FormatMeta(inputFileName, inputFileExtension, inputFileFilename, inputFile.getAbsolutePath());

            final String outputFileName;
            final String outputFileExtension;
            final String outputFileFilename;
            if (StringUtils.isNotEmpty(generator.getOutputFileName())) {
                String value = formatString(generator.getOutputFileName(), formatMeta);
                int n = value.lastIndexOf('.');
                if (n > 0) {
                    String name = value.substring(0, n);
                    String extension = value.substring(n + 1);
                    outputFileName = name;
                    outputFileExtension = extension;
                    outputFileFilename = outputFileName + "." + outputFileExtension;
                } else {
                    outputFileName = value;
                    outputFileExtension = "";
                    outputFileFilename = outputFileName;
                }
            } else {
                outputFileName = "output";
                outputFileExtension = "dat";
                outputFileFilename = outputFileName + "." + outputFileExtension;
            }
            File outputFile = new File(workingDir, outputFileFilename);
            formatMeta.setOutputFileName(outputFileName);
            formatMeta.setOutputFileExtension(outputFileExtension);
            formatMeta.setOutputFileFilename(outputFileFilename);
            formatMeta.setOutputFileFullPath(outputFile.getAbsolutePath());


            // Připravení vstupního souboru pro generování
            try (InputStream is = dmsService.downloadFile(dmsFile); OutputStream os = new FileOutputStream(inputFile)) {
                IOUtils.copy(is, os);
            }

            // Spuštění procesu
            final String sourceCommand = generator.getCommand();
            String command = formatString(sourceCommand, formatMeta);
            Process process = Runtime.getRuntime().exec(command, null, workingDir);

            // Zpracování běhu procesu a čekání na jeho dokončení
            // TODO [stanekpa] - kam dáme tuto konfiguraci?
            processService.process(process, 5 * 60 * 1000);

            if (!outputFile.exists()) {
                throw new IOException("Generování nevytvořilo výstupní soubor " + outputFile.getAbsolutePath());
            }

            final InputStream is = new FileInputStream(outputFile) {
                @Override
                public void close() throws IOException {
                    super.close();
                    // Pokud již přečetl, chceme adresář smazat
                    tempDir.delete();
                }
            };

            String extension = getExtension(outputMimeType);
            return new NamedInputStreamResource(extension != null ? inputFileName + "." + extension : inputFileName, is);
        } catch (Exception ex) {
            tempDir.delete();
            throw new BusinessException("Chyba generování výstupního souboru", ex, BaseCode.INVALID_STATE);
        }
    }

    /**
     * Mapa mime type na příponu souboru.
     */
    static Map<String, String> mimeExtMap = new HashMap<>();
    static {
        mimeExtMap.put("application/pdf", "pdf");
    }

    /**
     * Načtení přípony podle mime type.
     * @param mimeType mime type
     * @return přípona nebo null
     */
    private @Nullable String getExtension(final String mimeType) {
        return mimeExtMap.get(mimeType);
    }
}
