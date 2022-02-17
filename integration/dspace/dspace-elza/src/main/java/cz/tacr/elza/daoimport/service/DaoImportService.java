package cz.tacr.elza.daoimport.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
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

import cz.tacr.elza.daoimport.protocol.FileProtocol;
import cz.tacr.elza.daoimport.protocol.Protocol;
import cz.tacr.elza.daoimport.service.vo.DaoFile;
import cz.tacr.elza.daoimport.service.vo.ImportBatch;
import cz.tacr.elza.daoimport.service.vo.ImportBundle;
import cz.tacr.elza.daoimport.service.vo.ImportConfig;
import cz.tacr.elza.daoimport.service.vo.ImportDao;
import cz.tacr.elza.metadataconstants.MetadataEnum;
import cz.tacr.elza.xsd.dspace.dao.Dao;

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

    public static final String ORIGINAL_BUNDLE = "ORIGINAL";
    public static final String METADATA_BUNDLE = "METADATA";
    public static final String THUMBNAIL_BUNDLE = "THUMBNAIL";

    private static Logger log = Logger.getLogger(DaoImportService.class);

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
            Protocol protocol = createProtocolWriter(batchDir);

            boolean emptyBatch = false;
            try {
                ImportBatch importBatch = checkAndPrepareBatch(batchDir, config, protocol, context);
                emptyBatch = importBatch.getDaos().isEmpty();
                if (!emptyBatch) {
                    batches.add(importBatch);
                } else {
                    protocol.add("Dávka " + batchDir.toAbsolutePath() + " neobsahuje žádné digitalizáty a proto nebude dále zpracovávána.");
                }
                protocol.add("Konec přípravy dávky: " + batchDir.toAbsolutePath());
            } catch (Exception e) {
                protocol.add("Chyba při zpracování dávky: " + e.getMessage());
                protocol.add(ExceptionUtils.getStackTrace(e));
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
            Protocol protocol = batch.getProtocol();

            String mainDir = getMainDir();
            Path batchDir = batch.getBatchDir();
            try {
                importBatch(batch, protocol, context);
                context.commit();

                protocol.add("Konec zpracování dávky: " + batchDir.toAbsolutePath());
                protocol.close();

                Path archiveDir = Paths.get(mainDir, ARCHIVE_DIR);
                moveBatchDir(batchDir, archiveDir);
            } catch (Exception e) {
                context.abort();

                protocol.add("Chyba při zpracování dávky: " + e.getMessage());
                protocol.add(ExceptionUtils.getStackTrace(e));
                protocol.close();

                Path errorDir = Paths.get(mainDir, ERROR_DIR);
                moveBatchDir(batchDir, errorDir);
            }
        }
    }

    private void importBatch(final ImportBatch batch, final Protocol protocol, final Context context) throws IOException, SQLException {
        protocol.add("Import dávky " + batch.getBatchName());

        for (ImportDao importDao : batch.getDaos()) {
            final Item item = createItem(importDao, protocol, context);
            Map<String, Bundle> bundleMap = new HashMap<>();
            for(ImportBundle importBundle: importDao.getBundles()) {
                int sequence = 0;

                Bundle bundle = bundleMap.computeIfAbsent(importBundle.getBundleName(), 
            			new Function<String, Bundle>() {
							@Override
							public Bundle apply(String name) {
								return createBundle(name, item,  protocol, context);
							}
						});
            	for(DaoFile daoFile: importBundle.getFiles()) {
            		String bsName = daoFile.getFile().getFileName().toString();
            		Bitstream contentBitstream = createBitstream(bsName, daoFile.getFile(), bundle, sequence, protocol, context);
                    //pridani casu vytvoreni souboru do metadat
                    if (daoFile.getCreatedDate() != null) {
                        bitstreamService.addMetadata(context, contentBitstream, MetadataSchema.DC_SCHEMA, "date", "created", null, daoFile.getCreatedDate());
                    }
                    if (daoFile.getDescription() != null) {
                        bitstreamService.addMetadata(context, contentBitstream, MetadataSchema.DC_SCHEMA, "description", null, null, daoFile.getDescription());
                    }
                    Map<MetadataEnum, String> techMD = daoFile.getTechMD();
                    storeTechMD(contentBitstream, techMD, protocol, context);
            		
            		sequence++;
            	}
            }
        }
    }

    private void storeTechMD(Bitstream contentBitstream, Map<MetadataEnum, String> techMD, 
    		Protocol protocol, Context context) throws IOException, SQLException {
        if (!techMD.isEmpty()) {
            protocol.add("Ukládám technická metadata pro bitstream " + contentBitstream.getName());
        }

        for (MetadataEnum mt : techMD.keySet()) {
            String value = techMD.get(mt);

            protocol.add("Ukládám technická metadata " + mt.name() + " s hodnotou " + value);

            bitstreamService.addMetadata(context, contentBitstream, mt.getSchema(), mt.getElement(), mt.getQualifier(), null, value);
            try {
                bitstreamService.update(context, contentBitstream);
            } catch (AuthorizeException e) {
                throw new IllegalStateException("Chyba při ukládání technických metadat bitstreamu " + contentBitstream.getName(), e);
            }
        }
    }

    private Bitstream createBitstream(final String name, final Path file, 
    								  final Bundle bundle, final int sequence,
                                      final Protocol protocol, 
                                      final Context context) {
        protocol.add("Vytváření Bitstream " + name + " pro Bundle " + bundle.getName());

        try(InputStream inputStream = Files.newInputStream(file)) {
        	
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
            throw new IllegalStateException("Chyba při vytváření Bitstream " + name + " pro Bundle " + bundle.getName(), e);
        }
    }

    private Bundle createBundle(final String name, Item item, final Protocol protocol, final Context context) {
        try {

            protocol.add("Vytváření bundle " + name + " pro Item " + item.getName());

            return bundleService.create(context, item, name);
        } catch (Exception e) {
            throw new IllegalStateException("Chyba při vytváření Bundle " + name + " pro Item " + item.getName(), e);
        }
    }

    private Item createItem(final ImportDao importDao, final Protocol protocol, final Context context) {
        String daoName = importDao.getDaoName();
        try {
            Collection collection = collectionService.find(context, importDao.getCollectionId());

            protocol.add("Vytváření Item " + daoName + " v kolekci " + collection.getName());

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

    private Protocol createProtocolWriter(Path batchDir) throws IOException {
        Path protocolPath = Paths.get(batchDir.toAbsolutePath().toString(), PROTOCOL_FILE);
        if (Files.exists(protocolPath)) {
            Files.delete(protocolPath);
        }
        Files.createFile(protocolPath);
        BufferedWriter bw = Files.newBufferedWriter(protocolPath, Charset.forName("UTF-8"));
        return new FileProtocol(bw);
    }

    private ImportBatch checkAndPrepareBatch(Path batchDir, ImportConfig config, Protocol protocol, Context context) throws IOException, SQLException {
        protocol.add("Kontrola a příprava dávky " + batchDir.toAbsolutePath());

        ImportBatch importBatch = new ImportBatch();
        importBatch.setBatchDir(batchDir);
        importBatch.setBatchName(batchDir.getFileName().toString());
        importBatch.setProtocol(protocol);

        List<Path> daoDirs = new LinkedList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(batchDir)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    daoDirs.add(path);
                    protocol.add("Nalezené DAO " + path.getFileName());
                }
            }
        }

        for (Path daoDir : daoDirs) {        	
            ImportDao importDao = checkAndPrepareDao(daoDir, config, protocol, context);
            if (!importDao.getBundles().isEmpty()) {
                importBatch.addDao(importDao);
            } else {
                protocol.add("Digitalizát " + daoDir.toAbsolutePath() + " neobsahuje žádné soubory a proto nebude dále zpracováván.");
            }
        }

        return importBatch;
    }

    private ImportDao checkAndPrepareDao(Path daoDir, ImportConfig config, 
    		Protocol protocol, Context context) throws IOException, SQLException {
        protocol.add("Kontrola a příprava DAO " + daoDir.getFileName());

        ImportDao importDao = new ImportDao();

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
        DaoImportWorker daoImportWorker = new DaoImportWorker(context, metadataFieldService, droidService,
        		jhoveService,
        		config, protocol, daoDir);
        daoImportWorker.prepare(importDao);       
                
        return importDao;
    }
    

	private void processBitstreamDir(ImportConfig config, Path generatedDir, ImportDao importDao, Path contentFile,
			Protocol protocol, Dao daoXml) {
		// TODO Auto-generated method stub
		
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
