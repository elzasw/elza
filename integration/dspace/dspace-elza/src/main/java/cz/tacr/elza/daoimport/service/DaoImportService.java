package cz.tacr.elza.daoimport.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
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
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
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
import cz.tacr.elza.daoimport.schema.dao.Attribute;
import cz.tacr.elza.daoimport.schema.dao.Dao;
import cz.tacr.elza.daoimport.schema.dao.Page;
import cz.tacr.elza.daoimport.service.vo.DaoFile;
import cz.tacr.elza.daoimport.service.vo.ImportBatch;
import cz.tacr.elza.daoimport.service.vo.ImportConfig;
import cz.tacr.elza.daoimport.service.vo.ImportDao;
import cz.tacr.elza.daoimport.service.vo.MetadataInfo;
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
    public static final String DAO_XML = "dao.xml";

    public static final String CONTENT_BUNDLE = "ORIGINAL";
    public static final String METADATA_BUNDLE = "METADATA";
    public static final String THUMBNAIL_BUNDLE = "THUMBNAIL";

    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
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
        ImportConfig config = getImportConfig();
        Path  inputDir= Paths.get(config.getMainDir(), INPUT_DIR);
        log.info("Hledání dávek v adresáři " + inputDir.toAbsolutePath());

        List<Path> possibleBatches = getBatchDirs(inputDir);

        List<ImportBatch> batches = new LinkedList<>();
        for (Path batchDir : possibleBatches) {
            BufferedWriter protocol = createProtocolWriter(batchDir);

            boolean emptyBatch = false;
            try {
                ImportBatch importBatch = checkAndPrepareBatch(batchDir, config, protocol, context);
                emptyBatch = importBatch.getDaos().isEmpty();
                if (!emptyBatch) {
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

                Path errorDir = Paths.get(config.getMainDir(), ERROR_DIR);
                moveBatchDir(batchDir, errorDir);
            } finally {
                if (emptyBatch) {
                    protocol.close();
                }
            }
        }

        return batches;
    }

    private void moveBatchDir(Path batchDir, Path targetDir) throws IOException {
        int i = 1;
        Path errorDir = Paths.get(targetDir.toString(), batchDir.getFileName().toString());
        while (errorDir.toFile().exists()) {
            errorDir = Paths.get(targetDir.toString(), batchDir.getFileName().toString() + "-" + i++);
        }
        Files.move(batchDir, errorDir);
    }

    private ImportConfig getImportConfig() {
        ImportConfig config = new ImportConfig();
        config.setMainDir(getMainDir());
        config.setSupportedMimeTypes(getSupportedMimeTypes());
        config.setMimeTypesForConversion(getSupportedTypesForConvertion());
        config.setConversionCommand(getConversionCommand());
        config.setMimeTypeAferConversion(getMimeTypeAferConversion());
        config.setFileExtensionAferConversion(getFileExtensionAferConversion());
        config.setDaoNameExpression(getDaoNameExpression());
                
        return config;
    }

    private String getMainDir() {
        return StringUtils.trimToNull(configurationService.getProperty("elza.daoimport.dir"));
    }

    private List<String> getSupportedMimeTypes() {
        String mimeTypes = configurationService.getProperty("elza.daoimport.supportedMimeTypes");
        if (StringUtils.isBlank(mimeTypes)) {
            return Collections.emptyList();
        }
        String[] split = mimeTypes.split(" ");
        List<String> supportedMimeTypes = new ArrayList<>(split.length);
        for (String mimeType : split) {
            supportedMimeTypes.add(mimeType.toLowerCase());
        }
        return supportedMimeTypes;
    }

    private List<String> getSupportedTypesForConvertion() {
        String mimeTypes = configurationService.getProperty("elza.daoimport.convert.supportedMimeTypes");
        if (StringUtils.isBlank(mimeTypes)) {
            return Collections.emptyList();
        }
        String[] split = mimeTypes.split(" ");
        List<String> supportedTypes = new ArrayList<>(split.length);
        for (String mimeType : split) {
            if (StringUtils.isNotBlank(mimeType)) {
                supportedTypes.add(mimeType.toLowerCase().trim());
            }
        }
        return supportedTypes;
    }

    private String getConversionCommand() {
        return StringUtils.trimToNull(configurationService.getProperty("convert.files.app.command"));
    }

    private String getMimeTypeAferConversion() {
        return StringUtils.trimToNull(configurationService.getProperty("elza.daoimport.convert.outputMimeType"));
    }

    private String getFileExtensionAferConversion() {
        return StringUtils.trimToNull(configurationService.getProperty("elza.daoimport.convert.outputExtension"));
    }
    
    private String getDaoNameExpression() {
        return StringUtils.trimToNull(configurationService.getProperty("elza.daoimport.dao.name"));
    }

    public void importBatches(final List<ImportBatch> importBatches, final Context context) throws IOException {
        for (ImportBatch batch : importBatches) {
            BufferedWriter protocol = batch.getProtocol();

            String mainDir = getMainDir();
            Path batchDir = batch.getBatchDir();
            try {
                importBatch(batch, protocol, context);
                context.commit();

                protocol.write("Konec zpracování dávky: " + batchDir.toAbsolutePath());
                protocol.close();

                Path archiveDir = Paths.get(mainDir, ARCHIVE_DIR);
                moveBatchDir(batchDir, archiveDir);
            } catch (Exception e) {
                context.abort();

                protocol.write("Chyba při zpracování dávky: " + e.getMessage());
                protocol.newLine();
                protocol.write(ExceptionUtils.getStackTrace(e));
                protocol.close();

                Path errorDir = Paths.get(mainDir, ERROR_DIR);
                moveBatchDir(batchDir, errorDir);
            }
        }
    }

    private void importBatch(final ImportBatch batch, final BufferedWriter protocol, final Context context) throws IOException, SQLException {
        protocol.write("Import dávky " + batch.getBatchName());
        protocol.newLine();

        for (ImportDao importDao : batch.getDaos()) {
            int sequence = 0;
            Item item = createItem(importDao, protocol, context);
            Bundle origBundle = createBundle(CONTENT_BUNDLE, item,  protocol, context);
            Bundle metaBundle = createBundle(METADATA_BUNDLE, item,  protocol, context);
            Bundle thumbBundle = createBundle(THUMBNAIL_BUNDLE, item,  protocol, context);
            for (DaoFile daoFile : importDao.getFiles()) {
                String bsName = daoFile.getContentFile().getFileName().toString();
                Bitstream contentBitstream = createBitstream(bsName, daoFile.getContentFile(), origBundle, sequence, protocol, context);
                //pridani casu vytvoreni souboru do metadat
                if (daoFile.getCreatedDate() != null) {
                    bitstreamService.addMetadata(context, contentBitstream, MetadataSchema.DC_SCHEMA, "date", "created", null, daoFile.getCreatedDate());
                }
                if (daoFile.getMetadataFile() != null) {
                    Bitstream metadataBitstream = createBitstream(bsName, daoFile.getMetadataFile(), metaBundle, sequence, protocol, context);
                    Map<MetadataEnum, String> techMD = daoFile.getTechMD();
                    storeTechMD(contentBitstream, techMD, protocol, context);
                    if (daoFile.getDescription() != null) {
                        bitstreamService.addMetadata(context, metadataBitstream, MetadataSchema.DC_SCHEMA, "description", null, null, daoFile.getDescription());
                    }
                }

                Bitstream thumbnailBitstream = createBitstream(bsName, daoFile.getThumbnailFile(), thumbBundle, sequence, protocol, context);
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

    private Bitstream createBitstream(final String name, final Path file, final Bundle bundle, final int sequence,
                                      final BufferedWriter protocol, final Context context) {
        try {
            protocol.write("Vytváření Bitstream " + name + " pro Bundle " + bundle.getName());
            protocol.newLine();

            InputStream inputStream = Files.newInputStream(file);
            Bitstream bs = bitstreamService.create(context, bundle, inputStream);
            bs.setName(context, name);
            //bs.setDescription(context,null);
            bs.setSequenceID(sequence);

            String mimeType = null; //droidService.getMimeType(file, protocol);
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
            throw new IllegalStateException("Chyba při vytváření Bitstream " + name + " pro Bundle " + bundle.getName(), e);
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

    private Item createItem(final ImportDao importDao, final BufferedWriter protocol, final Context context) {
        String daoName = importDao.getDaoName();
        try {
            Collection collection = collectionService.find(context, importDao.getCollectionId());

            protocol.write("Vytváření Item " + daoName + " v kolekci " + collection.getName());
            protocol.newLine();

            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            BasicWorkflowItem workflowItem = basicWorkflowService.start(context, workspaceItem);
            workflowItem.setState(BasicWorkflowService.WFSTATE_ARCHIVE);
            basicWorkflowService.advance(context, workflowItem, context.getCurrentUser());


            Item item = workflowItem.getItem();

            itemService.setMetadataSingleValue(context, item, MetadataSchema.DC_SCHEMA, "title", null, null, daoName);
            itemService.setMetadataSingleValue(context, item, MetadataSchema.DC_SCHEMA, "description", null, null, importDao.getDaoId());

            DCDate dcDate = new DCDate(new Date());
            String date = dcDate.displayDate(false, true, Locale.getDefault());
            itemService.setMetadataSingleValue(context, item, MetadataSchema.DC_SCHEMA, "date", "issued", null, date);

            MetadataEnum metaData = MetadataEnum.ISELZA;
            itemService.addMetadata(context, item, metaData.getSchema(), metaData.getElement(), metaData.getQualifier(), null, "false");

            Map<Integer, String> metadata = importDao.getMetadata();
            if (metaData != null) {
                for (Map.Entry<Integer, String> entry : metadata.entrySet()) {
                    MetadataField metadataField = metadataFieldService.find(context, entry.getKey());
                    itemService.clearMetadata(context, item, metadataField.getMetadataSchema().getName(), metadataField.getElement(), metadataField.getQualifier(), null);
                    itemService.addMetadata(context, item, metadataField, null, entry.getValue());
                }
            }

            itemService.update(context, item);
            return item;
        } catch (Exception e) {
            throw new IllegalStateException("Chyba při vytváření Item " + daoName, e);
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

    private ImportBatch checkAndPrepareBatch(Path batchDir, ImportConfig config, BufferedWriter protocol, Context context) throws IOException, SQLException {
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
            ImportDao importDao = checkAndPrepareDao(daoDir, config, protocol, context);
            if (!importDao.getFiles().isEmpty()) {
                importBatch.addDao(importDao);
            } else {
                protocol.write("Digitalizát " + daoDir.toAbsolutePath() + " neobsahuje žádné soubory a proto nebude dále zpracováván.");
                protocol.newLine();
            }
        }

        return importBatch;
    }

    private ImportDao checkAndPrepareDao(Path daoDir, ImportConfig config, BufferedWriter protocol, Context context) throws IOException, SQLException {
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

        importDao.setCollectionId(collection.getID());
        importDao.setCommunityId(community.getID());
        importDao.setDaoId(daoId);
        importDao.setDaoName(daoId);

        // zpracování souborů
        // načtení dao.xml pokud existuje
        Dao daoXml = readDaoXml(daoDir, protocol);
        if (daoXml != null) {
            Map<String, String> md = fillDaoAttributes(context, importDao, daoXml, protocol);
            if (StringUtils.isNotBlank(config.getDaoNameExpression())) {
                generateDaoName(md, config.getDaoNameExpression(), importDao, protocol);
            }
        }

        try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(daoDir)) {
            for (Path contentFile : fileStream) {
                if (Files.isRegularFile(contentFile)
                        && !(contentFile.getFileName().toString().endsWith(METADATA_EXTENSION)
                        || contentFile.getFileName().toString().endsWith(THUMBNAIL_EXTENSION))) {

                    DaoFile daoFile = new DaoFile();
                    BasicFileAttributes fileAttributes = Files.readAttributes(contentFile, BasicFileAttributes.class);
                    daoFile.setCreatedDate(new Date(fileAttributes.creationTime().toMillis()));

                    boolean isConvertedFile = false;
                    Path destPath = Paths.get(generatedDir.toString(), contentFile.getFileName().toString());
                    String mimeType = droidService.getMimeType(contentFile, protocol);
                    if (isFileMimeTypeSupported(contentFile, mimeType, config, protocol)) {
                        if (config.getMimeTypesForConversion().contains(mimeType)) {
                            protocol.write("Soubor " + contentFile.getFileName().toString()  + " bude převeden na požaadovaný typ ");
                            protocol.newLine();

                            String convertAppCommand = config.getConversionCommand();
                            if (StringUtils.isBlank(convertAppCommand)) {
                                protocol.write("V konfiguraci není definován příkaz pro konverzi souborů. Nelze zahájit konverzi souboru.");
                                protocol.newLine();
                                continue;
                            }

                            contentFile = convertFile(contentFile, config, generatedDir, protocol);
                            daoFile.setContentFile(contentFile);
                            isConvertedFile = true;
                        } else {
                            protocol.write("Kopírování souboru " + contentFile.getFileName().toString()  + " do adresáře " + GENERATED_DIR);
                            protocol.newLine();
                            daoFile.setContentFile(Files.copy(contentFile, destPath, StandardCopyOption.REPLACE_EXISTING));
                        }
                    } else {
                        continue;
                    }

                    Path metadataFile = Paths.get(contentFile.toAbsolutePath().toString() + METADATA_EXTENSION);
                    destPath = Paths.get(generatedDir.toString(), metadataFile.getFileName().toString());
                    if (!isConvertedFile && Files.exists(metadataFile)) {
                        protocol.write("Kopírování souboru " + metadataFile.getFileName().toString()  + " do adresáře " + GENERATED_DIR);
                        protocol.newLine();

                        daoFile.setTechMD(parseMetadata(metadataFile));
                        daoFile.setMetadataFile(Files.copy(metadataFile, destPath, StandardCopyOption.REPLACE_EXISTING));
                    } else {
                        MetadataInfo metadataInfo = fillBasicdaoXmlSpecification(daoXml, daoFile, contentFile.getFileName().getFileName().toString());
                        protocol.write("Generování metadat souboru " + contentFile.getFileName().toString());
                        protocol.newLine();
                        if (jhoveService.generateMetadata(contentFile, protocol, destPath, metadataInfo)) {
                            daoFile.setMetadataFile(destPath);
                            daoFile.setTechMD(parseMetadata(destPath));
                        }
                    }

                    Path thumbnailFile = Paths.get(contentFile.toAbsolutePath().toString() + THUMBNAIL_EXTENSION);
                    destPath = Paths.get(generatedDir.toString(), thumbnailFile.getFileName().toString());
                    if (!isConvertedFile && Files.exists(thumbnailFile)) {
                        protocol.write("Kopírování souboru " + thumbnailFile.getFileName().toString()  + " do adresáře " + GENERATED_DIR);
                        protocol.newLine();

                        daoFile.setThumbnailFile(Files.copy(thumbnailFile, destPath, StandardCopyOption.REPLACE_EXISTING));
                    } else {
                        generateThumbnailFile(protocol, contentFile, destPath, daoFile);
                    }
                    importDao.addFile(daoFile);
                }
            }
        }

        return importDao;
    }

    private void generateDaoName(Map<String, String> md, String daoNameExpression, ImportDao importDao, BufferedWriter protocol) {
        String regex = "\\$\\{[\\w\\.]+\\}";      // příklad formátu výrazu: ${dc.title} - ${dc.creator}
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(daoNameExpression);
        String name = daoNameExpression;
        while (matcher.find()) {
            String exp = matcher.group();
            String key = exp.substring(2, exp.length() - 1); // získání klíče do mapy metadat, ${dc.title} -> dc.title
            String value = StringUtils.trimToEmpty(md.get(key));
            name = name.replace(exp, value);
        }
        importDao.setDaoName(name);
    }

    private Map<String, String> fillDaoAttributes(Context context, ImportDao importDao, Dao daoXml, BufferedWriter protocol) throws IOException {
        Map<String, String> mdForNameGeneration = new HashMap<>();
        Map<Integer, String> mdForStore = new HashMap<>();
        for (Attribute att : daoXml.getMeta().getAttr()) {
            try {
                MetadataField field = metadataFieldService.findByElement(context,
                        att.getSchema(),
                        att.getElement(),
                        StringUtils.trimToNull(att.getQualifier()));
                StringBuilder sb = new StringBuilder(att.getSchema())
                        .append(".")
                        .append(att.getElement());
                if (StringUtils.trimToNull(att.getQualifier()) != null) {
                    sb.append(".")
                        .append(att.getQualifier());
                }
                String key = sb.toString();
                if (field == null) {
                    throw new IllegalStateException("Neexistuje typ metadat " + key);
                }
                mdForNameGeneration.put(key, att.getValue());
                mdForStore.put(field.getID(), att.getValue());
                protocol.write("Načtení atributu metadat " + key + "=" + att.getValue());
            } catch (SQLException e) {
                throw new IllegalStateException("Došlo k chybě při pokusu o načtení typu metadat DAO " + importDao.getDaoId());
            }
        }
        importDao.setMetadata(mdForStore);
        return mdForNameGeneration;
    }

    private Dao readDaoXml(Path daoDir, BufferedWriter protocol) throws IOException {
        Dao daoXmlSpecification = null;
        Path daoXmlFile = Paths.get(daoDir.toAbsolutePath().toString(), DAO_XML);
        if (Files.exists(daoXmlFile)) {
            protocol.write("Byl nalezen volitelný soubor dao.xml.");
            try (FileInputStream is = new FileInputStream(daoXmlFile.toFile())) {
                JAXBContext jaxbContext = JAXBContext.newInstance(Dao.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                StreamSource source = new StreamSource(is);
                daoXmlSpecification = unmarshaller.unmarshal(source, Dao.class).getValue();
            } catch (JAXBException e) {
                protocol.write("Nepodařilo se načíst data ze souboru dao.xml.");
            }
        }
        return daoXmlSpecification;
    }

    private Path convertFile(Path file, ImportConfig config, Path generatedDir, BufferedWriter protocol) throws IOException {
        String convertAppCommand = config.getConversionCommand();
        convertAppCommand = convertAppCommand.replace("{input}", "'" + file.toString()) + "'";
        Path outputPath = Paths.get(generatedDir.toString(), file.getFileName().toString());
        String output = FilenameUtils.removeExtension(outputPath.toString());
        output += config.getFileExtensionAferConversion();
        convertAppCommand = convertAppCommand.replace("{output}", "'" + output + "'");
        ProcessBuilder builder = new ProcessBuilder(convertAppCommand);
        builder.redirectErrorStream(true);
        try {
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            int result = r.read();
            Path convertedPath = Paths.get(output);
            if (result == 0 && Files.exists(convertedPath)) {
                protocol.write("Došlo k úspěšné konverzi souboru: " + file.getFileName().toString() + " na " + output);
                protocol.newLine();
                return convertedPath;
            } else {
                throw new IllegalStateException("Došlo k chybě při pokusu o konverzi souboru " + file.getFileName().toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Došlo k chybě při pokusu o konverzi souboru " + file.getFileName().toString());
        }
    }

    private boolean isFileMimeTypeSupported(Path contentFile, String mimeType, ImportConfig config, BufferedWriter protocol) throws IOException {
        List<String> supportedMimeTypes = config.getSupportedMimeTypes();
        if (CollectionUtils.isEmpty(supportedMimeTypes)) {
            return true;
        }

        if (StringUtils.isBlank(mimeType)) {
            protocol.write("Nebyl zjištěn MimeType souboru " + contentFile.toAbsolutePath() +
                    " a proto nelze ověřit zda je mezi povolenými typy pro import. Soubor nebude importován.");
            protocol.newLine();
            return false;
        }

        boolean isSupported = supportedMimeTypes.contains(mimeType);
        if (!isSupported) {
            protocol.write("MimeType " + mimeType + " souboru " + contentFile.toAbsolutePath() +
                    " není mezi podporovanými typy. Soubor proto nebude importován.");
            protocol.newLine();
        }

        return isSupported;
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

    private void generateThumbnailFile(BufferedWriter protocol, Path contentFile, Path destPath, DaoFile daoFile) throws IOException {
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
    }

    private List<Path> getBatchDirs(Path inputDir) throws IOException {
        List<Path> possibleBatches = new LinkedList<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(inputDir)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    try (DirectoryStream<Path> paths = Files.newDirectoryStream(path, BATCH_READY_FILE)) {
                        log.info("Kontrola dávky " + path.getFileName());
                        Iterator<Path> it = paths.iterator();
                        if (it.hasNext()) {
                            log.info("Dávka " + path.getFileName() + " obsahuje soubor " + BATCH_READY_FILE);
                            possibleBatches.add(path);

                            Path batchReadyFile = it.next();
                            Files.delete(batchReadyFile);
                        }
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

    private MetadataInfo fillBasicdaoXmlSpecification(Dao daoXmlSpecification, DaoFile daoFile, String fileName) {
        MetadataInfo metadataInfo = null;
        if (daoXmlSpecification != null && daoXmlSpecification.getPages() != null) {
            List<Page> pageInfoList = daoXmlSpecification.getPages().getPage();
            Integer pageNumber = 1;
            for (Page pageInfo : pageInfoList) {
                if(pageInfo.getFile() != null && fileName.contains(pageInfo.getFile())) {
                    metadataInfo = new MetadataInfo();
                    metadataInfo.setMimeType(pageInfo.getMimetype());
                    metadataInfo.setCheckSum(pageInfo.getChecksum());
                    daoFile.setOrderNumber(pageNumber);
                    daoFile.setDescription(pageInfo.getDescription());
                }
                pageNumber++;
            }
        }
        return metadataInfo;
    }
}
