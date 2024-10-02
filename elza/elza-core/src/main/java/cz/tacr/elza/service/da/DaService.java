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
import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.connector.DaConnector;
import cz.tacr.elza.controller.vo.AipUpdateType;
import cz.tacr.elza.controller.vo.DaDaoType;
import cz.tacr.elza.controller.vo.DaoLink;
import cz.tacr.elza.controller.vo.DaoLinksResult;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
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
import cz.tacr.elza.domain.DaLevelView;
import cz.tacr.elza.domain.DaLocalCache;
import cz.tacr.elza.domain.DaRemoteRepositorySync;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaDaoFileFolderRepository;
import cz.tacr.elza.repository.DaDaoFileRepository;
import cz.tacr.elza.repository.DaDaoItemRepository;
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DaLevelViewRepository;
import cz.tacr.elza.repository.DaLocalCacheRepository;
import cz.tacr.elza.repository.DaRemoteRepositorySyncRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataUnitdateRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DaoLevelViewService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.utils.EadReaderWriter;
import gov.loc.mets.v1_11.schema.AmdSecType;
import gov.loc.mets.v1_11.schema.DivType;
import gov.loc.mets.v1_11.schema.MdSecType;
import gov.loc.mets.v1_11.schema.Mets;
import gov.loc.mets.v1_11.schema.MetsType;
import gov.loc.mets.v1_11.schema.StructMapType;
import gov.loc.premis.v3.IntellectualEntity;
import gov.loc.premis.v3.ObjectComplexType;
import gov.loc.premis.v3.ObjectIdentifierComplexType;
import gov.loc.premis.v3.PremisComplexType;
import gov.loc.premis.v3.SignificantPropertiesComplexType;
import gov.loc.premis.v3.StringPlusAuthority;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.archivists.ead3.schema.Abstract;
import org.archivists.ead3.schema.Archdesc;
import org.archivists.ead3.schema.Daterange;
import org.archivists.ead3.schema.Did;
import org.archivists.ead3.schema.Ead;
import org.archivists.ead3.schema.Fromdate;
import org.archivists.ead3.schema.Todate;
import org.archivists.ead3.schema.Unitdatestructured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
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
    private DaoLinkRepository daoLinkRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private DaDaoItemRepository daoItemRepository;
    @Autowired
    private ExternalSystemService externalSystemService;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private DaLevelViewRepository daLevelViewRepository;
    @Autowired
    private ArrangementService arrangementService;
    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private EventNotificationService eventNotificationService;
    @Autowired
    private FundRepository fundRepository;
    @Autowired
    private DaDaoItemRepository daDaoItemRepository;
    @Autowired
    private DataStringRepository dataStringRepository;
    @Autowired
    private DataUnitdateRepository dataUnitdateRepository;

    public void synchronizeDaRepository(String code) {
        logger.debug("Spuštěna synchronizace s DA pro externí systém CODE={}", code);
        ArrDigitalRepository arrDigitalRepository = externalSystemService.findDigitalRepositoryByCode(code);
        applicationContext.getBean(DaService.class).synchronizeDA(arrDigitalRepository);
        logger.debug("Dokončena synchronizace s DA pro externí systém CODE={}", code);
    }

    @Transactional
    public void synchronizeDA(ArrDigitalRepository digitalRepository) {
        DaRemoteRepositorySync daRemoteRepositorySync = getDaRemoteRepositorySync(digitalRepository);
        String nextQuery = daRemoteRepositorySync.getNextQuery();
        UpdatedAips updatesAips;
        do {
            logger.debug("Volání externího systému CODE={} s query {}", digitalRepository.getCode(), nextQuery);
            updatesAips = daConnector.updates(digitalRepository, DA_UPDATE_PAGE_SIZE, nextQuery);
            nextQuery = updatesAips.getNextQuery();

            if (CollectionUtils.isNotEmpty(updatesAips.getAipIds())) {
                logger.debug("Z externího systému CODE={} se vrátilo {} aip ID", digitalRepository.getCode(), updatesAips.getAipIds().size());
                List<String> aipCodes = updatesAips.getAipIds().stream()
                        .map(UpdatedInfo::getAipId)
                        .toList();

                List<DaAip> aipList = aipRepository.findByCodeIn(aipCodes);
                Map<String, DaAip> aipMap = aipList.stream()
                        .collect(Collectors.toMap(DaAip::getCode, a -> a));
                Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                        .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

                for (UpdatedInfo updatedInfo : updatesAips.getAipIds()) {
                    DaAip aip = aipMap.getOrDefault(updatedInfo.getAipId(), null);

                    DaSyncQueueItem.QueueItemState queueItemState = DaSyncQueueItem.QueueItemState.IMPORT_NEW;
                    AipType aipType = AipType.PACKAGE_INFO;
                    if (aip != null) {
                        DaAipState aipState = stateMap.get(aip);
                        queueItemState = DaSyncQueueItem.QueueItemState.UPDATE;

                        if (BooleanUtils.isTrue(aipState.getCompleteAipLoad())) {
                            aipType = AipType.AIP_BASE;
                        } else if (BooleanUtils.isTrue(aipState.getMetadataLoad())) {
                            aipType = AipType.METADATA_BASE;
                        }
                    }

                    createSyncQueueItem(updatedInfo.getAipId(), aip, digitalRepository, queueItemState, updatedInfo.getAipVersion(), aipType, true);
                }
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

    public void doCreateDaoStructure(List<Integer> aipIds, boolean forceUpdate) {
        for (Integer aipId : aipIds) {
            DaAip aip = findAipById(aipId);
            DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
            DaLocalCache localCache = daLocalCacheRepository.findByAipStateAndAipTypeIn(aipState,
                    EnumSet.of(AipType.METADATA_BASE, AipType.AIP_BASE),
                    getQueueImportStates());

            if (aipState.getFund() == null) {
                logger.info("AIP={} není navázaný na fund", aipId);
                continue;
            }

            if (localCache == null) {
                logger.info("Nebyla nalezena lokální cache s metadaty pro AIP={}", aipId);
                continue;
            }

            MetsType metsType;
            PremisComplexType premisComplexType;
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

                applicationContext.getBean(DaService.class).createDaoStructure(aip, metsType, premisComplexType, tempDir, forceUpdate);

                // Odstranit dočasné soubory a adresáře
                try (Stream<Path> str = Files.walk(tempDir)) {
                    str.map(Path::toFile).forEach(File::delete);
                }

                if (localCache.getFilePathMetadata() != null && !localCache.getFilePath().equals(localCache.getFilePathMetadata())) {
                    Path oldFile = Paths.get(localCache.getFilePathMetadata());
                    oldFile.toFile().delete();
                }

                localCache.setFilePathMetadata(localCache.getFilePath());
                daLocalCacheRepository.save(localCache);

                aipState.setMetadataError(false);
                aipState.setMetadataErrorException(null);
                aipState.setAipVersionMetadata(aipState.getAipVersion());
                aipStateRepository.save(aipState);

            } catch (Exception e) {
                logger.error("Došlo k chybě při zpracování metadat pro AIP={}", aipId, e);
                aipState.setMetadataError(true);
                aipState.setMetadataErrorException(e.getMessage());
                aipStateRepository.save(aipState);
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
    public void createDaoStructure(DaAip aip, MetsType metsType, PremisComplexType premisComplexType, Path tempDir, boolean forceUpdate) {
        DaoProcessor daoProcessor = applicationContext.getBean(DaoProcessor.class, aip, metsType, premisComplexType, tempDir, forceUpdate);
        daoProcessor.process();
    }

    @Transactional
    public void createDaoStructure(List<Integer> aipIds) {
        List<DaAip> aipList = aipRepository.findAllById(aipIds);
        Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

        List<DaAipState> stateList = new ArrayList<>();

        for (DaAip aip : aipList) {
            DaAipState aipState = stateMap.get(aip);
            if (aipState.getFund() == null) {
                ArrFund arrFund = fundRepository.findByInternalCode(aipState.getFundCode());
                aipState.setFund(arrFund);
            }
            if (BooleanUtils.isNotTrue(aipState.getMetadataLoad()) && BooleanUtils.isNotTrue(aipState.getCompleteAipLoad()) && aipState.getFund() != null) {
                aipState.setMetadataLoad(true);
                stateList.add(aipState);

                createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(), DaSyncQueueItem.QueueItemState.UPDATE,
                        aipState.getAipVersion(), AipType.METADATA_BASE, true);
            }
        }

        aipStateRepository.saveAll(stateList);
    }

    @Transactional
    public void deleteDaoStructure(List<Integer> aipIds) {
        List<DaAip> aipList = aipRepository.findByIdAndLinkNotExists(aipIds);

        Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

        List<DaAip> deletedAipList = new ArrayList<>();

        for (DaAip aip : aipList) {
            DaAipState aipState = stateMap.get(aip);
            if (BooleanUtils.isTrue(aipState.getMetadataLoad()) && BooleanUtils.isNotTrue(aipState.getCompleteAipLoad())) {
                createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(), DaSyncQueueItem.QueueItemState.IMPORT_OK,
                        aipState.getAipVersion(), AipType.PACKAGE_INFO, true);
                deletedAipList.add(aip);
            }
        }

        List<DaAipState> stateList = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(deletedAipList);
        List<DaDao> daDaoList = daoRepository.findByAipInAndDeleteChangeIsNull(deletedAipList);
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

        List<DaLocalCache> localCacheList = daLocalCacheRepository.findByAipInAndQueueItemStatesIn(deletedAipList, getQueueImportStates());
        for (DaLocalCache localCache : localCacheList) {
            if (localCache.getFilePath() != null) {
                if (localCache.getFilePathMetadata() != null && !localCache.getFilePath().equals(localCache.getFilePathMetadata())) {
                    Path oldFile = Paths.get(localCache.getFilePathMetadata());
                    oldFile.toFile().delete();
                }
                Path oldFile = Paths.get(localCache.getFilePath());
                oldFile.toFile().delete();
            }
        }
        daLocalCacheRepository.deleteAll(localCacheList);
    }

    @Transactional
    public void aipDownloadCompleteAip(List<Integer> aipIds) {
        List<DaAip> aipList = aipRepository.findAllById(aipIds);
        Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

        List<DaAipState> stateList = new ArrayList<>();

        for (DaAip aip : aipList) {
            DaAipState aipState = stateMap.get(aip);
            if (BooleanUtils.isTrue(aipState.getMetadataLoad()) && BooleanUtils.isNotTrue(aipState.getCompleteAipLoad())) {
                aipState.setCompleteAipLoad(true);
                stateList.add(aipState);

                createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(), DaSyncQueueItem.QueueItemState.UPDATE,
                        aipState.getAipVersion(), AipType.AIP_BASE, true);
            }
        }

        aipStateRepository.saveAll(stateList);
    }

    @Transactional
    public void aipDeleteCompleteAip(List<Integer> aipIds) {
        List<DaAip> aipList = aipRepository.findAllById(aipIds);
        Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

        List<DaAipState> stateList = new ArrayList<>();

        for (DaAip aip : aipList) {
            DaAipState aipState = stateMap.get(aip);
            if (BooleanUtils.isTrue(aipState.getCompleteAipLoad())) {
                aipState.setCompleteAipLoad(false);
                stateList.add(aipState);

                createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(), DaSyncQueueItem.QueueItemState.UPDATE,
                        aipState.getAipVersion(), AipType.METADATA_BASE, true);
            }
        }

        aipStateRepository.saveAll(stateList);
    }

    @Transactional
    public void aipUpdateAip(AipUpdateType type, List<Integer> aipIds) {
        if (type == AipUpdateType.DOWNLOAD_UPDATE) {
            List<DaAip> aipList = aipRepository.findAllById(aipIds);
            Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                    .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

            for (DaAip aip : aipList) {
                DaAipState aipState = stateMap.get(aip);
                AipType aipType = AipType.PACKAGE_INFO;
                if (BooleanUtils.isTrue(aipState.getCompleteAipLoad())) {
                    aipType = AipType.AIP_BASE;
                } else if (BooleanUtils.isTrue(aipState.getMetadataLoad())) {
                    aipType = AipType.METADATA_BASE;
                }
                createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(), DaSyncQueueItem.QueueItemState.UPDATE,
                        aipState.getAipVersion(), aipType, true);
            }
        } else if (type == AipUpdateType.DB_UPDATE) {
            doCreateDaoStructure(aipIds, false);
        } else if (type == AipUpdateType.FORCE_UPDATE) {
            doCreateDaoStructure(aipIds, true);
        }
    }

    @Transactional
    public void aipExportAip(List<Integer> aipIds) {
        List<DaAip> aipList = aipRepository.findByIdAndLinkExists(aipIds);
        Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

        for (DaAip aip : aipList) {
            DaAipState aipState = stateMap.get(aip);

            AipType aipType = AipType.PACKAGE_INFO;
            if (BooleanUtils.isTrue(aipState.getCompleteAipLoad())) {
                aipType = AipType.AIP_BASE;
            } else if (BooleanUtils.isTrue(aipState.getMetadataLoad())) {
                aipType = AipType.METADATA_BASE;
            }

            try {
                Path aipDir = Files.createTempDirectory(aip.getCode());
                createPackageInfo(aip, aipDir);
                createMets(aip, aipDir);
                createEad(aip, aipDir);

                Path aipOutputDir = resourcePathResolver.getAipDir().resolve("out");
                Path outputZip = createZip(aipDir.toFile(), aipOutputDir);

                DaSyncQueueItem syncQueueItem = createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(), DaSyncQueueItem.QueueItemState.EXPORT_NEW,
                        aipState.getAipVersion(), aipType, true);
                createExportLocalCache(aipState, aipType, outputZip, syncQueueItem);
            } catch (IOException | JAXBException e) {
                logger.error("Došlo k chybě při vytváření změnového balíčku AIP={}", aip.getCode(), e);
            }
        }
    }

    private void createEad(DaAip aip, Path aipDir) throws JAXBException {
        List<DaDao> daDaoList = daoRepository.findByAipAndDeleteChangeIsNull(aip);
        List<DaDaoItem> daDaoItemList = daDaoItemRepository.findByDaoInAndDeleteChangeIsNull(daDaoList);
        Did did = new Did();
        for (DaDaoItem daDaoItem : daDaoItemList) {
            ArrData data = daDaoItem.getData();
            DataType type = data.getType();


            switch (type) {
                case STRING -> {
                    ArrDataString arrDataString = dataStringRepository.findById(data.getDataId()).orElse(null);
                    if (arrDataString != null) {
                        Abstract abs = new Abstract();
                        abs.getContent().add(arrDataString.getStringValue());
                        did.getMDid().add(abs);
                    }
                }
                case UNITDATE -> {
                    ArrDataUnitdate arrDataUnitdate = dataUnitdateRepository.findById(data.getDataId()).orElse(null);
                    if (arrDataUnitdate != null) {
                        Unitdatestructured unitdatestructured = new Unitdatestructured();
                        Daterange daterange = new Daterange();
                        Fromdate fromDate = new Fromdate();
                        fromDate.setStandarddate(arrDataUnitdate.getValueFrom());
                        daterange.setFromdate(fromDate);
                        Todate todate = new Todate();
                        todate.setStandarddate(arrDataUnitdate.getValueTo());
                        daterange.setTodate(todate);
                        unitdatestructured.setDaterange(daterange);
                        did.getMDid().add(unitdatestructured);
                    }

                }
                default -> {}
            }


        }
        Archdesc archdesc = new Archdesc();
        archdesc.setDid(did);
        Ead ead = new Ead();
        ead.setArchdesc(archdesc);
        EadReaderWriter.marshal(ead, Path.of(aipDir.toString(), "EAD.xml"));
    }

    private void createPackageInfo(DaAip aip, Path aipDir) throws IOException {
        DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);

        List<ObjectComplexType> objectComplexTypeList = new ArrayList<>();

        List<SignificantPropertiesComplexType> significantProperties = new ArrayList<>();
        significantProperties.add(createSignificantPropertiesElement("AIP_VERSION", aipState.getAipVersion()));
        significantProperties.add(createSignificantPropertiesElement("AIP_SIZE", String.valueOf(aipState.getAipSize())));
        significantProperties.add(createSignificantPropertiesElement("INSTITUTION_ID", aipState.getInstitutionCode()));

        List<ObjectIdentifierComplexType> identifiers = new ArrayList<>();
        identifiers.add(createObjectIdentifierComplexType("FONDS_ID", aipState.getFundCode()));
        identifiers.add(createObjectIdentifierComplexType("AIP_ID", aip.getCode()));
        IntellectualEntity intellectualEntity = new IntellectualEntity(identifiers, null, significantProperties, null, null, null, null, null, null, null, null, null, null);

        objectComplexTypeList.add(intellectualEntity);
        PremisComplexType premisComplexType = new PremisComplexType(objectComplexTypeList, null, null, null, aipState.getAipVersionMetadata());
        JAXBElement<PremisComplexType> wrappedElement = XmlUtils.wrapElement("premisComplexType", premisComplexType);
        byte[] data = XmlUtils.marshallData(wrappedElement, PremisComplexType.class);
        File packageInfoFile = new File(aipDir.toString() + "/PACKAGE-INFO.xml");
        Files.write(packageInfoFile.toPath(), data);
    }

    private ObjectIdentifierComplexType createObjectIdentifierComplexType(String type, String value) {
        StringPlusAuthority stringPlusAuthority = new StringPlusAuthority();
        stringPlusAuthority.setValue(type);
        return new ObjectIdentifierComplexType(stringPlusAuthority, value, null);
    }

    private SignificantPropertiesComplexType createSignificantPropertiesElement(String type, String value) {
        StringPlusAuthority stringPlusAuthority = new StringPlusAuthority();
        stringPlusAuthority.setValue(type);
        JAXBElement<StringPlusAuthority> stringPlusAuthorityElement = XmlUtils.wrapElement("stringPlusAuthority", stringPlusAuthority);
        JAXBElement<String> stringElement = XmlUtils.wrapElement("string", value);
        List<JAXBElement<?>> elements = new ArrayList<>();
        elements.add(stringPlusAuthorityElement);
        elements.add(stringElement);
        return new SignificantPropertiesComplexType(elements);
    }

    private void createMets(DaAip aip, Path aipDir) throws JAXBException {
        Mets mets = new Mets();
        List<DaDao> daDaoList = daoRepository.findByAipAndDeleteChangeIsNull(aip);
        for (DaDao daDao : daDaoList) {
            switch (daDao.getType()) {
                case REPRESENTATION -> {
                    StructMapType structMapType = new StructMapType();
                    structMapType.setTYPE("PHYSICAL");
                    DivType divType = new DivType();
                    divType.setLABEL("Representations");
                    divType.setID(daDao.getCode());
                    structMapType.setDiv(divType);
                    mets.getStructMap().add(structMapType);
                }
                case METAAMD -> {
                    String[] daoSplit = daDao.getLabel().split(":");
                    if (daoSplit.length == 2) {
                        MdSecType mdSecType = new MdSecType();
                        MdSecType.MdRef mdRef = new MdSecType.MdRef();
                        mdRef.setMDTYPE(daoSplit[0]);
                        mdRef.setHref(daoSplit[1]);
                        mdSecType.setMdRef(mdRef);
                        AmdSecType amdSecType = new AmdSecType();
                        amdSecType.getDigiprovMD().add(mdSecType);
                        mets.getAmdSec().add(amdSecType);
                    }
                }
                case METADMDCONTEXTUAL, METADMDINHERENT -> {
                    MdSecType mdSecType = new MdSecType();
                    MdSecType.MdRef mdRef = new MdSecType.MdRef();
                    mdRef.setHref(daDao.getLabel());
                    mdSecType.setMdRef(mdRef);
                    AmdSecType amdSecType = new AmdSecType();
                    amdSecType.getDigiprovMD().add(mdSecType);
                    mets.getAmdSec().add(amdSecType);
                }
                case LOGICAL -> {
                    StructMapType structMapType = new StructMapType();
                    structMapType.setTYPE("LOGICAL");
                    String[] daoSplit = daDao.getLabel().split(":");
                    DivType divType = new DivType();
                    divType.setTYPE("LOGICAL");
                    if (daoSplit.length == 2) {
                        divType.setTYPE(daoSplit[0]);
                        divType.setLABEL(daoSplit[1]);
                    } else {
                        divType.setLABEL(daDao.getLabel());
                    }
                    structMapType.setDiv(divType);
                    mets.getStructMap().add(structMapType);
                }
                default -> {}
            }
        }

        MetsReaderWriter.marshal(mets, Path.of(aipDir.toString(), "METS.xml"));
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

    public DaDaoItem createDaDaoItem(DaDao dao, DaChange change, RulItemType itemType, RulItemSpec itemSpec, ArrData data) {
        DaDaoItem daoItem = new DaDaoItem();
        daoItem.setCreateChange(change);
        daoItem.setDao(dao);
        daoItem.setItemType(itemType);
        daoItem.setItemSpec(itemSpec);
        daoItem.setData(data);
        return daoItemRepository.save(daoItem);
    }

    @Transactional
    public List<DaSyncQueueItem> getNextItems(int pageSize, DaSyncQueueItem.QueueItemState... states) {
        Pageable pageable = PageRequest.of(0, pageSize);

        Iterable<DaSyncQueueItem> syncQueueItemIterable = syncQueueItemRepository.findByStates(Arrays.asList(states), pageable);
        List<DaSyncQueueItem> syncQueueItemList = new ArrayList<>();

        if (syncQueueItemIterable.iterator().hasNext()) {
            DaSyncQueueItem firstSyncQueueItem = syncQueueItemIterable.iterator().next();
            ArrDigitalRepository digitalRepository = firstSyncQueueItem.getDigitalRepository();
            AipType aipType = firstSyncQueueItem.getAipType();

            for (DaSyncQueueItem syncQueueItem : syncQueueItemIterable) {
                if (syncQueueItem.getDigitalRepository().getExternalSystemId().equals(digitalRepository.getExternalSystemId())
                        && Objects.equals(syncQueueItem.getAipType(), aipType)) {
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

    public void processPackageInfo(ArrDigitalRepository digitalRepository, InputStream tempZipInputStream, AipType aipType, List<DaSyncQueueItem> syncQueueItemList) throws IOException {
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

            Map<String, DaSyncQueueItem> syncQueueItemMap = syncQueueItemList.stream()
                    .collect(Collectors.toMap(DaSyncQueueItem::getCode, Function.identity()));

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

                if (aipState != null && aipType != AipType.PACKAGE_INFO) {
                    Path zipDir = createZip(aipDir, resourcePathResolver.getAipDir());
                    DaSyncQueueItem syncQueueItem = syncQueueItemMap.getOrDefault(aipState.getDaAip().getCode(), null);
                    applicationContext.getBean(DaService.class).createImportLocalCache(aipState, digitalRepository, aipType, zipDir, syncQueueItem);
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

    @Transactional
    public void createImportLocalCache(DaAipState aipState, ArrDigitalRepository digitalRepository, AipType aipType, Path filePath, DaSyncQueueItem syncQueueItem) {
        DaLocalCache localCache = daLocalCacheRepository.findByAipAndQueueItemStatesIn(aipState.getDaAip(), getQueueImportStates());
        if (localCache == null) {
            localCache = new DaLocalCache();
        }

        DaAip aip = aipState.getDaAip();
        if (syncQueueItem == null) {
            syncQueueItem = createSyncQueueItem(aip.getCode(), aip, digitalRepository, DaSyncQueueItem.QueueItemState.IMPORT_OK, aipState.getAipVersion(), aipType, true);
        }

        if (localCache.getFilePath() != null
                && !localCache.getFilePath().equals(localCache.getFilePathMetadata())
                && !localCache.getFilePath().equals(filePath.toAbsolutePath().toString())) {
            Path oldFile = Paths.get(localCache.getFilePath());
            oldFile.toFile().delete();
        }

        localCache.setAipType(aipType);
        localCache.setFilePath(filePath.toAbsolutePath().toString());
        localCache.setSyncQueueItem(syncQueueItem);
        localCache.setAipState(aipState);
        daLocalCacheRepository.save(localCache);
    }

    @Transactional
    public void createExportLocalCache(DaAipState aipState, AipType aipType, Path filePath, DaSyncQueueItem syncQueueItem) {
        DaLocalCache localCache = new DaLocalCache();
        localCache.setAipType(aipType);
        localCache.setFilePath(filePath.toAbsolutePath().toString());
        localCache.setSyncQueueItem(syncQueueItem);
        localCache.setAipState(aipState);
        daLocalCacheRepository.save(localCache);
    }

    @Transactional
    public DaSyncQueueItem createSyncQueueItem(String code, DaAip aip, ArrDigitalRepository digitalRepository,
                                               DaSyncQueueItem.QueueItemState queueItemState, String aipVersion, AipType aipType, boolean active) {
        List<DaSyncQueueItem.QueueItemState> queueItemStates = getQueueItemStates(queueItemState);
        syncQueueItemRepository.updateActiveByCodeAndDigitalRepositoryAndStateInAndActiveIsTrue(code, digitalRepository, queueItemStates);

        DaSyncQueueItem syncQueueItem = new DaSyncQueueItem();
        syncQueueItem.setCode(code);
        syncQueueItem.setAip(aip);
        syncQueueItem.setDigitalRepository(digitalRepository);
        syncQueueItem.setAipVersion(aipVersion);
        syncQueueItem.setState(queueItemState);
        syncQueueItem.setAipType(aipType);
        syncQueueItem.setActive(active);
        return syncQueueItemRepository.save(syncQueueItem);
    }

    private List<DaSyncQueueItem.QueueItemState> getQueueItemStates(DaSyncQueueItem.QueueItemState queueItemState) {
        List<DaSyncQueueItem.QueueItemState> queueItemStates = new ArrayList<>();
        switch (queueItemState) {
            case IMPORT_NEW, IMPORT_OK, IMPORT_ERROR, UPDATE:
                queueItemStates.add(DaSyncQueueItem.QueueItemState.IMPORT_NEW);
                queueItemStates.add(DaSyncQueueItem.QueueItemState.IMPORT_OK);
                queueItemStates.add(DaSyncQueueItem.QueueItemState.IMPORT_ERROR);
                queueItemStates.add(DaSyncQueueItem.QueueItemState.UPDATE);
                break;
            case EXPORT_NEW, EXPORT_OK, EXPORT_ERROR:
                queueItemStates.add(DaSyncQueueItem.QueueItemState.EXPORT_NEW);
                queueItemStates.add(DaSyncQueueItem.QueueItemState.EXPORT_OK);
                queueItemStates.add(DaSyncQueueItem.QueueItemState.EXPORT_ERROR);
                break;
        }
        return queueItemStates;
    }

    public static Collection<DaSyncQueueItem.QueueItemState> getQueueImportStates() {
        List<DaSyncQueueItem.QueueItemState> states = new ArrayList<>();
        states.add(DaSyncQueueItem.QueueItemState.UPDATE);
        states.add(DaSyncQueueItem.QueueItemState.IMPORT_NEW);
        states.add(DaSyncQueueItem.QueueItemState.IMPORT_OK);
        states.add(DaSyncQueueItem.QueueItemState.IMPORT_ERROR);
        return states;
    }

    public static Collection<DaSyncQueueItem.QueueItemState> getQueueExportStates() {
        List<DaSyncQueueItem.QueueItemState> states = new ArrayList<>();
        states.add(DaSyncQueueItem.QueueItemState.EXPORT_NEW);
        states.add(DaSyncQueueItem.QueueItemState.EXPORT_OK);
        states.add(DaSyncQueueItem.QueueItemState.EXPORT_ERROR);
        return states;
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
    public void connectToJP(Integer nodeId, Integer daAipId) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaAip daAip = findAipById(daAipId);
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
        ArrDaoLink arrDaoLink = new ArrDaoLink();
        arrDaoLink.setAip(daAip);
        arrDaoLink.setNode(arrNode);
        arrDaoLink.setLinkType(ArrDaoLink.LinkType.AIP);
        arrDaoLink.setCreateChange(change);
        daoLinkRepository.save(arrDaoLink);
    }

    @Transactional
    public void connectPartToJP(Integer nodeId, Integer daAipId, Integer daDaoId) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaAip daAip = findAipById(daAipId);
        DaDao daDao = findDaoById(daDaoId);
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
        ArrDaoLink arrDaoLink = new ArrDaoLink();
        arrDaoLink.setAip(daAip);
        arrDaoLink.setNode(arrNode);
        arrDaoLink.setLinkType(ArrDaoLink.LinkType.PART_AIP);
        arrDaoLink.setDaDao(daDao);
        arrDaoLink.setCreateChange(change);
        daoLinkRepository.save(arrDaoLink);
    }

    @Transactional
    public void createJPFromSelected(Integer nodeId, Integer daAipId, Integer daDaoId) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaAip daAip = findAipById(daAipId);
        DaDao daDao = findDaoById(daDaoId);
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
        ArrNode newNode = arrangementService.createNode(arrNode.getFund(), generateUuid(), change);
        ArrLevel arrLevel = new ArrLevel();
        arrLevel.setNodeParent(arrNode);
        arrLevel.setNode(newNode);
        arrLevel.setCreateChange(change);
        Integer maxPosition = levelRepository.findMaxPositionUnderParent(arrNode);
        if (maxPosition == null) {
            maxPosition = 0;
        }
        arrLevel.setPosition(maxPosition + 1);
        levelRepository.save(arrLevel);
        ArrFundVersion fundVersion = fundVersionRepository
                .findByFundIdAndLockChangeIsNull(arrNode.getFund().getFundId());
        final ArrLevel parentLevel = arrangementService.lockNode(arrNode, fundVersion, change);

        eventNotificationService.publishEvent(EventFactory.createAddNodeEvent(EventType.ADD_LEVEL_UNDER, fundVersion,
                parentLevel, arrLevel));
        if (daDao != null) {
            connectPartToJP(newNode.getNodeId(), daAip.getAipId(), daDao.getDaoId());
        } else {
            connectToJP(newNode.getNodeId(), daAip.getAipId());
        }
    }

    @Transactional
    public void connectSelectedToJP(Integer nodeId, Integer daAipId, Integer daDaoId) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaAip daAip = findAipById(daAipId);
        DaDao daDao = findDaoById(daDaoId);
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
        ArrDaoLink arrDaoLink = new ArrDaoLink();
        arrDaoLink.setAip(daAip);
        arrDaoLink.setNode(arrNode);
        arrDaoLink.setLinkType(ArrDaoLink.LinkType.COMPONENT_AIP);
        arrDaoLink.setDaDao(daDao);
        arrDaoLink.setCreateChange(change);
        daoLinkRepository.save(arrDaoLink);
    }

    @Transactional
    public void createAndLinkFromSelected(Integer nodeId, Integer daAipId, Integer daDaoId) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaAip daAip = findAipById(daAipId);
        DaDao daDao = findDaoById(daDaoId);
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
        ArrNode newNode = arrangementService.createNode(arrNode.getFund(), generateUuid(), change);
        ArrLevel arrLevel = new ArrLevel();
        Integer maxPosition = levelRepository.findMaxPositionUnderParent(arrNode);
        if (maxPosition == null) {
            maxPosition = 0;
        }
        arrLevel.setNodeParent(arrNode);
        arrLevel.setNode(newNode);
        arrLevel.setCreateChange(change);
        arrLevel.setPosition(maxPosition + 1);
        levelRepository.save(arrLevel);
        ArrFundVersion fundVersion = fundVersionRepository
                .findByFundIdAndLockChangeIsNull(arrNode.getFund().getFundId());
        final ArrLevel parentLevel = arrangementService.lockNode(arrNode, fundVersion, change);

        eventNotificationService.publishEvent(EventFactory.createAddNodeEvent(EventType.ADD_LEVEL_UNDER, fundVersion,
                parentLevel, arrLevel));
        ArrDaoLink arrDaoLink = new ArrDaoLink();
        arrDaoLink.setAip(daAip);
        arrDaoLink.setNode(arrNode);
        arrDaoLink.setLinkType(ArrDaoLink.LinkType.COMPONENT_AIP);
        arrDaoLink.setCreateChange(change);
        if (daDao != null) {
           arrDaoLink.setDaDao(daDao);
        }
        daoLinkRepository.save(arrDaoLink);
    }

    @Transactional
    public void bulkConnectToJP(Integer nodeId, List<Integer> daAipIdList) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        for (Integer daAipId : daAipIdList) {
            DaAip daAip = findAipById(daAipId);
            ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
            ArrDaoLink arrDaoLink = new ArrDaoLink();
            arrDaoLink.setAip(daAip);
            arrDaoLink.setNode(arrNode);
            arrDaoLink.setLinkType(ArrDaoLink.LinkType.AIP);
            arrDaoLink.setCreateChange(change);
            daoLinkRepository.save(arrDaoLink);
        }
    }

    @Transactional
    public void bulkCreateFromSelectedToJP(Integer nodeId, List<Integer> daAipIdList, Integer dalevelViewId) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaLevelView levelView = daLevelViewRepository.findById(dalevelViewId).orElse(null);
        if (levelView == null) {
            logger.error("Nebylo nalezeno level view s předaným ID. ID={}", dalevelViewId);
            throw new ObjectNotFoundException("Nebylo nalezeno level view s předaným ID. ID=" + dalevelViewId, BaseCode.ID_NOT_EXIST);
        }
        for (Integer daAipId : daAipIdList) {
            DaAip daAip = findAipById(daAipId);
            ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
            Integer maxPosition = levelRepository.findMaxPositionUnderParent(arrNode);
            if (maxPosition == null) {
                maxPosition = 0;
            }
            int position = maxPosition + 1;
            ArrNode newNode = arrangementService.createNode(arrNode.getFund(), generateUuid(), change);
            ArrLevel arrLevel = new ArrLevel();
            arrLevel.setNodeParent(arrNode);
            arrLevel.setNode(newNode);
            arrLevel.setCreateChange(change);
            arrLevel.setPosition(position);
            levelRepository.save(arrLevel);
            ArrFundVersion fundVersion = fundVersionRepository
                    .findByFundIdAndLockChangeIsNull(arrNode.getFund().getFundId());
            final ArrLevel parentLevel = arrangementService.lockNode(arrNode, fundVersion, change);

            eventNotificationService.publishEvent(EventFactory.createAddNodeEvent(EventType.ADD_LEVEL_UNDER, fundVersion,
                    parentLevel, arrLevel));

            for (DaLevelView child : levelView.getChildren()) {
                createNextLevel(arrNode, change, 1, child);
            }
            List<DaDao> daoList = daoRepository.findAllByLevelViewInAndDeleteChangeIsNull(Collections.singletonList(levelView));
            for (DaDao daDao : daoList) {
                ArrDaoLink arrDaoLink = new ArrDaoLink();
                arrDaoLink.setAip(daAip);
                arrDaoLink.setNode(arrNode);
                arrDaoLink.setDaDao(daDao);
                arrDaoLink.setLinkType(ArrDaoLink.LinkType.PART_AIP);
                arrDaoLink.setCreateChange(change);

                daoLinkRepository.save(arrDaoLink);
            }
        }
    }

    @Transactional
    public void bulkConnectLogicalStructureToJP(Integer nodeId, List<Integer> daAipIdList, Integer daLevelViewId) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaLevelView levelView = daLevelViewRepository.findById(daLevelViewId).orElse(null);
        if (levelView == null) {
            logger.error("Nebylo nalezeno level view s předaným ID. ID={}", daLevelViewId);
            throw new ObjectNotFoundException("Nebylo nalezeno level view s předaným ID. ID=" + daLevelViewId, BaseCode.ID_NOT_EXIST);
        }
        for (Integer daAipId : daAipIdList) {
            DaAip daAip = findAipById(daAipId);
            ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
            Integer maxPosition = levelRepository.findMaxPositionUnderParent(arrNode);
            if (maxPosition == null) {
                maxPosition = 0;
            }
            int position = maxPosition + 1;
            for (DaLevelView child : levelView.getChildren()) {
                createNextLevel(arrNode, change, position, child);
            }
            List<DaDao> daoList = daoRepository.findAllByLevelViewInAndDeleteChangeIsNull(Collections.singletonList(levelView));
            for (DaDao daDao : daoList) {
                ArrDaoLink arrDaoLink = new ArrDaoLink();
                arrDaoLink.setAip(daAip);
                arrDaoLink.setNode(arrNode);
                arrDaoLink.setDaDao(daDao);
                arrDaoLink.setLinkType(ArrDaoLink.LinkType.PART_AIP);
                arrDaoLink.setCreateChange(change);

                daoLinkRepository.save(arrDaoLink);
            }
        }
    }

    private void createNextLevel(ArrNode arrNode, ArrChange change, int position, DaLevelView levelView) {
        ArrNode newNode = arrangementService.createNode(arrNode.getFund(), generateUuid(), change);

        ArrLevel arrLevel = new ArrLevel();
        arrLevel.setNodeParent(arrNode);
        arrLevel.setNode(newNode);
        arrLevel.setCreateChange(change);
        arrLevel.setPosition(position);
        levelRepository.save(arrLevel);
        ArrFundVersion fundVersion = fundVersionRepository
                .findByFundIdAndLockChangeIsNull(arrNode.getFund().getFundId());
        final ArrLevel parentLevel = arrangementService.lockNode(arrNode, fundVersion, change);

        eventNotificationService.publishEvent(EventFactory.createAddNodeEvent(EventType.ADD_LEVEL_UNDER, fundVersion,
                parentLevel, arrLevel));
        for (DaLevelView child : levelView.getChildren()) {
            createNextLevel(newNode, change, 1, child);
        }
    }

    @Transactional
    public void bulkCreateFromSelected(Integer nodeId, List<Integer> daAipIdList) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        for (Integer daAipId : daAipIdList) {
            DaAip daAip = findAipById(daAipId);
            ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
            ArrNode newNode = arrangementService.createNode(arrNode.getFund(), generateUuid(), change);
            ArrLevel arrLevel = new ArrLevel();
            arrLevel.setNodeParent(arrNode);
            arrLevel.setNode(newNode);
            arrLevel.setCreateChange(change);
            Integer maxPosition = levelRepository.findMaxPositionUnderParent(arrNode);
            if (maxPosition == null) {
                maxPosition = 0;
            }
            arrLevel.setPosition(maxPosition + 1);
            ArrLevel newLevel = levelRepository.save(arrLevel);
            ArrFundVersion fundVersion = fundVersionRepository
                    .findByFundIdAndLockChangeIsNull(arrNode.getFund().getFundId());
            final ArrLevel parentLevel = arrangementService.lockNode(arrNode, fundVersion, change);

            eventNotificationService.publishEvent(EventFactory.createAddNodeEvent(EventType.ADD_LEVEL_UNDER, fundVersion, parentLevel, newLevel));
            connectToJP(newNode.getNodeId(), daAip.getAipId());
        }
    }

    /**
     * Vytvoření jednoznačného identifikátoru požadavku.
     *
     * @return jednoznačný identifikátor
     */
    public String generateUuid() {
        return UUID.randomUUID().toString();
    }

    @Transactional
    public void deleteDaoLink(Integer daoLinkId) {
        ArrDaoLink arrDaoLink = daoLinkRepository.getOneCheckExist(daoLinkId);

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.DELETE_DAO_LINK, arrDaoLink.getNode());
        arrDaoLink.setDeleteChange(change);
        daoLinkRepository.save(arrDaoLink);
    }


    @Transactional
    public ResponseEntity<Resource> getComponent(Integer fileId) {
        DaDaoFile daoFile = daoFileRepository.findById(fileId).orElseThrow(() -> new IllegalStateException("Nenalezen soubor s id " + fileId));
        DaAip aip = daoFile.getDao().getAip();
        DaLocalCache localCache = daLocalCacheRepository.findByAipAndQueueItemStatesIn(aip, getQueueImportStates());

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

            String filePath = daoFile.getFileName().replace("/", File.separator);
            String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);

            Path file;
            try (Stream<Path> str = Files.walk(tempDir).filter(path -> path.toString().endsWith(filePath))) {
                file = str.findFirst().orElseThrow(() -> new RuntimeException("Balíček neobsahuje soubor " + filePath));
            }

            FileSystemResource fsr = new FileSystemResource(file);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());
            headers.add(HttpHeaders.CONTENT_TYPE, daoFile.getMimeType());
            headers.add(HttpHeaders.CONTENT_LENGTH, daoFile.getSize().toString());
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

            return new ResponseEntity<>(fsr, headers, HttpStatus.OK);
        } catch (IOException e) {
            throw new IllegalStateException("Došlo k chybě při čtení souboru z cache", e);
        }
    }

    public DaoLinksResult getDaoLinks(Integer nodeId) {
        List<DaoLink> daoLinkList = new ArrayList<>();
        List<ArrDaoLink> arrDaoLinks = daoLinkRepository.findByNodeIdAndDeleteChangeIsNullFetchAip(nodeId);

        List<ArrDaoLink> aipDaoLinks = arrDaoLinks.stream().filter(d -> d.getLinkType() == ArrDaoLink.LinkType.AIP).toList();

        if (CollectionUtils.isNotEmpty(aipDaoLinks)) {
            arrDaoLinks.removeAll(aipDaoLinks);
            for (ArrDaoLink aipDaoLink : aipDaoLinks) {
                List<ArrDaoLink> daoLinks = arrDaoLinks.stream().filter(d -> d.getAip().getAipId().equals(aipDaoLink.getAip().getAipId())).toList();
                arrDaoLinks.removeAll(daoLinks);
                daoLinkList.add(createAipDaoLink(aipDaoLink, daoLinks));
            }
        }

        if (CollectionUtils.isNotEmpty(arrDaoLinks)) {
            for (ArrDaoLink daoLink : arrDaoLinks) {
                daoLinkList.add(createDaoLink(daoLink));
            }
        }

        DaoLinksResult daoLinksResult = new DaoLinksResult();
        daoLinksResult.setItems(daoLinkList);
        return daoLinksResult;
    }

    private DaoLink createAipDaoLink(ArrDaoLink aipDaoLink, List<ArrDaoLink> arrDaoLinks) {
        DaoLink daoLink = createDaoLink(aipDaoLink);

        if (CollectionUtils.isNotEmpty(arrDaoLinks)) {
            List<DaoLink> daoLinkList = new ArrayList<>();
            for (ArrDaoLink arrDaoLink : arrDaoLinks) {
                daoLinkList.add(createDaoLink(arrDaoLink));
            }
            daoLink.setChildren(daoLinkList);
        }

        return daoLink;
    }

    private DaoLink createDaoLink(ArrDaoLink arrDaoLink) {
        DaDao dao = arrDaoLink.getDaDao();

        DaoLink daoLink = new DaoLink();
        daoLink.setDaoLinkId(arrDaoLink.getDaoLinkId());
        daoLink.setAipId(arrDaoLink.getAip().getAipId());

        if (dao != null) {
            daoLink.setDaoId(dao.getDaoId());
            daoLink.setDaoCode(dao.getCode());
            daoLink.setDaoType(DaDaoType.fromValue(dao.getType().name()));
            daoLink.setName(dao.getLabel());
        } else {
            daoLink.setName(arrDaoLink.getAip().getAipId().toString());
        }

        return daoLink;
    }
}
