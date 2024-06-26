package cz.tacr.elza.service.da;

import com.lightcomp.kads.mets.MetsReaderWriter;
import com.lightcomp.kads.premis.PremisReaderWriter;
import cz.tacr.da.ApiException;
import cz.tacr.da.controller.vo.DownloadDownloadAips;
import cz.tacr.da.controller.vo.DownloadDownloadStatus;
import cz.tacr.da.controller.vo.RequestState;
import cz.tacr.da.controller.vo.UpdatedAips;
import cz.tacr.da.controller.vo.UpdatedInfo;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.connector.DaConnector;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoFile;
import cz.tacr.elza.domain.DaDaoFileFolder;
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
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DaLocalCacheRepository;
import cz.tacr.elza.repository.DaRemoteRepositorySyncRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.service.UserService;
import gov.loc.mets.v1_11.schema.MetsType;
import gov.loc.premis.v3.PremisComplexType;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Transactional
    public void synchronizaceDA(ArrDigitalRepository digitalRepository) {
        DaRemoteRepositorySync daRemoteRepositorySync = getDaRemoteRepositorySync(digitalRepository);
        UpdatedAips updatesAips = daConnector.updates(digitalRepository, DA_UPDATE_PAGE_SIZE, daRemoteRepositorySync.getNextQuery());

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

        daRemoteRepositorySync.setNextQuery(updatesAips.getNextQuery());
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
            DaAip aip = aipRepository.findById(aipId).orElseThrow(() -> new ObjectNotFoundException("Nebyl nalezen AIP=" + aipId, AIP_NOT_FOUND));
            DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
            DaLocalCache localCache = daLocalCacheRepository.findByAipStateAndAipTypeIn(aipState, EnumSet.of(AipType.METADATA_BASE, AipType.AIP_BASE));

            if (localCache != null) {
                MetsType metsType = null;
                PremisComplexType premisComplexType = null;
                try {
                    Path zip = Paths.get(localCache.getFilePath());

                    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zip))) {
                        ZipEntry entry;
                        while ((entry = zipInputStream.getNextEntry()) != null) {
                            if (entry.getName().endsWith("METS.xml")) {
                                metsType = MetsReaderWriter.unmarshal(zipInputStream);
                            } else if (entry.getName().endsWith("PREMIS.xml")) {
                                premisComplexType = PremisReaderWriter.unmarshal(zipInputStream);
                            }
                        }
                    }
                } catch (IOException | JAXBException e) {
                    logger.error("Došlo k chybě při načtení souborů z lokální cache pro AIP={}", aipId, e);
                }
                try {
                    applicationContext.getBean(DaService.class).createDaoStructure(aip, metsType, premisComplexType);
                } catch (Exception e) {
                    logger.error("Došlo k chybě při při vytváření struktury DAO pro AIP={}", aipId, e);
                }
            } else {
                logger.info("Nebyla nalezena lokální cache s metadaty pro AIP={}", aipId);
            }
        }
    }

    @Transactional
    public void createDaoStructure(DaAip aip, MetsType metsType, PremisComplexType premisComplexType) {
        DaoProcessor daoProcessor = applicationContext.getBean(DaoProcessor.class, aip, metsType, premisComplexType);
        daoProcessor.process();
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
    public void changeQueueItemsState(List<DaSyncQueueItem> syncQueueItemList, DaSyncQueueItem.QueueItemState state) {
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
                    Path zipDir = createZip(aipDir);
                    applicationContext.getBean(DaService.class).createLocalCache(aipState, digitalRepository, aipType, zipDir);
                }
            }
        }


        // Odstranit dočasné soubory a adresáře
        try (Stream<Path> str = Files.walk(tempDir)) {
            str.map(Path::toFile).forEach(File::delete);
        }
    }

    private Path createZip(File aipDir) throws IOException {
        String workDirAip = resourcePathResolver.getAipDir().toString();
        File zip = new File(workDirAip + aipDir.getName() + ".zip");
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
    public void createLocalCache(DaAipState aipState, ArrDigitalRepository digitalRepository, AipType aipType, Path filePath) {
        DaAip aip = aipState.getDaAip();
        DaSyncQueueItem syncQueueItem = syncQueueItemRepository.findByCodeAndDigitalRepository(aip.getCode(), digitalRepository);

        DaLocalCache localCache = new DaLocalCache();
        localCache.setAipType(aipType);
        localCache.setFilePath(filePath.getFileName().toString());
        localCache.setSyncQueueItem(syncQueueItem);
        localCache.setAipState(aipState);
        daLocalCacheRepository.save(localCache);
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
}
