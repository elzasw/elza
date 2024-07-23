package cz.tacr.elza.service.da;

import com.lightcomp.kads.mets.MetsReaderWriter;
import com.lightcomp.kads.premis.PremisReaderWriter;
import cz.tacr.da.ApiException;
import cz.tacr.da.controller.vo.DownloadDownloadAips;
import cz.tacr.da.controller.vo.DownloadDownloadStatus;
import cz.tacr.da.controller.vo.IngestIngestResult;
import cz.tacr.da.controller.vo.IngestIngestStatus;
import cz.tacr.da.controller.vo.IngestPackageIngestSuccess;
import cz.tacr.da.controller.vo.RequestState;
import cz.tacr.da.controller.vo.UpdatedAips;
import cz.tacr.da.controller.vo.UpdatedInfo;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.connector.DaConnector;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.DaDaoFileFolderVO;
import cz.tacr.elza.controller.vo.DaDaoFileVO;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoFile;
import cz.tacr.elza.domain.DaDaoFileFolder;
import cz.tacr.elza.domain.DaDaoItem;
import cz.tacr.elza.domain.DaDaoRelation;
import cz.tacr.elza.domain.DaLocalCache;
import cz.tacr.elza.domain.DaRemoteRepositorySync;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaDaoFileFolderRepository;
import cz.tacr.elza.repository.DaDaoFileRepository;
import cz.tacr.elza.repository.DaDaoItemRepository;
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DaLocalCacheRepository;
import cz.tacr.elza.repository.DaRemoteRepositorySyncRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.AipService;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.DaoLevelViewService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.utils.EadReaderWriter;
import gov.loc.mets.v1_11.schema.MetsType;
import gov.loc.premis.v3.PremisComplexType;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.collections4.CollectionUtils;
import org.archivists.ead3.schema.Ead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static cz.tacr.elza.exception.codes.ArrangementCode.AIP_NOT_FOUND;

@Service
public class DaService {

    private static final Logger logger = LoggerFactory.getLogger(DaService.class);

    private static final Integer DA_UPDATE_PAGE_SIZE = 1000;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private UserService userService;
    @Autowired
    private PackageInfoService packageInfoService;
    @Autowired
    private ArrangementInternalService arrangementInternalService;
    @Autowired
    private DaoLevelViewService levelViewService;
    @Autowired
    private ResourcePathResolver resourcePathResolver;
    @Autowired
    private DaConnector daConnector;
    @Autowired
    private DaSyncQueueItemRepository syncQueueItemRepository;
    @Autowired
    private DaRemoteRepositorySyncRepository remoteRepositorySyncRepository;
    @Autowired
    private DaChangeRepository changeRepository;
    @Autowired
    private DaDaoRepository daoRepository;
    @Autowired
    private DaDaoRelationRepository daoRelationRepository;
    @Autowired
    private DaDaoFileRepository daoFileRepository;
    @Autowired
    private DaDaoFileFolderRepository daoFileFolderRepository;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private DaLocalCacheRepository daLocalCacheRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private AipService aipService;
    @Autowired
    private DaDaoFileRepository daDaoFileRepository;
    @Autowired
    private DaoLinkRepository daoLinkRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private DaDaoItemRepository daoItemRepository;
    @Autowired
    private ClientFactoryVO clientFactoryVO;
    @Autowired
    private ExternalSystemService externalSystemService;

    @Scheduled(cron = "0 0 2 * * *")
    public void synchronizeDaRepositories() {
        List<ArrDigitalRepository> digitalRepositories = externalSystemService.findDigitalRepository();
        for (ArrDigitalRepository digitalRepository : digitalRepositories) {
            try {
                applicationContext.getBean(DaService.class).synchronizeDA(digitalRepository);
            } catch (Exception e) {
                logger.error("Došlo k chybě při pokusu o stažení změn z DA pro externí systém ID={} {}", digitalRepository.getExternalSystemId(), e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void synchronizeDA(ArrDigitalRepository digitalRepository) {
        DaRemoteRepositorySync daRemoteRepositorySync = getDaRemoteRepositorySync(digitalRepository);
        String nextQuery = daRemoteRepositorySync.getNextQuery();
        UpdatedAips updatesAips;
        do {
            updatesAips = daConnector.updates(digitalRepository, DA_UPDATE_PAGE_SIZE, nextQuery);
            nextQuery = updatesAips.getNextQuery();

            if (CollectionUtils.isNotEmpty(updatesAips.getAipIds())) {
                List<DaSyncQueueItem> syncQueueItemList = new ArrayList<>();

                List<String> aipCodes = updatesAips.getAipIds().stream()
                        .map(UpdatedInfo::getAipId)
                        .toList();

                Map<String, DaSyncQueueItem> existingSyncQueueItemMap = syncQueueItemRepository.findByCodeInAndDigitalRepository(aipCodes, digitalRepository).stream()
                        .collect(Collectors.toMap(DaSyncQueueItem::getCode, Function.identity()));

                for (UpdatedInfo updatedInfo : updatesAips.getAipIds()) {
                    DaSyncQueueItem syncQueueItem = existingSyncQueueItemMap.get(updatedInfo.getAipId());

                    if (syncQueueItem == null) {
                        syncQueueItem = new DaSyncQueueItem();
                        syncQueueItem.setCode(updatedInfo.getAipId());
                        syncQueueItem.setState(DaSyncQueueItem.QueueItemState.IMPORT_NEW);
                        syncQueueItem.setDigitalRepository(digitalRepository);
                    } else {
                        syncQueueItem.setState(DaSyncQueueItem.QueueItemState.UPDATE);
                    }

                    syncQueueItem.setAipVersion(updatedInfo.getAipVersion());
                    syncQueueItemList.add(syncQueueItem);
                }

                syncQueueItemRepository.saveAll(syncQueueItemList);
            }
        } while (updatesAips.getAipIds().size() == DA_UPDATE_PAGE_SIZE);

        daRemoteRepositorySync.setNextQuery(nextQuery);
        remoteRepositorySyncRepository.save(daRemoteRepositorySync);
    }

    private DaRemoteRepositorySync getDaRemoteRepositorySync(ArrDigitalRepository digitalRepository) {
        DaRemoteRepositorySync daRemoteRepositorySync = remoteRepositorySyncRepository.findByDigitalRepository(digitalRepository);
        if (daRemoteRepositorySync == null) {
            daRemoteRepositorySync = new DaRemoteRepositorySync();
            daRemoteRepositorySync.setDigitalRepository(digitalRepository);
        }
        return daRemoteRepositorySync;
    }

    public void createDaoStructure(List<Integer> aipIds) {
        for (Integer aipId : aipIds) {
            DaAip aip = findAipById(aipId);
            DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
            DaLocalCache localCache = daLocalCacheRepository.findByAipStateAndAipTypeIn(aipState, EnumSet.of(AipType.METADATA_BASE, AipType.AIP_BASE));

            if (aipState.getFund() == null) {
                logger.info("AIP={} není navázaný na fund", aipId);
                continue;
            }

            if (localCache == null) {
                logger.info("Nebyla nalezena lokální cache s metadaty pro AIP={}", aipId);
                continue;
            }

            MetsType metsType = null;
            PremisComplexType premisComplexType = null;
            try {
                Path zip = Paths.get(localCache.getFilePath());

                Path tempDir = Files.createTempDirectory("unzipped");

                try (ZipInputStream zipInputStream = new ZipInputStream((Files.newInputStream(zip)))) {
                    ZipEntry entry;
                    while ((entry = zipInputStream.getNextEntry()) != null) {
                        Path filePath = tempDir.resolve(entry.getName());
                        if (entry.isDirectory()) {
                            Files.createDirectories(filePath);
                        } else {
                            Files.createDirectories(filePath.getParent());
                            Files.copy(zipInputStream, filePath);
                        }
                    }
                }
                try (Stream<Path> str = Files.walk(tempDir).filter(path -> path.toString().endsWith("METS.xml"))) {
                    Path mets = str.findFirst().orElseThrow(() -> new RuntimeException("Balíček neobsahuje soubor METS.xml"));
                    metsType = MetsReaderWriter.unmarshal(mets);
                }
                try (Stream<Path> str = Files.walk(tempDir).filter(path -> path.toString().endsWith("PREMIS.xml"))) {
                    Path premis = str.findFirst().orElseThrow(() -> new RuntimeException("Balíček neobsahuje soubor PREMIS.xml"));
                    premisComplexType = PremisReaderWriter.unmarshal(premis);
                }

                try {
                    applicationContext.getBean(DaService.class).createDaoStructure(aip, metsType, premisComplexType, tempDir);
                } catch (Exception e) {
                    logger.error("Došlo k chybě při při vytváření struktury DAO pro AIP={}", aipId, e);
                }

                // Odstranit dočasné soubory a adresáře
                try (Stream<Path> str = Files.walk(tempDir)) {
                    str.map(Path::toFile).forEach(File::delete);
                }
            } catch (IOException | JAXBException e) {
                logger.error("Došlo k chybě při načtení souborů z lokální cache pro AIP={}", aipId, e);
            }
        }
    }

    public Ead loadEadFile(Path tempDir, String filePath) throws IOException, JAXBException {
        try (Stream<Path> str = Files.walk(tempDir).filter(path -> path.toString().endsWith(filePath))) {
            Path ead = str.findFirst().orElseThrow(() -> new RuntimeException("Balíček neobsahuje soubor " + filePath));
            return EadReaderWriter.unmarshal(ead);
        }
    }

    public DaAip findAipById(Integer aipId) {
        return aipRepository.findById(aipId).orElseThrow(() -> new ObjectNotFoundException("Nebyl nalezen AIP=" + aipId, AIP_NOT_FOUND));
    }

    public DaDao findDaoById(Integer daoId) {
        return daoRepository.findById(daoId).orElseThrow(() -> new ObjectNotFoundException("Nebylo nalezeno DAO=" + daoId, AIP_NOT_FOUND));
    }

    @Transactional
    public void createDaoStructure(DaAip aip, MetsType metsType, PremisComplexType premisComplexType, Path tempDir) {
        DaoProcessor daoProcessor = applicationContext.getBean(DaoProcessor.class, aip, metsType, premisComplexType, tempDir);
        daoProcessor.process();
    }

    @Transactional
    public void deleteDaoStructure(List<Integer> aipIds) {
        List<DaAip> aipList = aipRepository.findByIdAndLinkNotExists(aipIds);

        List<DaAipState> stateList = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList);
        List<DaDao> daDaoList = daoRepository.findByAipInAndDeleteChangeIsNull(aipList);
        List<DaDaoRelation> daDaoRelationList = daoRelationRepository.findByDaoInAndDeleteChangeIsNull(daDaoList);
        List<DaDaoFileFolder> daDaoFileFolderList = daoFileFolderRepository.findByRepresentationDaoInAndDeleteChangeIsNull(daDaoList);
        List<DaDaoFile> daDaoFileList = daoFileRepository.findByDaoInAndDeleteChangeIsNull(daDaoList);
        List<DaDaoItem> daDaoItemList = daoItemRepository.findByDaoInAndDeleteChangeIsNull(daDaoList);

        DaChange change = createDaChange(null, DaChangeType.AIP_UPDATE);

        stateList.forEach(s -> s.setMetadataLoad(false));
        daDaoList.forEach(d -> d.setDeleteChange(change));
        daDaoRelationList.forEach(r -> r.setDeleteChange(change));
        daDaoFileFolderList.forEach(f -> f.setDeleteChange(change));
        daDaoFileList.forEach(f -> f.setDeleteChange(change));
        daDaoItemList.forEach(i -> i.setDeleteChange(change));

        aipStateRepository.saveAll(stateList);
        daoRepository.saveAll(daDaoList);
        daoRelationRepository.saveAll(daDaoRelationList);
        daoFileFolderRepository.saveAll(daDaoFileFolderList);
        daoFileRepository.saveAll(daDaoFileList);
        daoItemRepository.saveAll(daDaoItemList);

        levelViewService.deleteDisconnectedLevelViews(change);
    }

    public DaChange createDaChange(DaAip aip, DaChangeType changeType) {
        DaChange change = new DaChange();
        change.setChangeDate(LocalDateTime.now());
        change.setUser(userService.getLoggedUser());
        change.setDaAip(aip);
        change.setType(changeType);
        return changeRepository.save(change);
    }

    public DaDao createDaDao(DaAip aip, DaChange change, String code, String label, DaDao.DaoType type) {
        DaDao dao = new DaDao();
        dao.setAip(aip);
        dao.setCode(code);
        dao.setCreateChange(change);
        dao.setType(type);
        dao.setLabel(label);
        return daoRepository.save(dao);
    }

    public DaDaoRelation createDaDaoRelation(DaDao dao, DaDao parentDao, DaChange change) {
        DaDaoRelation daoRelation = new DaDaoRelation();
        daoRelation.setCreateChange(change);
        daoRelation.setDao(dao);
        daoRelation.setParentDao(parentDao);
        return daoRelationRepository.save(daoRelation);
    }

    public DaDaoFileFolder createDaDaoFileFolder(DaDao representationDao, DaChange change, String label, @Nullable DaDaoFileFolder parentFileFolder) {
        DaDaoFileFolder daoFileFolder = new DaDaoFileFolder();
        daoFileFolder.setCreateChange(change);
        daoFileFolder.setParentFileFolder(parentFileFolder);
        daoFileFolder.setLabel(label);
        daoFileFolder.setRepresentationDao(representationDao);
        return daoFileFolderRepository.save(daoFileFolder);
    }

    public DaDaoFile createDaDaoFile(DaChange change, DaDao dao, DaDaoFileFolder daoFileFolder, String checksum, String checksumType,
                                     String mimeType, BigInteger size, Integer imageHeight, Integer imageWidth, String sourceXDimensionUnit,
                                     Integer sourceXDimensionValue, String sourceYDimensionUnit, Integer sourceYDimensionValue,
                                     String duration, String description, String fileName) {
        DaDaoFile daoFile = new DaDaoFile();
        daoFile.setCreateChange(change);
        daoFile.setDao(dao);
        daoFile.setDaoFileFolder(daoFileFolder);
        daoFile.setChecksum(checksum);
        daoFile.setChecksumType(checksumType);
        daoFile.setMimeType(mimeType);
        daoFile.setSize(size);
        daoFile.setImageHeight(imageHeight);
        daoFile.setImageWidth(imageWidth);
        daoFile.setSourceXDimensionUnit(sourceXDimensionUnit);
        daoFile.setSourceXDimensionValue(sourceXDimensionValue);
        daoFile.setSourceYDimensionUnit(sourceYDimensionUnit);
        daoFile.setSourceYDimensionValue(sourceYDimensionValue);
        daoFile.setDuration(duration);
        daoFile.setDescription(description);
        daoFile.setFileName(fileName);
        return daoFileRepository.save(daoFile);
    }

    @Transactional
    public List<DaSyncQueueItem> getNextItems(int pageSize, DaSyncQueueItem.QueueItemState... states) {
        Pageable pageable = PageRequest.of(0, pageSize);

        Iterable<DaSyncQueueItem> syncQueueItemIterable = syncQueueItemRepository.findByStates(Arrays.asList(states), pageable);
        List<DaSyncQueueItem> syncQueueItemList = new ArrayList<>();

        if (syncQueueItemIterable.iterator().hasNext()) {
            DaSyncQueueItem firstSyncQueueItem = syncQueueItemIterable.iterator().next();
            ArrDigitalRepository digitalRepository = firstSyncQueueItem.getDigitalRepository();

            for (DaSyncQueueItem syncQueueItem : syncQueueItemIterable) {
                if (syncQueueItem.getDigitalRepository().getExternalSystemId().equals(digitalRepository.getExternalSystemId())) {
                    syncQueueItemList.add(syncQueueItem);
                }
            }
        }

        return syncQueueItemList;
    }

    @Transactional
    public void updateAipToQueueItems(List<DaSyncQueueItem> syncQueueItemList) {
        if (CollectionUtils.isNotEmpty(syncQueueItemList)) {
            List<String> codes = syncQueueItemList.stream().map(DaSyncQueueItem::getCode).collect(Collectors.toList());
            Map<String, DaAip> aipMap = aipRepository.findByCodeIn(codes).stream()
                    .collect(Collectors.toMap(DaAip::getCode, aip -> aip));
            for (DaSyncQueueItem syncQueueItem : syncQueueItemList) {
                DaAip aip = aipMap.getOrDefault(syncQueueItem.getCode(), null);
                syncQueueItem.setAip(aip);
            }
            syncQueueItemRepository.saveAll(syncQueueItemList);
        }
    }

    @Transactional
    public void changeQueueItemsState(Collection<DaSyncQueueItem> syncQueueItemList, DaSyncQueueItem.QueueItemState state) {
        if (CollectionUtils.isNotEmpty(syncQueueItemList)) {
            for (DaSyncQueueItem syncQueueItem : syncQueueItemList) {
                syncQueueItem.setState(state);
            }
            syncQueueItemRepository.saveAll(syncQueueItemList);
        }
    }

    public void processPackageInfo(ArrDigitalRepository digitalRepository, InputStream tempZipInputStream, AipType aipType) throws IOException {
        Path tempDir = Files.createTempDirectory("unzipped");

        try (ZipInputStream zipInputStream = new ZipInputStream((tempZipInputStream))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path filePath = tempDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zipInputStream, filePath);
                }
            }
        }

        File[] tempFiles = tempDir.toFile().listFiles();

        if (tempFiles != null) {
            Set<File> aipDirSet = Stream.of(tempFiles)
                    .filter(file -> file.isDirectory() && file.toPath().getParent().equals(tempDir))
                    .collect(Collectors.toSet());

            for (File aipDir : aipDirSet) {
                DaAipState aipState = null;
                try (Stream<Path> str = Files.walk(aipDir.toPath()).filter(path -> path.toString().endsWith("PACKAGE-INFO.xml"))) {
                    Path packageInfo = str.findFirst().orElseThrow(() -> new RuntimeException("Balíček neobsahuje soubor PACKAGE-INFO.xml"));
                    File file = packageInfo.toFile();
                    try {
                        aipState = packageInfoService.processPackageInfo(digitalRepository, file);
                    } catch (Exception e) {
                        logger.error("Nastala chyba při zpracování souboru package-info.xml", e);
                    }
                }

                if (aipState != null) {
                    Path zipDir = createZip(aipDir, resourcePathResolver.getAipDir());
                    applicationContext.getBean(DaService.class).createLocalCache(aipState, digitalRepository, aipType, zipDir);
                }
            }
        }


        // Odstranit dočasné soubory a adresáře
        try (Stream<Path> str = Files.walk(tempDir)) {
            str.map(Path::toFile).forEach(File::delete);
        }
    }

    private Path createZip(File aipDir, Path folder) throws IOException {
        String workDirAip = folder.toString();
        File workDirAipFile = new File(workDirAip);
        if (!workDirAipFile.exists()) {
            workDirAipFile.mkdirs();
        }
        File zip = new File(workDirAip+ "/" + aipDir.getName() + ".zip");
        FileOutputStream fos = new FileOutputStream(zip);
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        zipFile(aipDir, aipDir.getName(), zipOut);
        zipOut.close();
        fos.close();
        return zip.toPath();
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    @Transactional
    public Path createOutputDir(List<DaSyncQueueItem> syncQueueItemList) throws IOException {
        List<DaLocalCache> localCaches = daLocalCacheRepository.findBySyncQueueItemIn(syncQueueItemList);

        Path tempDir = Files.createTempDirectory("result");

        for (DaLocalCache localCache : localCaches) {
            Path zip = Paths.get(localCache.getFilePath());

            Path aipDir = tempDir.resolve(localCache.getSyncQueueItem().getCode());
            Files.createDirectories(aipDir);

            try (ZipInputStream zipInputStream = new ZipInputStream((Files.newInputStream(zip)))) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    Path filePath = aipDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        Files.copy(zipInputStream, filePath);
                    }
                }
            }
        }

        Path exportDir = Files.createTempDirectory("exportZip");
        createZip(tempDir.toFile(), exportDir);


        // Odstranit dočasné soubory a adresáře
        try (Stream<Path> str = Files.walk(tempDir)) {
            str.map(Path::toFile).forEach(File::delete);
        }

        return exportDir;
    }

//    @Transactional
//    public DaDaoFileFolderVO findByAipIdAndTypeAndDeleteChangeIsNull(Integer aipId) {
//        DaAip aip = aipService.getAip(aipId);
//        List<DaDao> daDaoList = daoRepository.findByAipAndDeleteChangeIsNull(aip);
//        Map<Integer, DaDaoFileFolderVO> itemMap = new HashMap<>();
//        DaDaoFileFolderVO root = null;
//        List<DaDaoFileFolder> folders = daoFileFolderRepository.findByRepresentationDaoInAndDeleteChangeIsNull(daDaoList);
//
//        List<DaDao> fileList = daDaoList
//                .stream()
//                .filter(daDao -> daDao.getType() == DaDao.DaoType.FILE)
//                .toList();
//        List<DaDaoFile> files = daDaoFileRepository.findByDaoInAndDeleteChangeIsNull(fileList);
//
//        //LOGICAL
//        List<DaDao> logicalList = daDaoList
//                .stream()
//                .filter(daDao -> daDao.getType() == DaDao.DaoType.LOGICAL)
//                .toList();
//
//
//        //METADATA-----
//        List<DaDao> metadataList = daDaoList
//                .stream()
//                .filter(
//                    daDao -> daDao.getType() == DaDao.DaoType.METAAMD ||
//                    daDao.getType() == DaDao.DaoType.METADMDINHERENT ||
//                    daDao.getType() == DaDao.DaoType.METADMDCONTEXTUAL
//                ).toList();
//
//        List<DaDaoFileVO> metadataFiles = daDaoFileRepository.findByDaoInAndDeleteChangeIsNull(metadataList)
//                .stream()
//                .map(clientFactoryVO::createDaDaoFileVO)
//                .toList();
//        //--------------
//
//        for (DaDaoFileFolder folder : folders) {
//            DaDaoFileFolderVO item = clientFactoryVO.createDaDaoFileFolderVO(folder);
//            itemMap.put(folder.getDaoFileFolderId(), item);
//        }
//
//        for (DaDaoFileFolder folder : folders) {
//            DaDaoFileFolderVO item = itemMap.get(folder.getDaoFileFolderId());
//            if (folder.getParentFileFolder() == null) {
//                root = item;
//            } else {
//                DaDaoFileFolderVO parent = itemMap.get(folder.getParentFileFolder().getDaoFileFolderId());
//                if(parent.getChildFolders()  == null) {
//                    parent.setChildFolders(new ArrayList<>());
//                }
//                parent.getChildFolders().add(item);
//            }
//        }
//
//        for (DaDaoFile file : files) {
//            if (file.getDaoFileFolder() == null) {
//                if(root.getChildFiles() == null) {
//                    root.setChildFiles(new ArrayList<>());
//                }
//                root.getChildFiles().add(clientFactoryVO.createDaDaoFileVO(file));
//            } else {
//                DaDaoFileFolderVO parent = itemMap.get(file.getDaoFileFolder().getDaoFileFolderId());
//                if(parent.getChildFiles() == null) {
//                    parent.setChildFiles(new ArrayList<>());
//                }
//                parent.getChildFiles().add(clientFactoryVO.createDaDaoFileVO(file));
//            }
//        }
//
//        DaDaoFileFolderVO logicalRoot = null;
//        Map<Integer, DaDaoFileFolderVO> log = new HashMap<>();
//        for (DaDao dao : logicalList) {
//            DaDaoFileFolderVO item = new DaDaoFileFolderVO();
//            item.setDaoFileFolderId(dao.getDaoId());
//            item.setLabel(dao.getLabel());
//            log.put(item.getDaoFileFolderId(), item);
//        }
//
//        for (DaDao dao : logicalList) {
//            List<DaDaoRelation> relations = daoRelationRepository.findByDaoInAndDeleteChangeIsNull(Collections.singletonList(dao));
//            DaDaoFileFolderVO item = log.get(dao.getDaoId());
//            if(relations.isEmpty()) {
//                logicalRoot = item;
//            } else {
//                for(DaDaoRelation relation : relations) {
//                    DaDaoFileFolderVO parent = log.get(relation.getParentDao().getDaoId());
//                    if(parent.getChildFolders()  == null) {
//                        parent.setChildFolders(new ArrayList<>());
//                    }
//                    parent.getChildFolders().add(item);
//                }
//            }
//        }
//
//        DaDaoFileFolderVO mets = new DaDaoFileFolderVO();
//        mets.setDaoFileFolderId(-1);
//        mets.setLabel("Balíček (METS.xml)");
//
//        DaDaoFileFolderVO reprezentace = new DaDaoFileFolderVO();
//        reprezentace.setDaoFileFolderId(-2);
//        reprezentace.setLabel("Reprezentace");
//        if(root != null) {
//            reprezentace.setChildFolders(Collections.singletonList(root));
//        }
//
//        DaDaoFileFolderVO logical = new DaDaoFileFolderVO();
//        logical.setDaoFileFolderId(-3);
//        logical.setLabel("Logická struktura");
//        if(logicalRoot != null) {
//            logical.setChildFolders(Collections.singletonList(logicalRoot));
//        }
//
//        DaDaoFileFolderVO metadata = new DaDaoFileFolderVO();
//        metadata.setDaoFileFolderId(-4);
//        metadata.setLabel("Metadata");
//        metadata.setChildFiles(metadataFiles);
//
//        mets.setChildFolders(Arrays.asList(reprezentace, logical, metadata));
//
//        return mets;
//    }
@Transactional
public DaDaoFileFolderVO findByAipIdAndTypeAndDeleteChangeIsNull(Integer aipId) {
    DaAip aip = aipService.getAip(aipId);
    List<DaDao> daDaoList = daoRepository.findByAipAndDeleteChangeIsNull(aip);
    Map<Integer, DaDaoFileFolderVO> itemMap = new HashMap<>();

        List<DaDaoFileFolder> folders = daoFileFolderRepository.findByRepresentationDaoInAndDeleteChangeIsNull(daDaoList);
        List<DaDaoFile> files = daDaoFileRepository.findByDaoInAndDeleteChangeIsNull(filterDaDaoByType(daDaoList, DaDao.DaoType.FILE));
        List<DaDaoFileVO> fileVOs = new ArrayList<>();
        for (DaDaoFile f : files) {
            DaDaoRelation relation =
                    daoRelationRepository.findByDaoInAndDeleteChangeIsNull(Collections.singletonList(f.getDao()))
                            .stream()
                            .filter(i -> i.getParentDao().getType() == DaDao.DaoType.LOGICAL)
                            .toList()
                            .get(0);
            DaDaoFileVO fileVO =  clientFactoryVO.createDaDaoFileVO(f);
            fileVO.setParentFolderLogical(createParent(createFolderVO(relation.getParentDao().getDaoId(), relation.getParentDao().getLabel())));
            fileVOs.add(fileVO);
        };

        createRepresentationFolderMap(folders, itemMap);

        DaDaoFileFolderVO representationRoot = buildFolderHierarchy(folders, itemMap);
        addFilesToFolders(fileVOs, itemMap, representationRoot);

        Map<Integer, DaDaoFileFolderVO> logicalMap = new HashMap<>();
        createLogicalFolderMap(daDaoList, logicalMap);
        List<DaDao> logicalList = daDaoList.stream().filter(i-> i.getType() == DaDao.DaoType.LOGICAL).toList();

        DaDaoFileFolderVO logicalRoot = buildLogicalStructure(logicalList, logicalMap);
        addFilesToFoldersLogical(fileVOs, logicalMap, logicalRoot);
        DaDaoFileFolderVO metadata = buildMetadataStructure(daDaoList);

        DaDaoFileFolderVO representation = createFolderVO(-2, "Reprezentace", Collections.singletonList(representationRoot));
        DaDaoFileFolderVO logical = createFolderVO(-3, "Logická struktura", Collections.singletonList(logicalRoot));
        return createFolderVO(-1, "Balíček", Arrays.asList(representation, logical, metadata));
    }

private DaDaoFileFolderVO createParent(DaDaoFileFolderVO src) {
    DaDaoFileFolderVO result = new DaDaoFileFolderVO();
    result.setUuid(src.getUuid());
    result.setDaoFileFolderId(src.getDaoFileFolderId());
    result.setLabel(src.getLabel());
    return result;
}

    private List<DaDao> filterDaDaoByType(List<DaDao> daDaoList, DaDao.DaoType type) {
        return daDaoList.stream()
                .filter(daDao -> daDao.getType() == type)
                .toList();
    }

    private void createRepresentationFolderMap(List<DaDaoFileFolder> folders, Map<Integer, DaDaoFileFolderVO> itemMap) {
        folders.forEach(folder -> {
            DaDaoFileFolderVO item = clientFactoryVO.createDaDaoFileFolderVO(folder);
            itemMap.put(folder.getDaoFileFolderId(), item);
        });
    }

    private void createLogicalFolderMap(List<DaDao> daDaoList, Map<Integer, DaDaoFileFolderVO> itemMap) {
        List<DaDao> logicalList = filterDaDaoByType(daDaoList, DaDao.DaoType.LOGICAL);
        logicalList.forEach(dao -> {
            DaDaoFileFolderVO item = createFolderVO(dao.getDaoId(), dao.getLabel());
            itemMap.put(item.getDaoFileFolderId(), item);
        });

    }

    private DaDaoFileFolderVO buildFolderHierarchy(List<DaDaoFileFolder> folders, Map<Integer, DaDaoFileFolderVO> itemMap) {
        DaDaoFileFolderVO root = null;
        for (DaDaoFileFolder folder : folders) {
            DaDaoFileFolderVO item = itemMap.get(folder.getDaoFileFolderId());
            if (folder.getParentFileFolder() == null) {
                root = item;
            } else {
                DaDaoFileFolderVO parent = itemMap.get(folder.getParentFileFolder().getDaoFileFolderId());
                if (parent.getChildFolders() == null) {
                    parent.setChildFolders(new ArrayList<>());
                }
                parent.getChildFolders().add(item);
            }
        }
        return root;
    }

    private void addFilesToFoldersLogical(List<DaDaoFileVO> files, Map<Integer, DaDaoFileFolderVO> itemMap, DaDaoFileFolderVO root) {
        for (DaDaoFileVO file : files) {
            DaDaoFileVO copy = clientFactoryVO.copyFile(file);
            copy.setUuid(UUID.randomUUID().toString());
            DaDaoFileFolderVO parent = itemMap.getOrDefault(
                    file.getParentFolderLogical() != null ? file.getParentFolderLogical().getDaoFileFolderId() : null, root
            );
            if (parent.getChildFiles() == null){
                parent.setChildFiles(new ArrayList<>());
            }
            parent.getChildFiles().add(copy);
        }
    }
    private void addFilesToFolders(List<DaDaoFileVO> files, Map<Integer, DaDaoFileFolderVO> itemMap, DaDaoFileFolderVO root) {
        for (DaDaoFileVO file : files) {
            DaDaoFileFolderVO parent = itemMap.getOrDefault(
                    file.getDaoFileFolder() != null ? file.getDaoFileFolder().getDaoFileFolderId() : null, root
            );
            if (parent.getChildFiles() == null){
                parent.setChildFiles(new ArrayList<>());
            }
            parent.getChildFiles().add(file);
        }
    }

    private DaDaoFileFolderVO buildLogicalStructure(List<DaDao> daDaoList, Map<Integer, DaDaoFileFolderVO> itemMap) {
        DaDaoFileFolderVO logicalRoot = null;
        for (DaDao dao : daDaoList) {
            List<DaDaoRelation> relations = daoRelationRepository.findByDaoInAndDeleteChangeIsNull(Collections.singletonList(dao));
            DaDaoFileFolderVO item = itemMap.get(dao.getDaoId());
            if (relations.isEmpty()) {
                logicalRoot = item;
            } else {
                for (DaDaoRelation relation : relations) {
                    DaDaoFileFolderVO parent = itemMap.get(relation.getParentDao().getDaoId());
                    if (parent.getChildFolders() == null) {
                        parent.setChildFolders(new ArrayList<>());
                    }
                    item.setParentFolderLogical(createParent(parent));
                    parent.getChildFolders().add(item);

                    if (item.getChildFiles() == null) {
                        item.setChildFiles(new ArrayList<>());
                    }
                }
            }
        }
        return logicalRoot;
    }

    private DaDaoFileFolderVO buildMetadataStructure(List<DaDao> daDaoList) {
        List<DaDao> metadataList = daDaoList.stream()
                .filter(daDao -> daDao.getType() == DaDao.DaoType.METAAMD
                        || daDao.getType() == DaDao.DaoType.METADMDINHERENT
                        || daDao.getType() == DaDao.DaoType.METADMDCONTEXTUAL)
                .toList();

        List<DaDaoFileVO> metadataFiles = daDaoFileRepository.findByDaoInAndDeleteChangeIsNull(metadataList)
                .stream()
                .map(clientFactoryVO::createDaDaoFileVO)
                .toList();

        DaDaoFileFolderVO metadata = new DaDaoFileFolderVO();
        metadata.setUuid(UUID.randomUUID().toString());
        metadata.setDaoFileFolderId(-4);
        metadata.setLabel("Metadata");
        metadata.setChildFiles(metadataFiles);

        return metadata;
    }

    private DaDaoFileFolderVO createFolderVO(int id, String label) {
        DaDaoFileFolderVO vo = new DaDaoFileFolderVO();
        vo.setUuid(UUID.randomUUID().toString());
        vo.setDaoFileFolderId(id);
        vo.setLabel(label);
        return vo;
    }

    private DaDaoFileFolderVO createFolderVO(int id, String label, List<DaDaoFileFolderVO> children) {
        DaDaoFileFolderVO vo = createFolderVO(id, label);
        if(children != null) {
            vo.setChildFolders(children);
        }
        return vo;
    }

    @Transactional
    public void createLocalCache(DaAipState aipState, ArrDigitalRepository digitalRepository, AipType aipType, Path filePath) {
        DaLocalCache localCache = daLocalCacheRepository.findByAipState(aipState);
        if (localCache == null) {
            localCache = new DaLocalCache();
        }

        DaAip aip = aipState.getDaAip();
        DaSyncQueueItem syncQueueItem = syncQueueItemRepository.findByCodeAndDigitalRepository(aip.getCode(), digitalRepository);
        if (syncQueueItem == null) {
            syncQueueItem = createSyncQueueItem(aip.getCode(), aip, digitalRepository, DaSyncQueueItem.QueueItemState.IMPORT_OK, aipState.getAipVersion());
        }

        localCache.setAipType(aipType);
        localCache.setFilePath(filePath.toAbsolutePath().toString());
        localCache.setSyncQueueItem(syncQueueItem);
        localCache.setAipState(aipState);
        daLocalCacheRepository.save(localCache);
    }

    @Transactional
    public DaSyncQueueItem createSyncQueueItem(String code, DaAip aip, ArrDigitalRepository digitalRepository,
                                               DaSyncQueueItem.QueueItemState queueItemState, String aipVersion) {
        DaSyncQueueItem syncQueueItem = new DaSyncQueueItem();
        syncQueueItem.setCode(code);
        syncQueueItem.setAip(aip);
        syncQueueItem.setDigitalRepository(digitalRepository);
        syncQueueItem.setAipVersion(aipVersion);
        syncQueueItem.setState(queueItemState);
        return syncQueueItemRepository.save(syncQueueItem);
    }


    public String downloadAips(ArrDigitalRepository digitalRepository, List<DaSyncQueueItem> syncQueueItemList, AipType aipType) {
        List<String> aipIds = syncQueueItemList.stream()
                .map(DaSyncQueueItem::getCode)
                .toList();

        DownloadDownloadAips downloadDownloadAips = new DownloadDownloadAips();
        downloadDownloadAips.setDipType(aipType.getValue());
        downloadDownloadAips.setAipIds(aipIds);

        return daConnector.downloadAips(digitalRepository, downloadDownloadAips);
    }

    public boolean downloadStatusFinished(ArrDigitalRepository digitalRepository, String batchId) {
        DownloadDownloadStatus status = daConnector.downloadStatus(digitalRepository, batchId);
        return status.getState() == RequestState.FINISHED;
    }

    public Path downloadDownload(ArrDigitalRepository digitalRepository, String batchId) throws ApiException, IOException {
        byte[] file =  daConnector.downloadDownload(digitalRepository, batchId);

        Path tempZip = Files.createTempFile("temp", ".zip");
        try (FileOutputStream fos = new FileOutputStream(tempZip.toFile())) {
            fos.write(file);
        }

        return tempZip;
    }

    public Path downloadFileTransfer(ArrDigitalRepository digitalRepository, String batchId) throws IOException {
        Path downloadDir = Files.createTempDirectory(batchId);
        daConnector.downloadFileTransfer(digitalRepository, batchId, downloadDir);
        try (Stream<Path> str = Files.walk(downloadDir).filter(p -> p.getFileName().endsWith(".zip"))) {
            return str.findFirst().orElseThrow(() -> new IllegalStateException("Nenalezen stažený soubor přes Filetransfer"));
        }
    }

    public boolean ingestStatusFinished(ArrDigitalRepository digitalRepository, String batchId) {
        IngestIngestStatus status = daConnector.ingestStatus(digitalRepository, batchId);
        return status.getState() == RequestState.FINISHED;
    }

    public List<String> ingestResult(ArrDigitalRepository digitalRepository, String batchId) {
        IngestIngestResult result = daConnector.ingestResult(digitalRepository, batchId);
        return result.getAccepted().stream()
                .map(IngestPackageIngestSuccess::getAipId)
                .collect(Collectors.toList());
    }

    public void ingestFileTransfer(ArrDigitalRepository digitalRepository, Path exportDir, String batchId) {
        daConnector.ingestFileTransfer(digitalRepository, exportDir, batchId);
    }

    @Transactional
    public void createDaoLink(Integer aipId, Integer daoId, Integer nodeId, ArrDaoLink.LinkType linkType) {
        ArrNode node = nodeRepository.getOneCheckExist(nodeId);
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, node);
        DaAip aip = findAipById(aipId);
        DaDao daDao = null;

        if (daoId != null) {
            daDao = findDaoById(daoId);
        }

        ArrDaoLink arrDaoLink = new ArrDaoLink();
        arrDaoLink.setNode(node);
        arrDaoLink.setCreateChange(change);
        arrDaoLink.setAip(aip);
        arrDaoLink.setDaDao(daDao);
        arrDaoLink.setLinkType(linkType);
        daoLinkRepository.save(arrDaoLink);
    }

    @Transactional
    public void deleteDaoLink(Integer daoLinkId) {
        ArrDaoLink arrDaoLink = daoLinkRepository.getOneCheckExist(daoLinkId);

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.DELETE_DAO_LINK, arrDaoLink.getNode());
        arrDaoLink.setDeleteChange(change);
        daoLinkRepository.save(arrDaoLink);
    }
}
