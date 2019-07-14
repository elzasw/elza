package cz.tacr.elza.daoimport.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.mediafilter.JPEGFilter;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.dspace.workflowbasic.factory.BasicWorkflowServiceFactory;
import org.dspace.workflowbasic.service.BasicWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import cz.tacr.elza.daoimport.DaoImportScheduler;
import cz.tacr.elza.daoimport.parser.TechnicalMDParser;
import cz.tacr.elza.daoimport.service.vo.DaoFile;
import cz.tacr.elza.daoimport.service.vo.ImportBatch;
import cz.tacr.elza.daoimport.service.vo.ImportDao;
import cz.tacr.elza.metadataconstants.MetadataEnum;

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

    public static final String CONTENT_BITSTREAM = "ORIGINAL";
    public static final String METADATA_BITSTREAM = "METADATA";
    public static final String THUMBNAIL_BITSTREAM = "THUMBNAIL";

    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private BasicWorkflowService basicWorkflowService = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowService();

    @Autowired
    private JhoveService jhoveService;

    @Autowired
    private DroidService droidService;

    public List<ImportBatch> prepareImport(final Context context) throws IOException {
        String mainDir = getMainDir();
        Path inputDir = Paths.get(mainDir, INPUT_DIR);
        log.info("Hledání dávek v adresáři " + inputDir.toAbsolutePath());

        List<Path> possibleBatches = getBatchDirs(inputDir);

        List<ImportBatch> batches = new LinkedList<>();
        for (Path batchDir : possibleBatches) {
            BufferedWriter protocol = createProtocolWriter(batchDir);

            try {
                ImportBatch importBatch = checkAndPrepareBatch(batchDir, protocol, context);
                if (!importBatch.getDaos().isEmpty()) {
                    batches.add(importBatch);
                } else {
                    protocol.write("Dávka " + batchDir.toAbsolutePath() + " neobsahuje žádné digitalizáty a proto nebude dále zpracovávána.");
                    protocol.newLine();
                }
                protocol.write("Konec přípravy dávky: " + batchDir.toAbsolutePath());
                protocol.newLine();
            } catch (Exception e) {
                protocol.write("Chyba při zpracování dávky: " + e.getMessage());
                protocol.newLine();
                protocol.write(ExceptionUtils.getStackTrace(e));
                protocol.close();

                Path errorDir = Paths.get(mainDir, ERROR_DIR, batchDir.getFileName().toString());
                Files.move(batchDir, errorDir);
            }
        }

        return batches;
    }

    private String getMainDir() {
        return configurationService.getProperty("elza.daoimport.dir");
    }

    public void importBatches(final List<ImportBatch> importBatches, final Context context) throws IOException {
        for (ImportBatch batch : importBatches) {
            BufferedWriter protocol = batch.getProtocol();

            String mainDir = getMainDir();
            Path batchDir = batch.getBatchDir();
            try {
                importBatch(batch, protocol, context);
                context.complete();

                protocol.write("Konec zpracování dávky: " + batchDir.toAbsolutePath());
                protocol.close();

                Path archiveDir = Paths.get(mainDir, ARCHIVE_DIR, batchDir.getFileName().toString());
                Files.move(batchDir, archiveDir);
            } catch (Exception e) {
                context.abort();

                protocol.write("Chyba při zpracování dávky: " + e.getMessage());
                protocol.newLine();
                protocol.write(ExceptionUtils.getStackTrace(e));
                protocol.close();

                Path errorDir = Paths.get(mainDir, ERROR_DIR, batchDir.getFileName().toString());
                Files.move(batchDir, errorDir);
            }
        }
    }

    private void importBatch(final ImportBatch batch, final BufferedWriter protocol, final Context context) throws IOException, SQLException {
        protocol.write("Import dávky " + batch.getBatchName());
        protocol.newLine();

        for (ImportDao importDao : batch.getDaos()) {
            int sequence = 0;
            Item item = createItem(importDao.getCollectionId(), importDao.getDaoId(), protocol, context);
            Bundle origBundle = createBundle(CONTENT_BITSTREAM, item,  protocol, context);
            Bundle metaBundle = createBundle(METADATA_BITSTREAM, item,  protocol, context);
            Bundle thumbBundle = createBundle(THUMBNAIL_BITSTREAM, item,  protocol, context);
            for (DaoFile daoFile : importDao.getFiles()) {
                String bsName = daoFile.getContentFile().getFileName().toString();
                Bitstream contentBitstream = createBitstream(bsName, daoFile.getContentFile(), origBundle, CONTENT_BITSTREAM, sequence, protocol, context);
                // TODO vanek - datum vytvoření = datum vytvoření souboru? nebo importu do DSpace?
                if (daoFile.getMetadataFile() != null) {
                    Bitstream metadataBitstream = createBitstream(bsName, daoFile.getMetadataFile(), metaBundle, METADATA_BITSTREAM, sequence, protocol, context);
                    Map<MetadataEnum, String> techMD = daoFile.getTechMD();
                    storeTechMD(contentBitstream, techMD, protocol, context);
                }
                Bitstream thumbnailBitstream = createBitstream(bsName, daoFile.getThumbnailFile(), thumbBundle, THUMBNAIL_BITSTREAM, sequence, protocol, context);
                sequence++;
            }
        }
    }

    private void storeTechMD(Bitstream contentBitstream, Map<MetadataEnum, String> techMD, BufferedWriter protocol, Context context) throws IOException, SQLException {
        if (!techMD.isEmpty()) {
            protocol.write("Ukládám technická metadata pro bitstream " + contentBitstream.getName());
            protocol.newLine();
        }

        for (MetadataEnum mt : techMD.keySet()) {
            String value = techMD.get(mt);

            protocol.write("Ukládám technická metadata " + mt.name() + " s hodnotou " + value);
            protocol.newLine();

            bitstreamService.addMetadata(context, contentBitstream, mt.getSchema(), mt.getElement(), mt.getQualifier(), null, value);
            try {
                bitstreamService.update(context, contentBitstream);
            } catch (AuthorizeException e) {
                throw new IllegalStateException("Chyba při ukládání technických metadat bitstreamu " + contentBitstream.getName(), e);
            }
        }
    }

    private Bitstream createBitstream(final String name, final Path file, final Bundle bundle, final String bitstreamName,
                                      final int sequence, final BufferedWriter protocol, final Context context) {
        try {
            protocol.write("Vytváření Bitstream " + bitstreamName + " pro Bundle " + bundle.getName());
            protocol.newLine();

            InputStream inputStream = Files.newInputStream(file);
            Bitstream bs = bitstreamService.create(context, bundle, inputStream);
            bs.setName(context, name);
            //bs.setDescription(context,null);
            bs.setSequenceID(sequence);

            String mimeType = droidService.getMimeType(file, protocol);
            BitstreamFormat bf;
            if (mimeType == null) {
                bf = bitstreamFormatService.guessFormat(context, bs);
            } else {
                bf = bitstreamFormatService.findByMIMEType(context, mimeType);
            }
            bitstreamService.setFormat(context, bs, bf);

            bitstreamService.update(context, bs);
            return bs;
        } catch (Exception e) {
            throw new IllegalStateException("Chyba při vytváření Bitstream " + bitstreamName + " pro Bundle " + bundle.getName(), e);
        }
    }

    private Bundle createBundle(final String name, Item item, final BufferedWriter protocol, final Context context) {
        try {

            protocol.write("Vytváření bundle " + name + " pro Item " + item.getName());
            protocol.newLine();

            return bundleService.create(context, item, name);
        } catch (Exception e) {
            throw new IllegalStateException("Chyba při vytváření Bundle " + name + " pro Item " + item.getName(), e);
        }
    }

    private Item createItem(final UUID collectionId, final String daoId, final BufferedWriter protocol, final Context context) {
        try {
            Collection collection = collectionService.find(context, collectionId);

            protocol.write("Vytváření Item " + daoId + " v kolekci " + collection.getName());
            protocol.newLine();

            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            BasicWorkflowItem workflowItem = basicWorkflowService.start(context, workspaceItem);
            workflowItem.setState(BasicWorkflowService.WFSTATE_ARCHIVE);
            basicWorkflowService.advance(context, workflowItem, context.getCurrentUser());


            Item item = workflowItem.getItem();
            itemService.setMetadataSingleValue(context, item, MetadataSchema.DC_SCHEMA, "title", null, null, daoId);
            itemService.setMetadataSingleValue(context, item, MetadataSchema.DC_SCHEMA, "description", null, null, daoId);

            DCDate dcDate = new DCDate(new Date());
            String date = dcDate.displayDate(false, true, Locale.getDefault());
            itemService.setMetadataSingleValue(context, item, MetadataSchema.DC_SCHEMA, "date", "issued", null, date);

            MetadataEnum metaData = MetadataEnum.ISELZA;
            itemService.addMetadata(context, item, metaData.getSchema(), metaData.getElement(), metaData.getQualifier(), null, "false");

            itemService.update(context, item);
            // set metadata?
            return item;
        } catch (Exception e) {
            throw new IllegalStateException("Chyba při vytváření Item " + daoId, e);
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

    private ImportBatch checkAndPrepareBatch(Path batchDir, BufferedWriter protocol, Context context) throws IOException, SQLException {
        protocol.write("Kontrola a příprava dávky " + batchDir.toAbsolutePath());
        protocol.newLine();

        ImportBatch importBatch = new ImportBatch();
        importBatch.setBatchDir(batchDir);
        importBatch.setBatchName(batchDir.getFileName().toString());
        importBatch.setProtocol(protocol);

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
            ImportDao importDao = checkAndPrepareDao(daoDir, protocol, context);
            if (!importDao.getFiles().isEmpty()) {
                importBatch.addDao(importDao);
            } else {
                protocol.write("Digitalizát " + daoDir.toAbsolutePath() + " neobsahuje žádné soubory a proto nebude dále zpracováván.");
                protocol.newLine();
            }
        }

        return importBatch;
    }

    private ImportDao checkAndPrepareDao(Path daoDir, BufferedWriter protocol, Context context) throws IOException, SQLException {
        protocol.write("Kontrola a příprava DAO " + daoDir.getFileName());
        protocol.newLine();

        ImportDao importDao = new ImportDao();

        Path generatedDir = Paths.get(daoDir.toAbsolutePath().toString(), GENERATED_DIR);
        if (Files.exists(generatedDir)) {
            protocol.write("Odstraňování adresáře " + GENERATED_DIR);
            protocol.newLine();
            deleteDirectoryRecursion(generatedDir);
        }
        protocol.write("Vytváření adresáře " + GENERATED_DIR);
        protocol.newLine();
        Files.createDirectory(generatedDir);

        String daoDirName = daoDir.getFileName().toString();
        String[] names = daoDirName.split("_");
        if (names.length != 3) {
            throw new IllegalStateException("Název DAO není ve formátu CisloArchivu_CisloAS_DAO");
        }

        String archiveId = names[0];
        String fundId = names[1];
        String daoId = names[2];

        if (StringUtils.isBlank(archiveId)) {
            throw new IllegalStateException("V názvu adresáře DAO(" + daoDirName + ") není vyplněno číslo archivu.");
        }

        if (StringUtils.isBlank(fundId)) {
            throw new IllegalStateException("V názvu adresáře DAO(" + daoDirName + ") není vyplněno číslo archivního souboru.");
        }

        if (StringUtils.isBlank(daoId)) {
            throw new IllegalStateException("V názvu adresáře DAO(" + daoDirName + ") není vyplněn identifikátor digitalizátu.");
        }

        Community community = null;
        List<Community> communities = communityService.findAll(context);
        for (Community c : communities) {
            String desc = communityService.getMetadataFirstValue(c, MetadataSchema.DC_SCHEMA, "description", "abstract", Item.ANY);
            if (archiveId.equalsIgnoreCase(desc)) {
                community = c;
                break;
            }
        }

        if (community == null) {
            throw new IllegalStateException("Komunita(archiv) s názvem " + archiveId + " neexistuje.");
        }

        Collection collection = null;
        List<Collection> collections = communityService.getAllCollections(context, community);
        for (Collection c : collections) {
            String desc = collectionService.getMetadataFirstValue(c, MetadataSchema.DC_SCHEMA, "description", "abstract", Item.ANY);
            if (fundId.equalsIgnoreCase(desc)) {
                collection = c;
                break;
            }
        }

        if (collection == null) {
            throw new IllegalStateException("Kolekce(archivní soubor) s názvem " + fundId + " neexistuje.");
        }

        // kontrola na existenci dao/item
//        metadataValueDAO.findMany()


        importDao.setCollectionId(collection.getID());
        importDao.setCommunityId(community.getID());
        importDao.setDaoId(daoId);

        // zpracování souborů
        try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(daoDir)) {
            for (Path contentFile : fileStream) {
                if (Files.isRegularFile(contentFile)
                        && !(contentFile.getFileName().toString().endsWith(METADATA_EXTENSION)
                        || contentFile.getFileName().toString().endsWith(THUMBNAIL_EXTENSION))) {
                    DaoFile daoFile = new DaoFile();

                    protocol.write("Kopírování souboru " + contentFile.getFileName().toString()  + " do adresáře " + GENERATED_DIR);
                    protocol.newLine();
                    Path destPath = Paths.get(generatedDir.toString(), contentFile.getFileName().toString());
                    daoFile.setContentFile(Files.copy(contentFile, destPath, StandardCopyOption.REPLACE_EXISTING));

                    Path metadataFile = Paths.get(contentFile.toAbsolutePath().toString() + METADATA_EXTENSION);
                    destPath = Paths.get(generatedDir.toString(), metadataFile.getFileName().toString());
                    if (Files.exists(metadataFile)) {
                        protocol.write("Kopírování souboru " + metadataFile.getFileName().toString()  + " do adresáře " + GENERATED_DIR);
                        protocol.newLine();

                        daoFile.setTechMD(parseMetadata(metadataFile));
                        daoFile.setMetadataFile(Files.copy(metadataFile, destPath, StandardCopyOption.REPLACE_EXISTING));
                    } else {
                        protocol.write("Generování metadat souboru " + contentFile.getFileName().toString());
                        protocol.newLine();

                        if (jhoveService.generateMetadata(contentFile, protocol, destPath)) {
                            daoFile.setMetadataFile(destPath);
                            daoFile.setTechMD(parseMetadata(destPath));
                        }
                    }

                    Path thumbnailFile = Paths.get(contentFile.toAbsolutePath().toString() + THUMBNAIL_EXTENSION);
                    destPath = Paths.get(generatedDir.toString(), thumbnailFile.getFileName().toString());
                    if (Files.exists(thumbnailFile)) {
                        protocol.write("Kopírování souboru " + thumbnailFile.getFileName().toString()  + " do adresáře " + GENERATED_DIR);
                        protocol.newLine();

                        daoFile.setThumbnailFile(Files.copy(thumbnailFile, destPath, StandardCopyOption.REPLACE_EXISTING));
                    } else {
                        protocol.write("Generování náhledu souboru " + contentFile.getFileName().toString());
                        protocol.newLine();

                        JPEGFilter jpegFilter = new JPEGFilter();
                        InputStream is = null;
                        InputStream thumbnailIS = null;
                        try {
                            is = Files.newInputStream(contentFile);
                            thumbnailIS = jpegFilter.getDestinationStream(null, is, false);
                            Files.copy(thumbnailIS, destPath, StandardCopyOption.REPLACE_EXISTING);
                            daoFile.setThumbnailFile(destPath);
                        } catch (Exception e) {
                            throw new IllegalStateException("Chyba při generování náhledu souboru " + contentFile.getFileName() + ". ", e);
                        } finally {
                            if (is != null) {
                                IOUtils.closeQuietly(is);
                            }
                            if (thumbnailIS != null) {
                                IOUtils.closeQuietly(thumbnailIS);
                            }
                        }
                        //TODO implementovat
                    }
                    importDao.addFile(daoFile);
                }
            }
        }

        return importDao;
    }

    private Map<MetadataEnum, String> parseMetadata(Path mdFile) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            TechnicalMDParser technicalMdParser = new TechnicalMDParser();
            saxParser.parse(mdFile.toFile(), technicalMdParser);
            return technicalMdParser.getMd();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Chyba při parsování technických metadat.", e);
        } catch (SAXException e) {
            throw new IllegalStateException("Chyba při parsování technických metadat.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Chyba při parsování technických metadat.", e);
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
        String mainDir = getMainDir();
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
