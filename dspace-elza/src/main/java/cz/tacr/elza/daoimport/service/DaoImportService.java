package cz.tacr.elza.daoimport.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.stereotype.Service;

import cz.tacr.elza.daoimport.DaoImportScheduler;

@Service
public class DaoImportService {

    public static final String INPUT_DIR = "inputDir";
    public static final String ARCHIVE_DIR = "archDir";
    public static final String ERROR_DIR = "errorDir";
    public static final String GENERATED_DIR = "generated";

    public static final String BATCH_READY_FILE = "Konec.txt";
    public static final String PROTOCOL_FILE = "Proto.txt";

    public static final String METADATA_EXTENSION = ".meta";
    public static final String THUMBNAIL_EXTENSION = ".thumb";

    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    public void importDAOs() throws IOException {
        String mainDir = configurationService.getProperty("elza.daoimport.dir");
        Path inputDir = Paths.get(mainDir, INPUT_DIR);
        log.info("Hledání dávek v adresáři " + inputDir.toAbsolutePath());

        List<Path> possibleBatches = getBatchDirs(inputDir);

        for (Path batchDir : possibleBatches) {
            BufferedWriter protocol = createProtocolWriter(batchDir);

            boolean batchError = false;
            try {
                checkAndPrepareBatch(batchDir, protocol);
            } catch (Exception e) {
                protocol.write("Chyba při zpracování dávky: " + e.getMessage());
                batchError = true;
                protocol.close();

                Path errorDir = Paths.get(mainDir, ERROR_DIR, batchDir.getFileName().toString());
                Files.move(batchDir, errorDir);
            } finally {
                if (!batchError) {
                    protocol.write("Konec zpracování dávky: " + batchDir.toAbsolutePath());
                    protocol.close();

                    Path archiveDir = Paths.get(mainDir, ARCHIVE_DIR);
                    Files.move(batchDir, archiveDir);
                }
            }
        }
    }

    private BufferedWriter createProtocolWriter(Path batchDir) throws IOException {
        Path protocolPath = Paths.get(batchDir.toAbsolutePath().toString(), PROTOCOL_FILE);
        if (Files.exists(protocolPath)) {
            Files.delete(protocolPath);
        }
        Files.createFile(protocolPath);
        return Files.newBufferedWriter(protocolPath, Charset.forName("UTF-8"));
    }

    private void checkAndPrepareBatch(Path batchDir, BufferedWriter protocol) throws IOException {
        protocol.write("Kontrola a příprava dávky " + batchDir.toAbsolutePath());
        protocol.newLine();

        List<Path> daoDirs = new LinkedList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(batchDir)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    daoDirs.add(path);
                    protocol.write("Nalezené DAO " + path.getFileName());
                    protocol.newLine();
                }
            }
        }

        for (Path daoDir : daoDirs) {
            checkAndPrepareDao(daoDir, protocol);
        }
    }

    private void checkAndPrepareDao(Path daoDir, BufferedWriter protocol) throws IOException {
        protocol.write("Kontrola a příprava DAO " + daoDir.getFileName());
        protocol.newLine();

        Path generatedDir = Paths.get(daoDir.toAbsolutePath().toString(), GENERATED_DIR);
        if (Files.exists(generatedDir)) {
            protocol.write("Odstraňování adresáře " + GENERATED_DIR);
            protocol.newLine();
            deleteDirectoryRecursion(generatedDir);
        }
        protocol.write("Vytváření adresáře " + GENERATED_DIR);
        protocol.newLine();
        Files.createDirectory(generatedDir);

        String batchName = daoDir.getFileName().toString();
        String[] names = batchName.split("_");
        if (names.length != 3) {
            throw new IllegalStateException("Název DAO není ve formátu CisloArchivu_CisloAS_DAO");
        }
    }

    private List<Path> getBatchDirs(Path inputDir) throws IOException {
        List<Path> possibleBatches = new LinkedList<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(inputDir)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    DirectoryStream<Path> paths = Files.newDirectoryStream(path, BATCH_READY_FILE);
                    log.info("Kontrola dávky " + path.getFileName());
                    if (paths.iterator().hasNext()) {
                        log.info("Dávka " + path.getFileName() + " obsahuje soubor " + BATCH_READY_FILE);
                        possibleBatches.add(path);
                    }
                }
            }
        }
        return possibleBatches;
    }

    void deleteDirectoryRecursion(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.delete(path);
    }

    @PostConstruct
    public void configImportDirectories() throws IOException {
        String mainDir = configurationService.getProperty("elza.daoimport.dir");
        log.info("Kořenová složka pro import " + mainDir);

        if (StringUtils.isBlank(mainDir)) {
            throw new IllegalStateException("Není nastaven kořenový adresář pro import.");
        }

        Path rootDir = Paths.get(mainDir);
        if (Files.exists(rootDir)) {
            if (!Files.isDirectory(rootDir)) {
                throw new IllegalStateException("Kořenový adresář pro import není adresář(" + mainDir + ")");
            }
        } else {
            Files.createDirectories(rootDir);
        }

        checkOrCreateDir(rootDir, INPUT_DIR);
        checkOrCreateDir(rootDir, ARCHIVE_DIR);
        checkOrCreateDir(rootDir, ERROR_DIR);
    }

    private void checkOrCreateDir(final Path rootDir, final String dir) throws IOException {
        Path directory = Paths.get(rootDir.toAbsolutePath().toString(), dir);
        if (Files.exists(directory)) {
            if (!Files.isDirectory(directory)) {
                throw new IllegalStateException("Adresář pro import není adresář(" + directory + ")");
            }
        } else {
            Files.createDirectory(directory);
        }
    }
}
