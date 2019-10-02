package cz.tacr.elza.service.attachment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.CloseablePathResource;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.ProcessService;
import cz.tacr.elza.utils.TempDirectory;

/**
 * Servisní třída pro práci s přílohami.
 *
 */
@Service
public class AttachmentService {
    private static final Logger logger = LoggerFactory.getLogger(AttachmentService.class);

    private static final long MAX_PROCESS_TIMEOUT = 5L * 60 * 1000;

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
     *
     * @param dmsFile        dms file
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
     * Command parameter formatter
     */
    private static class CmdParamFormatter {
        final private String workDir;
        final private String inputFileName;
        final private String inputFilePath;
        final private String outputFileName;
        final private String outputFilePath;

        public CmdParamFormatter(final String workDir, final String inputFileName, final String inputFilePath,
                final String outputFileName, final String outputFilePath) {
            this.workDir = workDir;
            this.inputFileName = inputFileName;
            this.inputFilePath = inputFilePath;
            this.outputFileName = outputFileName;
            this.outputFilePath = outputFilePath;
        }

        public CmdParamFormatter(Path workDirPath, Path inputFile, Path outputFile) {
            this.workDir = workDirPath.toString();
            this.inputFileName = inputFile.getFileName().toString();
            this.inputFilePath = inputFile.toString();
            this.outputFileName = outputFile.getFileName().toString();
            this.outputFilePath = outputFile.toString();
        }

        /**
         * Formátování řetězce s podporou použití proměnných jako ja např. název input
         * souboru apod.
         *
         * @param text
         *            text
         * @return výstupní text
         */
        String formatString(final String text) {
            return MessageFormat.format(
                                        text,
                                        workDir,
                                        inputFileName,
                                        inputFilePath,
                                        outputFileName,
                                        outputFilePath);

        }
    }

    /**
     * Provede generování souboru do výstupu podle konfigurace (mime type).
     *
     * @param dmsFile vstupní soubor
     * @return vygenerovaný pdf soubor, pokud je null, není podpora pro převod vstupního souboru do požadovaného výstupu
     */
    public CloseablePathResource generate(final DmsFile dmsFile, final String outputMimeType) {
        logger.info("Generuje se pdf soubor pro dms id {}", dmsFile.getFileId());

        Path origFilePath = dmsService.getFilePath(dmsFile);
        // Pokud je již pdf, jen se vrátí
        if (dmsFile.getMimeType().equalsIgnoreCase(outputMimeType)) {
            return new CloseablePathResource(origFilePath);
        }

        // Pokud není podpora, vrátí se null
        AttachmentConfig.MimeDef mimeDef = attachmentConfig.findMimeDef(dmsFile.getMimeType());
        if (mimeDef == null) {
            throw new SystemException("Cannot find mimetype in attachment configuration", BaseCode.PROPERTY_NOT_EXIST)
                    .set("mime-type", dmsFile.getMimeType());
        }
        AttachmentConfig.Generator generator = mimeDef.findGenerator(outputMimeType);
        if (generator == null) {
            throw new SystemException("Cannot find suitable generator in attachment configuration",
                    BaseCode.PROPERTY_NOT_EXIST)
                            .set("originalMimeType", dmsFile.getMimeType())
                            .set("targetMimeType", outputMimeType);
        }

        // Vytvoření pracovního adresáře
        TempDirectory tempDir = new TempDirectory("elza-generate");
        logger.info("Pracovní adresář: {}", tempDir.getPath().toString());

        Path inputFile = tempDir.getPath().resolve("input.dat");
        Path outputFile = tempDir.getPath().resolve(generator.getOutputFileName());

        try {
            // copy source file as inputFile
            Files.copy(origFilePath, inputFile);

            // Spuštění procesu - příprava parametrů
            final String sourceCommand = generator.getCommand();
            CmdParamFormatter cpf = new CmdParamFormatter(tempDir.getPath(), inputFile, outputFile);
            String command = cpf.formatString(sourceCommand);

            Process process = Runtime.getRuntime().exec(command, null, tempDir.getPath().toFile());

            // Zpracování běhu procesu a čekání na jeho dokončení
            processService.process(process, MAX_PROCESS_TIMEOUT);

            if (!Files.exists(outputFile)) {
                throw new IOException("Generování nevytvořilo výstupní soubor " + outputFile.toString());
            }

            final CloseablePathResource cpr = new CloseablePathResource(outputFile) {
                @Override
                public void close() throws IOException {
                    super.close();
                    // Pokud již přečetl, chceme adresář smazat
                    tempDir.delete();
                }
            };

            return cpr;
        } catch (Exception ex) {
            tempDir.delete();
            throw new BusinessException("Chyba generování výstupního souboru", ex, BaseCode.INVALID_STATE);
        }
    }
}
