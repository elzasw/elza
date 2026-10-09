package cz.tacr.elza.service.da;

import com.lightcomp.ft.client.Transfer;
import com.lightcomp.ft.xsd.v1.GenericDataType;
import com.lightcomp.kads.common.AnyUriAdapter;
import com.lightcomp.kads.mets.MetsReaderWriter;
import com.lightcomp.kads.premis.PremisReaderWriter;
import cz.tacr.da.ApiException;
import cz.tacr.da.controller.vo.DownloadDownloadAips;
import cz.tacr.da.controller.vo.DownloadDownloadStatus;
import cz.tacr.da.controller.vo.IngestIngestResult;
import cz.tacr.da.controller.vo.IngestIngestStatus;
import cz.tacr.da.controller.vo.RequestState;
import cz.tacr.da.controller.vo.UpdatedAips;
import cz.tacr.da.controller.vo.UpdatedInfo;
import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.api.DaAipActionType;
import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.connector.DaConnector;
import cz.tacr.elza.controller.vo.AipUpdateType;
import cz.tacr.elza.controller.vo.DaDaoType;
import cz.tacr.elza.controller.vo.DaoLink;
import cz.tacr.elza.controller.vo.DaoLinksResult;
import cz.tacr.elza.controller.vo.UserInfoVO;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.security.Authorization;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaAipActionItem;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoFile;
import cz.tacr.elza.domain.DaDaoFileFolder;
import cz.tacr.elza.domain.DaDaoRelation;
import cz.tacr.elza.domain.DaLevelView;
import cz.tacr.elza.domain.DaLocalCache;
import cz.tacr.elza.domain.DaRemoteRepositorySync;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaDaoFileFolderRepository;
import cz.tacr.elza.repository.DaDaoFileRepository;
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DaLevelViewRepository;
import cz.tacr.elza.repository.DaAipActionItemRepository;
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
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DaoLevelViewService;
import cz.tacr.elza.service.ExternalSystemService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.DaoLinkPolicy;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.da.vo.DaUploadRequestImpl;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdNodeIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.utils.EadReaderWriter;
import gov.loc.mets.v1_11.schema.AmdSecType;
import gov.loc.mets.v1_11.schema.DivType;
import gov.loc.mets.v1_11.schema.MdSecType;
import gov.loc.mets.v1_11.schema.Mets;
import gov.loc.mets.v1_11.schema.MetsType;
import gov.loc.mets.v1_11.schema.StructMapType;
import gov.loc.premis.v3.AgentComplexType;
import gov.loc.premis.v3.AgentIdentifierComplexType;
import gov.loc.premis.v3.EventComplexType;
import gov.loc.premis.v3.EventIdentifierComplexType;
import gov.loc.premis.v3.IntellectualEntity;
import gov.loc.premis.v3.LinkingAgentIdentifierComplexType;
import gov.loc.premis.v3.LinkingObjectIdentifierComplexType;
import gov.loc.premis.v3.ObjectComplexType;
import gov.loc.premis.v3.ObjectIdentifierComplexType;
import gov.loc.premis.v3.PremisComplexType;
import gov.loc.premis.v3.SignificantPropertiesComplexType;
import gov.loc.premis.v3.StringPlusAuthority;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.archivists.ead3.schema.Ead;
import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.function.Supplier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static cz.tacr.elza.exception.codes.ArrangementCode.AIP_NOT_FOUND;
import cz.tacr.elza.common.io.SpooledContent;
import org.apache.commons.io.file.PathUtils;
import java.util.zip.ZipFile;
import org.springframework.core.io.InputStreamResource;
import java.nio.file.StandardCopyOption;
import cz.tacr.elza.api.DaOnReceivedAction;
import cz.tacr.elza.controller.vo.AipPackageEntry;
import cz.tacr.elza.exception.SystemException;

@Service
public class DaService {

    private static final Logger logger = LoggerFactory.getLogger(DaService.class);

    private static final Integer DA_UPDATE_PAGE_SIZE = 1000;

    /**
     * The column holding the description is unbounded; the cap only keeps a pathological
     * exception message from bloating the row.
     */
    private static final int STATE_MESSAGE_MAX_LENGTH = 4000;

    /** Why a request for an AIP the digital archive invalidated is not carried out. */
    static final String AIP_INVALIDATED = "AIP byl v digitálním archivu zneplatněn.";

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
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private NodeRepository nodeRepository;
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
    private NodeCacheService nodeCacheService;
    @Autowired
    private FundRepository fundRepository;
    @Autowired
    private DataStringRepository dataStringRepository;
    @Autowired
    private DataUnitdateRepository dataUnitdateRepository;
    @Autowired
    private DaAipReferenceResolver referenceResolver;
    @Autowired
    private DaAipActionService actionService;
    @Autowired
    private DaoLinkPolicy daoLinkPolicy;
    @Autowired
    private DaAipLinkStateResolver linkStateResolver;

    @Autowired
    private DaLinkCloser linkCloser;
    @Autowired
    private DaCommunicationLock communicationLock;
    @Autowired
    private DaAipActionItemRepository aipActionItemRepository;

    /** Reads and writes what an action was asked to do; see {@link ConnectParams}. */
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private AsyncRequestService asyncRequestService;
    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager txManager;

    public void synchronizeDaRepository(String code) {
        logger.debug("Spuštěna synchronizace s DA pro externí systém CODE={}", code);
        ArrDigitalRepository arrDigitalRepository = externalSystemService.findDigitalRepositoryByCode(code);
        // held over the commit, so the processors see the withdrawn requests as withdrawn
        communicationLock.lock();
        try {
            applicationContext.getBean(DaService.class).synchronizeDA(arrDigitalRepository);
        } finally {
            communicationLock.unlock();
        }
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
                processUpdates(digitalRepository, updatesAips.getAipIds());
            }
        } while (updatesAips.getAipIds().size() == DA_UPDATE_PAGE_SIZE);

        daRemoteRepositorySync.setNextQuery(nextQuery);
        remoteRepositorySyncRepository.save(daRemoteRepositorySync);
    }

    /**
     * Handles one page of changes reported by the digital archive.
     *
     * A valid AIP is queued for download. An AIP without an active state - unknown to ELZA, or
     * invalidated earlier - is imported as a new package; links it had before its invalidation are
     * not restored, the current rules of the repository decide what happens to it. An invalidated
     * AIP is withdrawn, see {@link #invalidateAip}.
     */
    void processUpdates(ArrDigitalRepository digitalRepository, List<UpdatedInfo> updates) {
        List<String> aipCodes = updates.stream()
                .map(UpdatedInfo::getAipId)
                .toList();

        List<DaAip> aipList = aipRepository.findByCodeIn(aipCodes);
        Map<String, DaAip> aipMap = aipList.stream()
                .collect(Collectors.toMap(DaAip::getCode, a -> a));
        Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

        for (UpdatedInfo updatedInfo : updates) {
            DaAip aip = aipMap.get(updatedInfo.getAipId());
            DaAipState aipState = aip == null ? null : stateMap.get(aip);

            if (BooleanUtils.isTrue(updatedInfo.getInvalidated())) {
                invalidateAip(updatedInfo.getAipId(), aip, aipState, digitalRepository);
                continue;
            }

            DaSyncQueueItem.QueueItemState queueItemState = DaSyncQueueItem.QueueItemState.IMPORT_NEW;
            AipType aipType = AipType.PACKAGE_INFO;
            if (aipState != null) {
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

    /**
     * Withdraws an AIP the digital archive has invalidated, so that it disappears from ELZA while its
     * history stays readable: its links to the archival description, its digital entities and its
     * state are closed by a change instead of being deleted. The downloaded packages are removed -
     * they are of a package that no longer exists.
     *
     * Requests still waiting for the AIP are withdrawn as well, whether ELZA knows the AIP or not: a
     * download would fetch the package again, or retry forever once the archive stops delivering it,
     * and an export would send a change of a package that no longer exists. A download the DA is
     * preparing is withdrawn too - its batch is finished without it. An export the DA received is
     * not: its result is still awaited and recorded.
     *
     * @param aip      null when ELZA does not know the AIP
     * @param aipState null when ELZA does not know the AIP or it is invalidated already
     */
    private void invalidateAip(String code, @Nullable DaAip aip, @Nullable DaAipState aipState,
                               ArrDigitalRepository digitalRepository) {
        deactivateQueueItems(code, aip, digitalRepository, getQueueWithdrawableStates(), AIP_INVALIDATED);
        if (aipState == null) {
            logger.info("AIP {} byl v DA CODE={} zneplatněn, v ELZA není aktivní, není co stahovat",
                        code, digitalRepository.getCode());
            return;
        }

        DaChange change = createDaChange(aip, DaChangeType.AIP_INVALIDATE);
        int unlinked = linkCloser.close(daLinkRepository.findByAipIdAndDeleteChangeIsNull(aip.getAipId()));

        List<DaAip> aips = List.of(aip);
        deleteDaoEntities(aips, change);
        deleteLocalCaches(aips, getQueueAllStates());

        aipState.setDeleteChange(change);
        aipStateRepository.save(aipState);
        logger.info("AIP {} byl v DA CODE={} zneplatněn, počet odpojených vazeb na jednotky popisu: {}",
                    code, digitalRepository.getCode(), unlinked);
    }

    private DaRemoteRepositorySync getDaRemoteRepositorySync(ArrDigitalRepository digitalRepository) {
        DaRemoteRepositorySync daRemoteRepositorySync = remoteRepositorySyncRepository.findByDigitalRepository(digitalRepository);
        if (daRemoteRepositorySync == null) {
            daRemoteRepositorySync = new DaRemoteRepositorySync();
            daRemoteRepositorySync.setDigitalRepository(digitalRepository);
        }
        return daRemoteRepositorySync;
    }

    /**
     * Akce nad AIPy podle jejího ID.
     */
    @Transactional
    public DaAipAction getAipAction(Integer actionId) {
        return actionService.getAction(actionId);
    }

    /**
     * Runs one step of an action in a transaction of its own, so that a step which fails costs
     * only its own work and the steps before it stay committed.
     */
    private <T> T inTransaction(Supplier<T> step) {
        return new TransactionTemplate(txManager).execute(status -> step.get());
    }

    /**
     * Builds the DAO structure of the given AIPs from their cached metadata packages.
     *
     * Each AIP is a step of its own: what is read, what is unpacked and what is written happen in
     * separate transactions, and one AIP is committed before the next is started. A failure
     * therefore costs only the AIP it happened on, and the outcomes recorded so far are readable
     * while the rest is still running. Unpacking the package is deliberately left outside a
     * transaction - it is file work and can take a while on a large package.
     *
     * @param sink records what the rebuild did to each AIP; {@link AipOutcomeSink#NONE} when
     *             nothing asked for it
     * @return UUIDs offered for node matching ({@link AipNodeUuids}) per successfully
     *         processed AIP; AIPs without fund, without cached metadata or failing to process
     *         are absent
     */
    public Map<Integer, List<String>> doCreateDaoStructure(List<Integer> aipIds, AipOutcomeSink sink) {
        Map<Integer, List<String>> uuidsByAip = new LinkedHashMap<>();
        for (Integer aipId : aipIds) {
            List<String> nodeUuids = rebuildOneAip(aipId, sink);
            if (nodeUuids != null) {
                uuidsByAip.put(aipId, nodeUuids);
            }
        }
        return uuidsByAip;
    }

    /** What the rebuild of one AIP needs, read in one transaction of its own. */
    private record RebuildInput(DaAip aip, Integer aipStateId, Integer localCacheId, Path zip) {
    }

    /**
     * @return the UUIDs offered for node matching, or null when the AIP was skipped or failed
     */
    @Nullable
    private List<String> rebuildOneAip(Integer aipId, AipOutcomeSink sink) {
        RebuildInput input = inTransaction(() -> readRebuildInput(aipId, sink));
        if (input == null) {
            return null;
        }

        Path tempDir = null;
        AipPackageType packageType = null;
        try {
            tempDir = unpack(input.zip());
            MetsType metsType = readMets(tempDir);
            packageType = AipPackageType.of(metsType);
            PremisComplexType premisComplexType = readPremis(tempDir, metsType);

            Path unpacked = tempDir;
            AipPackageType type = packageType;
            return inTransaction(() -> storeDaoStructure(input, metsType, type, premisComplexType, unpacked, sink));
        } catch (Exception e) {
            AipProblem problem = AipProblem.of(e);
            logger.error("Došlo k chybě při zpracování metadat pro AIP={} ({}), balíček {}{}: {}", aipId,
                    input.aip().getCode(), input.zip(),
                    problem.file() != null ? ", soubor " + problem.file() : "",
                    problem.description(), e);
            // The transaction the rebuild ran in is gone; the problem has to be written in one of
            // its own or it would be rolled back together with the work that failed. What kind of
            // package it is stays known when its METS could be read - it helps to tell which
            // packages fail.
            AipPackageType typeRead = packageType;
            inTransaction(() -> {
                DaAipState aipState = aipStateRepository.findById(input.aipStateId()).orElse(null);
                if (aipState != null) {
                    if (typeRead != null) {
                        typeRead.applyTo(aipState);
                    }
                    referenceResolver.recordProblem(aipState, problem);
                    aipStateRepository.save(aipState);
                }
                sink.failed(aipId, problem.description());
                return null;
            });
            return null;
        } finally {
            deleteTempDirectory(tempDir);
        }
    }

    /** Null when the AIP cannot be rebuilt; the reason is recorded on the sink. */
    @Nullable
    private RebuildInput readRebuildInput(Integer aipId, AipOutcomeSink sink) {
        DaAip aip = findAipById(aipId);
        DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
        if (aipState == null) {
            sink.skipped(aipId, AIP_INVALIDATED);
            return null;
        }
        DaLocalCache localCache = daLocalCacheRepository.findByAipStateAndAipTypeIn(aipState,
                EnumSet.of(AipType.METADATA_BASE, AipType.AIP_BASE),
                getQueueImportStates());

        if (aipState.getFund() == null) {
            logger.info("AIP={} není navázaný na fund", aipId);
            sink.skipped(aipId, "AIP není navázaný na archivní soubor, není kam digitální entity vytvořit.");
            return null;
        }
        if (localCache == null) {
            logger.info("Nebyla nalezena lokální cache s metadaty pro AIP={}", aipId);
            sink.skipped(aipId, "V ELZA není uložený balíček s metadaty, ze kterého by šlo entity sestavit.");
            return null;
        }
        return new RebuildInput(aip, aipState.getAipStateId(), localCache.getLocalCacheId(),
                                Paths.get(localCache.getFilePath()));
    }

    private List<String> storeDaoStructure(RebuildInput input, MetsType metsType, AipPackageType packageType,
                                           PremisComplexType premisComplexType, Path tempDir, AipOutcomeSink sink) {
        // The package was read in an earlier transaction; the AIP may have been invalidated since.
        // The invalidation holds the lock until it commits, so it is either seen here or waits for
        // the entities built below and closes them too.
        aipRepository.lockByIds(List.of(input.aip().getAipId()));
        if (aipStateRepository.findByDaAipAndDeleteChangeIsNull(input.aip()) == null) {
            sink.skipped(input.aip().getAipId(), AIP_INVALIDATED);
            return null;
        }
        List<String> nodeUuids = createDaoStructure(input.aip(), metsType, premisComplexType, tempDir);

        DaLocalCache localCache = daLocalCacheRepository.findById(input.localCacheId()).orElseThrow();
        if (localCache.getFilePathMetadata() != null && !localCache.getFilePath().equals(localCache.getFilePathMetadata())) {
            Path oldFile = Paths.get(localCache.getFilePathMetadata());
            oldFile.toFile().delete();
        }
        localCache.setFilePathMetadata(localCache.getFilePath());
        daLocalCacheRepository.save(localCache);

        DaAipState aipState = aipStateRepository.findById(input.aipStateId()).orElseThrow();
        aipState.setAipVersionMetadata(aipState.getAipVersion());
        packageType.applyTo(aipState);
        referenceResolver.clearProblem(aipState);
        // Rebuilding the package can add files to it, so how much of it is attached changes even
        // though no link was touched.
        linkStateResolver.updateLinkState(aipState);
        aipStateRepository.save(aipState);
        sink.finished(input.aip().getAipId());
        return nodeUuids;
    }

    Path unpack(Path zip) throws IOException {
        Path tempDir = Files.createTempDirectory("unzipped");
        try (ZipInputStream zipInputStream = new ZipInputStream((Files.newInputStream(zip)))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path filePath = AipPackageFiles.resolveInside(tempDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zipInputStream, filePath);
                }
            }
        }
        return tempDir;
    }

    /**
     * @param unpacked the directory a stored package was unpacked to
     * @return the root METS of the package
     */
    MetsType readMets(Path unpacked) throws Exception {
        return MetsReaderWriter.unmarshal(AipPackageFiles.mets(AipPackageFiles.packageRoot(unpacked)));
    }

    /**
     * Reads every PREMIS file the root METS refers to, under whatever name, into one.
     *
     * Only the original names of the files are taken from PREMIS, and a file without one is
     * named by its path, so a package without PREMIS is processed too; a PREMIS file the METS
     * refers to but the package does not carry is an error of the package.
     */
    private PremisComplexType readPremis(Path unpacked, MetsType mets) throws Exception {
        Path root = AipPackageFiles.packageRoot(unpacked);
        PremisComplexType premis = new PremisComplexType();
        for (String href : AipPackageFiles.premisHrefs(mets)) {
            premis.getObject().addAll(PremisReaderWriter.unmarshal(AipPackageFiles.referenced(root, href)).getObject());
        }
        return premis;
    }

    /**
     * @param unpacked the directory a stored package was unpacked to
     * @param href     the path of the EAD as the METS gives it
     */
    public Ead loadEadFile(Path unpacked, String href) throws IOException, JAXBException {
        return EadReaderWriter.unmarshal(AipPackageFiles.referenced(AipPackageFiles.packageRoot(unpacked), href));
    }

    public DaAip findAipById(Integer aipId) {
        return aipRepository.findById(aipId).orElseThrow(() -> new ObjectNotFoundException("Nebyl nalezen AIP=" + aipId, AIP_NOT_FOUND));
    }

    public DaDao findDaoById(Integer daoId) {
        return daoRepository.findById(daoId).orElseThrow(() -> new ObjectNotFoundException("Nebylo nalezeno DAO=" + daoId, AIP_NOT_FOUND));
    }

    /**
     * @return UUIDs the AIP offers for node matching, see {@link DaoProcessor#getNodeUuids()}
     */
    @Transactional
    public List<String> createDaoStructure(DaAip aip, MetsType metsType, PremisComplexType premisComplexType, Path tempDir) {
        DaoProcessor daoProcessor = applicationContext.getBean(DaoProcessor.class, aip, metsType, premisComplexType, tempDir);
        daoProcessor.process();
        return daoProcessor.getNodeUuids();
    }

    /**
     * Requests the metadata of the given AIPs, as an action of the current user.
     */
    @Transactional
    public DaAipAction requestMetadata(List<Integer> aipIds) {
        DaAipAction action = actionService.start(DaAipActionType.LOAD_METADATA, aipRepository.findAllById(aipIds));
        createDaoStructure(aipIds, actionService.sinkFor(action));
        return action;
    }

    /**
     * Whether a download that would deliver at least the requested form of the package is already
     * waiting in the queue. The load flags say what is on disk and the queue says what was asked
     * for; asking again would only replace the pending item with an identical one.
     */
    private boolean isDownloadPending(DaAip aip, AipType requested) {
        DaSyncQueueItem pending = syncQueueItemRepository.findFirstByAipAndStateInAndActiveIsTrueOrderBySyncQueueItemIdDesc(aip,
                List.of(DaSyncQueueItem.QueueItemState.IMPORT_NEW, DaSyncQueueItem.QueueItemState.UPDATE,
                        DaSyncQueueItem.QueueItemState.DOWNLOAD_REQUESTED));
        return pending != null && pending.getAipType() != null && rank(pending.getAipType()) >= rank(requested);
    }

    /**
     * Order of the package forms by content: each form contains everything the forms below it do
     * (see the DA API for dipType). The native form is the whole package, so it ranks with the
     * complete one.
     */
    private static int rank(AipType type) {
        return switch (type) {
            case PACKAGE_INFO -> 0;
            case ARCHDESC -> 1;
            case METADATA_BASE -> 2;
            case AIP_BASE, AIP_RAW -> 3;
        };
    }

    @Transactional
    public void createDaoStructure(List<Integer> aipIds) {
        createDaoStructure(aipIds, AipOutcomeSink.NONE);
    }

    @Transactional
    public void createDaoStructure(List<Integer> aipIds, AipOutcomeSink sink) {
        List<DaAip> aipList = aipRepository.findAllById(aipIds);
        Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

        List<DaAipState> resolvedStates = new ArrayList<>();

        for (DaAip aip : aipList) {
            DaAipState aipState = stateMap.get(aip);
            if (aipState == null) {
                sink.skipped(aip.getAipId(), AIP_INVALIDATED);
                continue;
            }
            if (aipState.getFund() == null) {
                referenceResolver.resolveReferences(aipState);
                resolvedStates.add(aipState);
            }
            if (aipState.getFund() == null) {
                sink.skipped(aip.getAipId(), "AIP není navázaný na archivní soubor, není ke kterému fondu metadata stahovat.");
            } else if (BooleanUtils.isTrue(aipState.getMetadataLoad()) || BooleanUtils.isTrue(aipState.getCompleteAipLoad())) {
                sink.skipped(aip.getAipId(), "Metadata AIPu už jsou stažená.");
            } else if (isDownloadPending(aip, AipType.METADATA_BASE)) {
                sink.skipped(aip.getAipId(), "Stažení metadat AIPu už je ve frontě.");
            } else {
                DaSyncQueueItem queueItem = createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(),
                        DaSyncQueueItem.QueueItemState.UPDATE, aipState.getAipVersion(), AipType.METADATA_BASE, true);
                sink.enqueued(aip.getAipId(), queueItem);
            }
        }

        aipStateRepository.saveAll(resolvedStates);
    }

    /**
     * Drops the digital entities built from the metadata, as an action of the current user.
     */
    @Transactional
    public DaAipAction deleteMetadata(List<Integer> aipIds) {
        DaAipAction action = actionService.start(DaAipActionType.DELETE_METADATA, aipRepository.findAllById(aipIds));
        deleteDaoStructure(aipIds, actionService.sinkFor(action));
        return action;
    }

    @Transactional
    public void deleteDaoStructure(List<Integer> aipIds) {
        deleteDaoStructure(aipIds, AipOutcomeSink.NONE);
    }

    @Transactional
    public void deleteDaoStructure(List<Integer> aipIds, AipOutcomeSink sink) {
        List<DaAip> aipList = aipRepository.findByIdAndLinkNotExists(aipIds);
        // An AIP attached to a unit of description is filtered out by the query above; without
        // saying so the action would report success and leave it untouched.
        Set<Integer> selectable = aipList.stream().map(DaAip::getAipId).collect(Collectors.toSet());
        aipIds.stream().filter(id -> !selectable.contains(id)).forEach(id ->
                sink.skipped(id, "AIP je napojený na jednotku popisu, jeho digitální entity nelze smazat."));

        Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

        List<DaAip> deletedAipList = new ArrayList<>();

        for (DaAip aip : aipList) {
            DaAipState aipState = stateMap.get(aip);
            if (aipState == null) {
                sink.skipped(aip.getAipId(), AIP_INVALIDATED);
            } else if (BooleanUtils.isTrue(aipState.getMetadataLoad()) && BooleanUtils.isNotTrue(aipState.getCompleteAipLoad())) {
                createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(), DaSyncQueueItem.QueueItemState.IMPORT_OK,
                        aipState.getAipVersion(), AipType.PACKAGE_INFO, true);
                deletedAipList.add(aip);
                sink.finished(aip.getAipId());
            } else {
                sink.skipped(aip.getAipId(), BooleanUtils.isTrue(aipState.getCompleteAipLoad())
                        ? "AIP má stažený kompletní balíček; nejprve je nutné smazat ten."
                        : "AIP nemá stažená metadata, není co mazat.");
            }
        }

        List<DaAipState> stateList = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(deletedAipList);
        DaChange change = createDaChange(null, DaChangeType.AIP_UPDATE);

        stateList.forEach(this::deleteStateMetadata);
        aipStateRepository.saveAll(stateList);

        deleteDaoEntities(deletedAipList, change);
        deleteLocalCaches(deletedAipList, getQueueImportStates());
    }

    /**
     * Closes the digital entities of the AIPs - DAOs, their relations, file folders and files - by
     * the given change, and drops the level views no longer used by any of them.
     */
    private void deleteDaoEntities(List<DaAip> aips, DaChange change) {
        List<DaDao> daDaoList = daoRepository.findByAipInAndDeleteChangeIsNull(aips);
        List<DaDaoRelation> daDaoRelationList = daoRelationRepository.findByDaoInAndDeleteChangeIsNull(daDaoList);
        List<DaDaoFileFolder> daDaoFileFolderList = daoFileFolderRepository.findByRepresentationDaoInAndDeleteChangeIsNull(daDaoList);
        List<DaDaoFile> daDaoFileList = daoFileRepository.findByDaoInAndDeleteChangeIsNull(daDaoList);

        daDaoList.forEach(d -> d.setDeleteChange(change));
        daDaoRelationList.forEach(r -> r.setDeleteChange(change));
        daDaoFileFolderList.forEach(f -> f.setDeleteChange(change));
        daDaoFileList.forEach(f -> f.setDeleteChange(change));

        daoRepository.saveAll(daDaoList);
        daoRelationRepository.saveAll(daDaoRelationList);
        daoFileFolderRepository.saveAll(daDaoFileFolderList);
        daoFileRepository.saveAll(daDaoFileList);

        levelViewService.deleteDisconnectedLevelViews(change);
    }

    /**
     * Deletes the packages of the AIPs stored for queue items in the given states - downloaded ones
     * for the import states, prepared exports for the export states - the files together with their
     * records.
     */
    private void deleteLocalCaches(List<DaAip> aips, Collection<DaSyncQueueItem.QueueItemState> queueItemStates) {
        List<DaLocalCache> localCacheList = daLocalCacheRepository.findByAipInAndQueueItemStatesIn(aips, queueItemStates);
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

    private void deleteStateMetadata(DaAipState s) {
        s.setMetadataLoad(false);
        referenceResolver.clearProblem(s);
    }

    /**
     * Resolves the references of the given AIPs again - used after the missing institution or
     * fund has been created in ELZA, so the AIPs stop being reported as problematic.
     *
     * @return number of AIPs whose reference was newly resolved
     */
    @Transactional
    public int remapReferences(List<Integer> aipIds) {
        return remapReferences(aipIds, AipOutcomeSink.NONE);
    }

    @Transactional
    public int remapReferences(List<Integer> aipIds, AipOutcomeSink sink) {
        List<DaAip> aipList = aipRepository.findAllById(aipIds);
        List<DaAipState> stateList = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList);
        Set<Integer> activeAipIds = stateList.stream().map(s -> s.getDaAip().getAipId()).collect(Collectors.toSet());
        aipList.stream().map(DaAip::getAipId).filter(id -> !activeAipIds.contains(id))
                .forEach(id -> sink.skipped(id, AIP_INVALIDATED));
        List<DaAipState> resolvedStates = new ArrayList<>();
        for (DaAipState aipState : stateList) {
            Integer aipId = aipState.getDaAip().getAipId();
            if (referenceResolver.resolveReferences(aipState)) {
                resolvedStates.add(aipState);
                sink.finished(aipId);
            } else if (aipState.getFund() != null) {
                sink.skipped(aipId, "Instituce i archivní soubor jsou dohledané, není co měnit.");
            } else {
                sink.skipped(aipId, aipState.getProblemDescription() != null
                        ? aipState.getProblemDescription()
                        : "Archivní soubor se podle údajů balíčku nepodařilo dohledat.");
            }
        }
        aipStateRepository.saveAll(stateList);
        logger.info("Reference resolution requested for {} AIP(s), newly resolved: {}",
                stateList.size(), resolvedStates.size());

        // A repository that downloads metadata automatically could not do so while the fund
        // was unknown; once it is resolved the download is requested, as it would have been
        // at import time.
        List<Integer> autoDownloadAipIds = resolvedStates.stream()
                .filter(s -> s.getFund() != null)
                .filter(s -> s.getDaAip().getDigitalRepository().getOnReceived() == DaOnReceivedAction.DOWNLOAD_METADATA)
                .map(s -> s.getDaAip().getAipId())
                .toList();
        if (!autoDownloadAipIds.isEmpty()) {
            logger.info("Requesting metadata of {} newly paired AIP(s): {}",
                    autoDownloadAipIds.size(), autoDownloadAipIds);
            createDaoStructure(autoDownloadAipIds);
        }
        return resolvedStates.size();
    }

    @Transactional
    public DaAipAction aipDownloadCompleteAip(List<Integer> aipIds) {
        DaAipAction action = actionService.start(DaAipActionType.LOAD_COMPLETE_AIP, aipRepository.findAllById(aipIds));
        AipOutcomeSink sink = actionService.sinkFor(action);
        List<DaAip> aipList = aipRepository.findAllById(aipIds);
        Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

        for (DaAip aip : aipList) {
            DaAipState aipState = stateMap.get(aip);
            if (aipState == null) {
                sink.skipped(aip.getAipId(), AIP_INVALIDATED);
            } else if (BooleanUtils.isTrue(aipState.getCompleteAipLoad())) {
                sink.skipped(aip.getAipId(), "Kompletní AIP je už stažený.");
            } else if (BooleanUtils.isNotTrue(aipState.getMetadataLoad())) {
                sink.skipped(aip.getAipId(), "AIP nemá stažená metadata; nejprve je nutné stáhnout ta.");
            } else if (isDownloadPending(aip, AipType.AIP_BASE)) {
                sink.skipped(aip.getAipId(), "Stažení kompletního AIPu už je ve frontě.");
            } else {
                DaSyncQueueItem queueItem = createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(),
                        DaSyncQueueItem.QueueItemState.UPDATE, aipState.getAipVersion(), AipType.AIP_BASE, true);
                sink.enqueued(aip.getAipId(), queueItem);
            }
        }

        return action;
    }

    @Transactional
    public DaAipAction aipDeleteCompleteAip(List<Integer> aipIds) {
        DaAipAction action = actionService.start(DaAipActionType.DELETE_COMPLETE_AIP, aipRepository.findAllById(aipIds));
        AipOutcomeSink sink = actionService.sinkFor(action);
        List<DaAip> aipList = aipRepository.findAllById(aipIds);
        Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

        // The complete package is replaced by the metadata package when that one arrives, and the
        // flags follow the package on disk - so nothing is cleared here, the pending downgrade
        // shows as the queue item of the AIP.
        for (DaAip aip : aipList) {
            DaAipState aipState = stateMap.get(aip);
            if (aipState == null) {
                sink.skipped(aip.getAipId(), AIP_INVALIDATED);
            } else if (BooleanUtils.isNotTrue(aipState.getCompleteAipLoad())) {
                sink.skipped(aip.getAipId(), "AIP nemá stažený kompletní balíček, není co mazat.");
            } else if (isDownloadPending(aip, AipType.METADATA_BASE)) {
                sink.skipped(aip.getAipId(), "Nahrazení kompletního balíčku metadaty už je ve frontě.");
            } else {
                DaSyncQueueItem queueItem = createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(),
                        DaSyncQueueItem.QueueItemState.UPDATE, aipState.getAipVersion(), AipType.METADATA_BASE, true);
                sink.enqueued(aip.getAipId(), queueItem);
            }
        }

        return action;
    }

    /**
     * Not transactional on purpose: the steps of the action each open a transaction of their own,
     * so an AIP that fails does not take the ones already done with it.
     */
    public DaAipAction aipUpdateAip(AipUpdateType type, List<Integer> aipIds) {
        DaAipAction action = actionService.start(actionTypeOf(type), aipRepository.findAllById(aipIds));
        AipOutcomeSink sink = actionService.sinkFor(action);
        if (type == AipUpdateType.DOWNLOAD_UPDATE) {
            // Enqueueing is a few inserts per AIP and cannot fail halfway through the work of
            // one, so the whole request is one transaction rather than one per AIP.
            inTransaction(() -> {
                List<DaAip> aipList = aipRepository.findAllById(aipIds);
                Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                        .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

                for (DaAip aip : aipList) {
                    DaAipState aipState = stateMap.get(aip);
                    if (aipState == null) {
                        sink.skipped(aip.getAipId(), AIP_INVALIDATED);
                        continue;
                    }
                    AipType aipType = AipType.PACKAGE_INFO;
                    if (BooleanUtils.isTrue(aipState.getCompleteAipLoad())) {
                        aipType = AipType.AIP_BASE;
                    } else if (BooleanUtils.isTrue(aipState.getMetadataLoad())) {
                        aipType = AipType.METADATA_BASE;
                    }
                    DaSyncQueueItem queueItem = createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(),
                            DaSyncQueueItem.QueueItemState.UPDATE, aipState.getAipVersion(), aipType, true);
                    sink.enqueued(aip.getAipId(), queueItem);
                }
                return null;
            });
        } else {
            // The work ELZA does on its own is queued, one step per AIP: the request is answered
            // without waiting for it, and each AIP is carried out in a transaction of its own.
            enqueueSteps(action);
        }
        return action;
    }

    /**
     * Queues one step per AIP of the action. The steps are carried out one at a time, in the order
     * they were requested.
     */
    private void enqueueSteps(DaAipAction action) {
        actionService.enqueueSteps(action.getAipActionId());
    }

    private static DaAipActionType actionTypeOf(AipUpdateType type) {
        return switch (type) {
            case DOWNLOAD_UPDATE -> DaAipActionType.DOWNLOAD_UPDATE;
            case DB_UPDATE -> DaAipActionType.DB_UPDATE;
            case FORCE_UPDATE -> DaAipActionType.FORCE_UPDATE;
            case REMAP_REFERENCES -> DaAipActionType.REMAP_REFERENCES;
        };
    }

    @Transactional
    public DaAipAction aipExportAip(List<Integer> aipIds) {
        DaAipAction action = actionService.start(DaAipActionType.EXPORT, aipRepository.findAllById(aipIds));
        AipOutcomeSink sink = actionService.sinkFor(action);
        List<DaAip> aipList = aipRepository.findByIdAndLinkExists(aipIds);
        // Only an AIP attached to a unit of description can be exported; the query filters the
        // rest out, and without saying so the action would report success and send nothing.
        Set<Integer> exportable = aipList.stream().map(DaAip::getAipId).collect(Collectors.toSet());
        // an export submitted twice at once waits here for the first one, then supersedes its items -
        // otherwise both build the packages and both queue them as active (seen after a double click)
        if (!exportable.isEmpty()) {
            aipRepository.lockByIds(exportable);
        }
        aipIds.stream().filter(id -> !exportable.contains(id)).forEach(id ->
                sink.skipped(id, "AIP není napojený na jednotku popisu, není co exportovat."));
        Map<DaAip, DaAipState> stateMap = aipStateRepository.findByDaAipInAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.toMap(DaAipState::getDaAip, Function.identity()));

        for (DaAip aip : aipList) {
            DaAipState aipState = stateMap.get(aip);
            if (aipState == null) {
                sink.skipped(aip.getAipId(), AIP_INVALIDATED);
                continue;
            }

            AipType aipType = AipType.PACKAGE_INFO;
            if (BooleanUtils.isTrue(aipState.getCompleteAipLoad())) {
                aipType = AipType.AIP_BASE;
            } else if (BooleanUtils.isTrue(aipState.getMetadataLoad())) {
                aipType = AipType.METADATA_BASE;
            }

            try {
                Path exportDir = Files.createTempDirectory("export");
                Path aipDir = exportDir.resolve(aip.getCode());
                Files.createDirectories(aipDir);

                XMLGregorianCalendar createDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
                Path packageInfoPath = createPackageInfo(aip, aipDir, createDate);
                Path eadPath = createEad(aip, aipDir);

                createMets(aipDir, packageInfoPath, eadPath, createDate);


                Path aipOutputDir = resourcePathResolver.getAipDir().resolve("out");
                Path outputZip = createZip(aipDir.toFile(), aipOutputDir);

                // Odstranit dočasné soubory a adresáře
                try (Stream<Path> str = Files.walk(exportDir)) {
                    str.map(Path::toFile).forEach(File::delete);
                }

                DaSyncQueueItem syncQueueItem = createSyncQueueItem(aip.getCode(), aip, aip.getDigitalRepository(), DaSyncQueueItem.QueueItemState.EXPORT_NEW,
                        aipState.getAipVersion(), aipType, true);
                createExportLocalCache(aipState, aipType, outputZip, syncQueueItem);
                sink.enqueued(aip.getAipId(), syncQueueItem);
            } catch (IOException | JAXBException | DatatypeConfigurationException e) {
                logger.error("Došlo k chybě při vytváření změnového balíčku AIP={}", aip.getCode(), e);
                sink.failed(aip.getAipId(), "Změnový balíček se nepodařilo vytvořit: " + AipProblem.reason(e));
            }
        }
        return action;
    }

    private Path createEad(DaAip aip, Path aipDir) throws IOException {
//        List<DaDao> daDaoList = daoRepository.findByAipAndDeleteChangeIsNull(aip); //TODO zatím zakomentováno, nutno ujasnit, jak má vypadat výstup. Použit defaultní soubor
//        List<DaDaoItem> daDaoItemList = daDaoItemRepository.findByDaoInAndDeleteChangeIsNull(daDaoList);
//        Did did = new Did();
//        for (DaDaoItem daDaoItem : daDaoItemList) {
//            ArrData data = daDaoItem.getData();
//            DataType type = data.getType();
//
//
//            switch (type) {
//                case STRING -> {
//                    ArrDataString arrDataString = dataStringRepository.findById(data.getDataId()).orElse(null);
//                    if (arrDataString != null) {
//                        Abstract abs = new Abstract();
//                        abs.getContent().add(arrDataString.getStringValue());
//                        did.getMDid().add(abs);
//                    }
//                }
//                case UNITDATE -> {
//                    ArrDataUnitdate arrDataUnitdate = dataUnitdateRepository.findById(data.getDataId()).orElse(null);
//                    if (arrDataUnitdate != null) {
//                        Unitdatestructured unitdatestructured = new Unitdatestructured();
//                        Daterange daterange = new Daterange();
//                        Fromdate fromDate = new Fromdate();
//                        fromDate.setStandarddate(arrDataUnitdate.getValueFrom());
//                        daterange.setFromdate(fromDate);
//                        Todate todate = new Todate();
//                        todate.setStandarddate(arrDataUnitdate.getValueTo());
//                        daterange.setTodate(todate);
//                        unitdatestructured.setDaterange(daterange);
//                        did.getMDid().add(unitdatestructured);
//                    }
//
//                }
//                default -> {}
//            }
//
//
//        }
//        Archdesc archdesc = new Archdesc();
//        archdesc.setDid(did);
//        Ead ead = new Ead();
//        ead.setArchdesc(archdesc);
//        EadReaderWriter.marshal(ead, Path.of(aipDir.toString(), "EAD.xml"));
        ClassPathResource classPathResource = new ClassPathResource("exportDaTemplates/EAD-CONTEXT.xml");
        Path path = Paths.get(aipDir + "/metadata/descriptive");
        Files.createDirectories(path);
        Path resultPath = Paths.get(path + "/EAD-CONTEXT.xml");
        Files.copy(classPathResource.getInputStream(), resultPath);
        return resultPath;
    }

    private Path createPackageInfo(DaAip aip, Path aipDir, XMLGregorianCalendar createDate) throws IOException, JAXBException {
        DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
        PremisComplexType premisComplexType = new PremisComplexType();
        premisComplexType.setVersion("3.0");
        IntellectualEntity thisIntellectualEntity = new IntellectualEntity();
        thisIntellectualEntity.getObjectIdentifier().add(createObjectIdentifierComplexType("local", "_THIS"));
        premisComplexType.getObject().add(thisIntellectualEntity);

        IntellectualEntity aipIntellectualEntity = new IntellectualEntity();
        String localUUID = generateExportUUID();
        aipIntellectualEntity.getObjectIdentifier().add(createObjectIdentifierComplexType("local", localUUID));
        aipIntellectualEntity.getObjectIdentifier().add(createObjectIdentifierComplexType("AIP_ID", aip.getCode()));
        aipIntellectualEntity.getSignificantProperties().add(createSignificantPropertiesElement("AIP_VERSION", aipState.getAipVersion()));
        premisComplexType.getObject().add(aipIntellectualEntity);
        String elzaAgentUUID = generateExportUUID();
        AgentComplexType elzaAgent = getAgentComplexType("local", elzaAgentUUID, "Elza", "sof", "3.0");
        premisComplexType.getAgent().add(elzaAgent);

        String userAgentUUID = generateExportUUID();
        UserInfoVO loggedUserInfo = userService.getLoggedUserInfo();
        String userInfoName = loggedUserInfo.getPreferredName();
        AgentComplexType userAgent = getAgentComplexType("local", userAgentUUID, userInfoName, "org", null);
        premisComplexType.getAgent().add(userAgent);

        EventComplexType eventComplexType = new EventComplexType();
        LinkingAgentIdentifierComplexType userAgentIdentifierComplexType = new LinkingAgentIdentifierComplexType();
        userAgentIdentifierComplexType.setLinkingAgentIdentifierType(createStringPlusAuthority("local"));
        userAgentIdentifierComplexType.setLinkingAgentIdentifierValue(userAgentUUID);
        userAgentIdentifierComplexType.getLinkingAgentRole().add(createStringPlusAuthority("SUBMITTER"));
        eventComplexType.getLinkingAgentIdentifier().add(userAgentIdentifierComplexType);
        LinkingAgentIdentifierComplexType elzaAgentIdentifierComplexType = new LinkingAgentIdentifierComplexType();
        elzaAgentIdentifierComplexType.setLinkingAgentIdentifierType(createStringPlusAuthority("local"));
        elzaAgentIdentifierComplexType.setLinkingAgentIdentifierValue(elzaAgentUUID);
        elzaAgentIdentifierComplexType.getLinkingAgentRole().add(createStringPlusAuthority("imp"));
        eventComplexType.getLinkingAgentIdentifier().add(elzaAgentIdentifierComplexType);
        EventIdentifierComplexType eventIdentifierComplexType = new EventIdentifierComplexType();
        eventIdentifierComplexType.setEventIdentifierType(createStringPlusAuthority("local"));
        eventIdentifierComplexType.setEventIdentifierValue(generateExportUUID());
        eventComplexType.setEventIdentifier(eventIdentifierComplexType);
        eventComplexType.setEventDateTime(createDate.toString());
        eventComplexType.setEventType(createStringPlusAuthority("ing"));
        eventComplexType.getLinkingObjectIdentifier().add(createLinkingObjectIdentifier("local", "_THIS", "out"));
        eventComplexType.getLinkingObjectIdentifier().add(createLinkingObjectIdentifier("local", localUUID, "sou"));
        premisComplexType.getEvent().add(eventComplexType);

        JAXBElement<PremisComplexType> wrappedElement = XmlUtils.wrapElement("premis", premisComplexType);

        JAXBContext jaxbContext = JAXBContext.newInstance(PremisComplexType.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        String schemaLocation = "http://www.loc.gov/premis/v3 http://www.loc.gov/standards/premis/premis.xsd";
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, schemaLocation);
        AnyUriAdapter.register(marshaller, AnyUriAdapter.isLegacyDefault());
        marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new CustomNamespacePrefixMapper());

        StringWriter sw = new StringWriter();
        marshaller.marshal(wrappedElement, sw);
        String xml = sw.toString();
        String finalXml = xml.replace("premis:", "").replace("xmlns:premis", "xmlns");

        Path path = Paths.get(aipDir + "/metadata/preservation");
        Files.createDirectories(path);
        Path resultPath = Paths.get(path + "/PACKAGE-INFO.xml");
        try (FileWriter writer = new FileWriter(resultPath.toFile())) {
            writer.write(finalXml);
        }
        return resultPath;
    }

    private LinkingObjectIdentifierComplexType createLinkingObjectIdentifier(String type, String value, String role) {
        LinkingObjectIdentifierComplexType linkingObjectIdentifierComplexType = new LinkingObjectIdentifierComplexType();
        linkingObjectIdentifierComplexType.setLinkingObjectIdentifierType(createStringPlusAuthority(type));
        linkingObjectIdentifierComplexType.setLinkingObjectIdentifierValue(value);
        linkingObjectIdentifierComplexType.getLinkingObjectRole().add(createStringPlusAuthority(role));
        return linkingObjectIdentifierComplexType;
    }

    @NotNull
    private AgentComplexType getAgentComplexType(String agentIdentifierType, String agentIdentifierValue, String agentName, String agentType, String agentVersion) {
        AgentComplexType localAgent = new AgentComplexType();
        AgentIdentifierComplexType localAgentIdentifier = new AgentIdentifierComplexType();
        localAgentIdentifier.setAgentIdentifierType(createStringPlusAuthority(agentIdentifierType));

        localAgentIdentifier.setAgentIdentifierValue(agentIdentifierValue);
        localAgent.getAgentIdentifier().add(localAgentIdentifier);
        localAgent.getAgentName().add(createStringPlusAuthority(agentName));
        localAgent.setAgentType(createStringPlusAuthority(agentType));
        localAgent.setAgentVersion(agentVersion);
        return localAgent;
    }

    private StringPlusAuthority createStringPlusAuthority(String value) {
        return new StringPlusAuthority(value, null, null, null);
    }

    private static class CustomNamespacePrefixMapper extends NamespacePrefixMapper {
        public static final Map<String, String> NAMESPACE_MAP = Map.of(
                "http://www.loc.gov/premis/v3", "premis", // Bez prefixu pro tento jmenný prostor
                "http://www.w3.org/2001/XMLSchema-instance", "xsi",
                "http://www.w3.org/1999/xlink", "xlink"
        );

        private Map<String, String> namespaceMap;
        public CustomNamespacePrefixMapper(final Map<String, String> namespaceMap) {
            this.namespaceMap = namespaceMap;
        }
        public CustomNamespacePrefixMapper() {
            this(new HashMap<>(NAMESPACE_MAP));
        }
        @Override
        public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
            return namespaceMap.getOrDefault(namespaceUri, suggestion);
        }
    }

    private ObjectIdentifierComplexType createObjectIdentifierComplexType(String type, String value) {
        StringPlusAuthority stringPlusAuthority = new StringPlusAuthority();
        stringPlusAuthority.setValue(type);
        return new ObjectIdentifierComplexType(stringPlusAuthority, value, null);
    }

    private SignificantPropertiesComplexType createSignificantPropertiesElement(String type, String value) {
        JAXBElement<String> stringPlusAuthorityElement = XmlUtils.wrapElement("significantPropertiesType", type);
        JAXBElement<String> stringElement = XmlUtils.wrapElement("significantPropertiesValue", value);
        List<JAXBElement<?>> elements = new ArrayList<>();
        elements.add(stringPlusAuthorityElement);
        elements.add(stringElement);
        return new SignificantPropertiesComplexType(elements);
    }

    private void createMets(Path aipDir, Path packageInfoPath, Path eadPath, XMLGregorianCalendar createDate) throws JAXBException, DatatypeConfigurationException, IOException {
        Mets mets = new Mets();
        mets.getOtherAttributes().put(QName.valueOf("csip:CONTENTINFORMATIONTYPE"), "OTHER");
        mets.getOtherAttributes().put(QName.valueOf("csip:OTHERCONTENTINFORMATIONTYPE"), "change_request_v1_0");
        MetsType.MetsHdr metsHdr = new MetsType.MetsHdr();
        metsHdr.setCREATEDATE(createDate);
        MetsType.MetsHdr.Agent creatorAgent = createAgent("CREATOR", "OTHER", "SOFTWARE", "ELZA", "3.0");

        metsHdr.getAgent().add(creatorAgent);
        mets.setMetsHdr(metsHdr);

        String dmdSecUUID = generateExportUUID();
        String amdSecUUID =  generateExportUUID();
        MdSecType dmdSec = createDmdSec(dmdSecUUID, createDate, eadPath);
        mets.getDmdSec().add(dmdSec);
        AmdSecType amdSec = createAmdSec(amdSecUUID, createDate, packageInfoPath);
        mets.getAmdSec().add(amdSec);

        StructMapType structMap = new StructMapType();
        structMap.setTYPE("PHYSICAL");
        structMap.setLABEL("CSIP");
        DivType div = new DivType();
        div.setID( generateExportUUID());
        DivType innerDiv = new DivType();
        innerDiv.setID( generateExportUUID());
        innerDiv.setLABEL("Metadata");
        innerDiv.getDMDID().add(dmdSec);

        innerDiv.getADMID().add(amdSec.getDigiprovMD().get(0));

        div.getDiv().add(innerDiv);
        structMap.setDiv(div);
        structMap.setID(generateExportUUID());
        mets.getStructMap().add(structMap);
        mets.setOBJID(aipDir.getFileName().toString());
        mets.setTYPE("Dataset");
        mets.setPROFILE("https://stands.nacr.cz/da/2023/aip.xml");
        Path resultPath = Path.of(aipDir.toString(), "METS.xml");
        MetsReaderWriter.marshal(mets, resultPath);
        String content = Files.readString(resultPath, StandardCharsets.UTF_8);
        content = content.replace("<metsHdr", "<metsHdr csip:OAISPACKAGETYPE=\"AIP\"").replace("<note", "<note csip:NOTETYPE=\"SOFTWARE VERSION\"");
        Files.writeString(resultPath, content, StandardCharsets.UTF_8);
    }

    private String generateExportUUID() {
        return "uuid-" + UUID.randomUUID();
    }

    private AmdSecType createAmdSec(String amdSecUUID, XMLGregorianCalendar createDate, Path packageInfoPath) throws IOException {
        AmdSecType amdSecType = new AmdSecType();
        MdSecType digiprovMD = new MdSecType();
        digiprovMD.setID(amdSecUUID);
        digiprovMD.setGROUPID("PRESERVATION");
        digiprovMD.setSTATUS("CURRENT");
        MdSecType.MdRef mdRef = new MdSecType.MdRef();
        mdRef.setType("simple");
        mdRef.setHref("metadata/preservation/PACKAGE-INFO.xml");
        mdRef.setMDTYPE("PREMIS");
        mdRef.setLOCTYPE("URL");
        mdRef.setMIMETYPE("application/xml");
        mdRef.setSIZE(10L);
        mdRef.setCREATED(createDate);
        mdRef.setCHECKSUM(calculateSHA512(packageInfoPath));
        mdRef.setCHECKSUMTYPE("SHA-512");
        mdRef.setSIZE(Files.size(packageInfoPath));

        digiprovMD.setMdRef(mdRef);
        amdSecType.getDigiprovMD().add(digiprovMD);
        return amdSecType;
    }

    public static String calculateSHA512(Path filePath) {
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            // Využití DigestUtils pro výpočet SHA-512 hash
            return DigestUtils.sha512Hex(fis);
        } catch (IOException e) {
            logger.error("Nastala chyba při čtení souboru {}", filePath, e);
            return null;
        }
    }

    private MdSecType createDmdSec(String dmdSecUUID, XMLGregorianCalendar createDate, Path eadPath) throws IOException {
        MdSecType mdSecType = new MdSecType();
        mdSecType.setID(dmdSecUUID);
        mdSecType.setGROUPID("CONTEXTUAL");
        mdSecType.setCREATED(createDate);
        mdSecType.setSTATUS("CURRENT");


        MdSecType.MdRef mdRef = new MdSecType.MdRef();
        mdRef.setType("simple");
        mdRef.setHref("metadata/descriptive/EAD-CONTEXT.xml");
        mdRef.setMDTYPE("EAD");
        mdRef.setLOCTYPE("URL");
        mdRef.setCREATED(createDate);
        mdRef.setMIMETYPE("application/xml");
        mdRef.setCHECKSUM(calculateSHA512(eadPath));
        mdRef.setCHECKSUMTYPE("SHA-512");
        mdRef.setSIZE(Files.size(eadPath));


        mdSecType.setMdRef(mdRef);

        return mdSecType;
    }

    private MetsType.MetsHdr.Agent createAgent(String role, String type, String otherType, String name, String note) {
        MetsType.MetsHdr.Agent creatorAgent = new MetsType.MetsHdr.Agent();
        creatorAgent.setROLE(role);
        creatorAgent.setTYPE(type);
        creatorAgent.setOTHERTYPE(otherType);
        creatorAgent.setName(name);
        creatorAgent.getNote().add(note);
        return creatorAgent;
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

    /**
     * The next batch of waiting items: due ones only, of one repository and one package form, and
     * never of a repository that has a batch of the same direction in flight - one batch per
     * direction at a time, so the DA is not asked for more than before.
     *
     * @param inFlightState the state of a batch in flight of this direction
     */
    @Transactional
    public List<DaSyncQueueItem> getNextItems(int pageSize, DaSyncQueueItem.QueueItemState inFlightState,
                                              DaSyncQueueItem.QueueItemState... states) {
        Pageable pageable = PageRequest.of(0, pageSize);

        List<Integer> busy = new ArrayList<>(syncQueueItemRepository.findRepositoriesWithState(inFlightState));
        if (busy.isEmpty()) {
            busy.add(-1); // NOT IN () is not valid SQL
        }
        Iterable<DaSyncQueueItem> syncQueueItemIterable = syncQueueItemRepository.findDueByStates(Arrays.asList(states),
                OffsetDateTime.now(), busy, pageable);
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

    /**
     * The active items of the batch in flight whose next question about it is due, or an empty
     * list. Items of the batch that were withdrawn meanwhile are left out - the batch is finished
     * without them.
     */
    @Transactional
    public List<DaSyncQueueItem> getDueBatch(DaSyncQueueItem.QueueItemState inFlightState) {
        List<DaSyncQueueItem> due = syncQueueItemRepository.findDueInFlight(inFlightState, OffsetDateTime.now(),
                PageRequest.of(0, 1)).getContent();
        if (due.isEmpty()) {
            return List.of();
        }
        DaSyncQueueItem first = due.get(0);
        if (first.getBatchId() == null) {
            return List.of(first);
        }
        List<DaSyncQueueItem> batch = syncQueueItemRepository
                .findByBatchIdAndStateAndActiveIsTrueOrderBySyncQueueItemId(first.getBatchId(), inFlightState);
        // initialize what the processors read outside of this transaction
        batch.forEach(item -> item.getDigitalRepository().getExternalSystemId());
        return batch;
    }

    /**
     * @return when the processor of the given states has to look at the queue again at the
     *         latest, or null when no item waits for a time
     */
    @Transactional
    public OffsetDateTime getEarliestAttempt(Collection<DaSyncQueueItem.QueueItemState> states) {
        return syncQueueItemRepository.findEarliestAttempt(states);
    }

    /**
     * The items were handed over to the DA as one batch: requested for download or sent for
     * ingest. The first question about the batch is due after the status interval.
     */
    @Transactional
    public void markBatch(Collection<DaSyncQueueItem> items, DaSyncQueueItem.QueueItemState state, String batchId,
                          ArrDigitalRepository digitalRepository) {
        OffsetDateTime now = OffsetDateTime.now();
        for (DaSyncQueueItem item : items) {
            item.setState(state);
            item.setBatchId(batchId);
            item.setDate(now);
            item.setStateMessage(null);
            item.setNextAttemptAt(now.plus(Duration.ofMillis(statusPollMillis(digitalRepository))));
        }
        syncQueueItemRepository.saveAll(items);
    }

    /** The DA still works on the batch; the next question is due after the status interval. */
    @Transactional
    public void scheduleNextCheck(Collection<DaSyncQueueItem> items, ArrDigitalRepository digitalRepository) {
        OffsetDateTime next = OffsetDateTime.now().plus(Duration.ofMillis(statusPollMillis(digitalRepository)));
        for (DaSyncQueueItem item : items) {
            item.setNextAttemptAt(next);
        }
        syncQueueItemRepository.saveAll(items);
    }

    /**
     * How long a processor with nothing to do sleeps: until the earliest waiting item is due,
     * at most the given time.
     */
    static long idleMillis(@Nullable OffsetDateTime earliestAttempt, long max) {
        if (earliestAttempt == null) {
            return max;
        }
        long untilDue = Duration.between(OffsetDateTime.now(), earliestAttempt).toMillis();
        return Math.max(50, Math.min(untilDue, max));
    }

    /**
     * Delay before the next attempt after the given number of failures: the status interval,
     * doubled with every failure, at most {@link #MAX_RETRY_DELAY}. A DA that is down for a while
     * is not flooded, and "Zkusit znovu teď" takes the item at once.
     */
    static Duration retryDelay(ArrDigitalRepository digitalRepository, int failures) {
        long base = statusPollMillis(digitalRepository);
        int doublings = Math.min(Math.max(failures - 1, 0), 20);
        return Duration.ofMillis(Math.min(base << doublings, MAX_RETRY_DELAY.toMillis()));
    }

    /** The repository of a queue item read in an earlier transaction, with its settings loaded. */
    private ArrDigitalRepository repositoryOf(DaSyncQueueItem item) {
        return externalSystemService.getDigitalRepository(item.getDigitalRepository().getExternalSystemId());
    }

    /**
     * A transient failure of an exchange about the items: they keep their state and are tried
     * again later, after a longer delay with every failure. The description says what failed;
     * the number of attempts is added.
     */
    @Transactional
    public void scheduleRetry(Collection<DaSyncQueueItem> items, String description) {
        OffsetDateTime now = OffsetDateTime.now();
        for (DaSyncQueueItem item : items) {
            int attempts = (item.getAttemptCount() == null ? 0 : item.getAttemptCount()) + 1;
            item.setAttemptCount(attempts);
            item.setDate(now);
            item.setNextAttemptAt(now.plus(retryDelay(repositoryOf(item), attempts)));
            item.setStateMessage(StringUtils.abbreviate(description + " (pokusů: " + attempts + ")",
                                                        STATE_MESSAGE_MAX_LENGTH));
        }
        syncQueueItemRepository.saveAll(items);
    }

    /**
     * The DA gave the requested download batch up (the status answered 403/404, which the DA API
     * defines as a permanent failure of the request). The package is still there, so the items
     * return to waiting and a new batch is requested later - a download is never given up.
     */
    @Transactional
    public void returnToPending(List<DaSyncQueueItem> items, Exception failure) {
        for (DaSyncQueueItem item : items) {
            boolean known = item.getAip() != null
                    && aipStateRepository.findByDaAipAndDeleteChangeIsNull(item.getAip()) != null;
            item.setState(known ? DaSyncQueueItem.QueueItemState.UPDATE : DaSyncQueueItem.QueueItemState.IMPORT_NEW);
            item.setBatchId(null);
        }
        recordDownloadFailure(items, failure);
    }

    /** Whether the request waits for the queue - delivered to the DA or not. */
    static boolean isWaiting(DaSyncQueueItem item) {
        return BooleanUtils.isTrue(item.getActive()) && WAITING_STATES.contains(item.getState());
    }

    /** Whether the request waits and the DA has not received it - only such can be cancelled. */
    static boolean isUndelivered(DaSyncQueueItem item) {
        return BooleanUtils.isTrue(item.getActive()) && UNDELIVERED_STATES.contains(item.getState());
    }

    /** Whether the request ended by an error and nothing newer replaced it. */
    static boolean isFailed(DaSyncQueueItem item) {
        return BooleanUtils.isTrue(item.getActive())
                && (item.getState() == DaSyncQueueItem.QueueItemState.IMPORT_ERROR
                    || item.getState() == DaSyncQueueItem.QueueItemState.EXPORT_ERROR);
    }

    /**
     * "Zkusit znovu teď": the waiting requests are taken on the next cycle of the queue, without
     * the retry delay - a question about a batch in flight as well.
     *
     * @param items waiting requests ({@link #isWaiting})
     */
    @Transactional
    public void retryNow(Collection<DaSyncQueueItem> items) {
        for (DaSyncQueueItem item : items) {
            item.setNextAttemptAt(null);
        }
        syncQueueItemRepository.saveAll(items);
    }

    /**
     * Cancels requests the DA has not received yet; their actions end skipped.
     *
     * @param items undelivered requests ({@link #isUndelivered})
     */
    @Transactional
    public void withdraw(Collection<DaSyncQueueItem> items) {
        for (DaSyncQueueItem item : items) {
            deactivateQueueItems(item.getCode(), item.getAip(), item.getDigitalRepository(), List.of(item.getState()),
                                 "Požadavek zrušil uživatel.");
        }
    }

    /**
     * Repeats failed requests. A download is requested again in the form that failed; it
     * replaces the failed request, which stays in the history. An export sends a new change
     * package built from the current description - the description may have been corrected
     * since the DA refused the package.
     *
     * @param items failed requests ({@link #isFailed})
     * @return the number of requests repeated
     */
    @Transactional
    public int repeat(Collection<DaSyncQueueItem> items) {
        int repeated = 0;
        List<Integer> exportAipIds = new ArrayList<>();
        for (DaSyncQueueItem item : items) {
            if (item.getState() == DaSyncQueueItem.QueueItemState.EXPORT_ERROR) {
                if (item.getAip() != null) {
                    exportAipIds.add(item.getAip().getAipId());
                }
                continue;
            }
            DaAipState aipState = item.getAip() == null ? null
                    : aipStateRepository.findByDaAipAndDeleteChangeIsNull(item.getAip());
            if (item.getAip() != null && aipState == null) {
                continue; // invalidated - nothing to download
            }
            createSyncQueueItem(item.getCode(), item.getAip(), item.getDigitalRepository(),
                                aipState != null ? DaSyncQueueItem.QueueItemState.UPDATE
                                                 : DaSyncQueueItem.QueueItemState.IMPORT_NEW,
                                item.getAipVersion(), item.getAipType(), true);
            repeated++;
        }
        if (!exportAipIds.isEmpty()) {
            DaAipAction action = applicationContext.getBean(DaService.class).aipExportAip(exportAipIds);
            repeated += (int) aipActionItemRepository.findByAipActionOrderByAipActionItemId(action).stream()
                    .filter(i -> i.getState() != DaAipActionItemState.SKIPPED && i.getState() != DaAipActionItemState.ERROR)
                    .count();
        }
        return repeated;
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
        changeQueueItemsState(syncQueueItemList, state, null);
    }

    /**
     * Sets the state of the given queue items and describes why they ended in it.
     *
     * @param stateMessage description of the outcome, expected for the error states - it is the
     *                     only account of the failure the user can reach
     */
    @Transactional
    public void changeQueueItemsState(Collection<DaSyncQueueItem> syncQueueItemList, DaSyncQueueItem.QueueItemState state,
                                      @Nullable String stateMessage) {
        if (CollectionUtils.isNotEmpty(syncQueueItemList)) {
            OffsetDateTime now = OffsetDateTime.now();
            for (DaSyncQueueItem syncQueueItem : syncQueueItemList) {
                syncQueueItem.setState(state);
                syncQueueItem.setStateMessage(StringUtils.abbreviate(stateMessage, STATE_MESSAGE_MAX_LENGTH));
                syncQueueItem.setDate(now);
                syncQueueItem.setNextAttemptAt(null);
            }
            syncQueueItemRepository.saveAll(syncQueueItemList);
        }
    }

    /**
     * Records a failed download of the package of the given queue items. Nothing of the package
     * arrived, so unlike a failure of the processing the items are not closed: they stay in
     * their state, sent behind their peers by the raised attempt count, and are retried after a
     * delay growing with the failures ({@link #retryDelay}) - nothing gives a download up, the
     * DA holds the package and every retry may succeed. The failure is described on each item and as a problem of
     * its AIP - an AIP the DA announced but ELZA could never download is created from what the
     * change carries, so the user finds it in the AIP list instead of only in the queue.
     */
    @Transactional
    public void recordDownloadFailure(List<DaSyncQueueItem> syncQueueItemList, Exception failure) {
        AipProblem problem = AipProblem.downloadFailure(failure);
        OffsetDateTime now = OffsetDateTime.now();
        for (DaSyncQueueItem syncQueueItem : syncQueueItemList) {
            int attempts = (syncQueueItem.getAttemptCount() == null ? 0 : syncQueueItem.getAttemptCount()) + 1;
            syncQueueItem.setAttemptCount(attempts);
            syncQueueItem.setDate(now);
            syncQueueItem.setNextAttemptAt(now.plus(retryDelay(repositoryOf(syncQueueItem), attempts)));
            syncQueueItem.setStateMessage(StringUtils.abbreviate(
                    problem.description() + " (pokusů: " + attempts + ")", STATE_MESSAGE_MAX_LENGTH));
            recordAipProblem(syncQueueItem, problem);
        }
        syncQueueItemRepository.saveAll(syncQueueItemList);
    }

    /**
     * Describes the problem on the AIP of the queue item, whatever kind of problem it is - the
     * download that never delivered the package as well as the processing of a package that
     * arrived. An AIP unknown to ELZA is created first, with the code and version the change
     * carries as all that is known about it, so that the user finds the failure in the AIP list
     * and not only in the queue; the next successfully processed package replaces this state,
     * which clears the problem the same way it is cleared for an AIP that already existed.
     */
    private void recordAipProblem(DaSyncQueueItem syncQueueItem, AipProblem problem) {
        DaAip aip = syncQueueItem.getAip() != null
                ? syncQueueItem.getAip()
                : aipRepository.findByCode(syncQueueItem.getCode());
        DaAipState aipState;
        if (aip == null) {
            aip = new DaAip();
            aip.setCode(syncQueueItem.getCode());
            aip.setDigitalRepository(syncQueueItem.getDigitalRepository());
            aipRepository.save(aip);

            DaChange change = new DaChange();
            change.setType(DaChangeType.AIP_CREATE);
            change.setChangeDate(LocalDateTime.now());
            change.setDaAip(aip);
            changeRepository.save(change);

            aipState = new DaAipState();
            aipState.setDaAip(aip);
            aipState.setCreateChange(change);
            aipState.setAipVersion(StringUtils.defaultString(syncQueueItem.getAipVersion()));
        } else {
            aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
            if (aipState == null) {
                return;
            }
        }
        syncQueueItem.setAip(aip);
        referenceResolver.recordProblem(aipState, problem);
        aipStateRepository.save(aipState);
    }

    /**
     * Closes the given queue items in the state their own problem ended them in. Used where the
     * items of one batch fail for different reasons and a shared description would lose them.
     *
     * The problem is written on the AIP as well, so a package that arrived and could not be
     * processed is as visible as one that could not be downloaded - the failure is terminal,
     * nothing retries it, and the queue alone is not where the user looks for it.
     *
     * The action item the queue item was carrying out is finished here too: these items are
     * taken out of the batch, so the caller closes the batch without them and an action item
     * nobody finishes leaves the request the user is watching running forever.
     */
    @Transactional
    public void failQueueItems(Map<DaSyncQueueItem, AipProblem> problemByItem, DaSyncQueueItem.QueueItemState state) {
        if (MapUtils.isNotEmpty(problemByItem)) {
            OffsetDateTime now = OffsetDateTime.now();
            problemByItem.forEach((syncQueueItem, problem) -> {
                syncQueueItem.setState(state);
                syncQueueItem.setStateMessage(StringUtils.abbreviate(problem.description(), STATE_MESSAGE_MAX_LENGTH));
                syncQueueItem.setDate(now);
                recordAipProblem(syncQueueItem, problem);
                actionService.completeFromQueue(List.of(syncQueueItem), DaAipActionItemState.ERROR,
                                                problem.description());
            });
            syncQueueItemRepository.saveAll(problemByItem.keySet());
        }
    }

    public void processPackageInfo(ArrDigitalRepository digitalRepository, InputStream tempZipInputStream, AipType aipType, List<DaSyncQueueItem> syncQueueItemList) throws IOException {
        Path tempDir = Files.createTempDirectory("unzipped");
        try {
            processPackageInfo(digitalRepository, tempZipInputStream, aipType, syncQueueItemList, tempDir);
        } finally {
            PathUtils.deleteDirectory(tempDir);
        }
    }

    private void processPackageInfo(ArrDigitalRepository digitalRepository, InputStream tempZipInputStream, AipType aipType,
                                    List<DaSyncQueueItem> syncQueueItemList, Path tempDir) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream((tempZipInputStream))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                // A package that does not have the expected layout is reported by the file that
                // is missing from it, which says nothing about what the DA did send instead -
                // the names of the received entries are the only account of that.
                logger.debug("Balíček dávky obsahuje položku {}", entry.getName());
                Path filePath = AipPackageFiles.resolveInside(tempDir, entry.getName());
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

            // The directory of the package is named by the code of the AIP, which is the code
            // of its queue item - a package that fails is reported on its own item, so one bad
            // package neither hides itself nor takes the rest of the batch down with it.
            Map<DaSyncQueueItem, AipProblem> failedItems = new LinkedHashMap<>();

            for (File aipDir : aipDirSet) {
                if (!syncQueueItemMap.containsKey(aipDir.getName())) {
                    // withdrawn while the DA prepared the batch (invalidated, superseded): its
                    // package must not bring the AIP back
                    logger.info("Balíček {} dávky už není požadován, přeskočen", aipDir.getName());
                    continue;
                }
                DaAipState aipState;
                try {
                    Path packageInfo = AipPackageFiles.packageInfo(aipDir.toPath());
                    aipState = packageInfoService.processPackageInfo(digitalRepository, packageInfo.toFile());
                    // The load flags of the AIP are set by storing the package, so a package is
                    // stored only when it is what its type claims: a DA that answers a metadata
                    // request with less would otherwise be recorded as having delivered the
                    // metadata, and the AIP could never be asked for them again.
                    if (aipType != AipType.PACKAGE_INFO) {
                        checkMetadataFiles(aipDir.toPath());
                    }
                } catch (Exception e) {
                    AipProblem problem = AipProblem.of(e);
                    logger.error("Balíček {} se nepodařilo načíst: {}", aipDir.getName(), problem.description(), e);
                    DaSyncQueueItem failedItem = syncQueueItemMap.getOrDefault(aipDir.getName(), null);
                    if (failedItem != null) {
                        failedItems.put(failedItem, problem);
                    }
                    continue;
                }

                if (aipState != null && aipType != AipType.PACKAGE_INFO) {
                    Path zipDir = createZip(aipDir, resourcePathResolver.getAipDir());
                    DaSyncQueueItem syncQueueItem = syncQueueItemMap.getOrDefault(aipState.getDaAip().getCode(), null);
                    applicationContext.getBean(DaService.class).createImportLocalCache(aipState, digitalRepository, aipType, zipDir, syncQueueItem);
                }
            }

            // Taken out of the batch before the caller records its result, so the failure is
            // not overwritten by the state of the packages that were loaded.
            if (!failedItems.isEmpty()) {
                syncQueueItemList.removeAll(failedItems.keySet());
                applicationContext.getBean(DaService.class)
                        .failQueueItems(failedItems, DaSyncQueueItem.QueueItemState.IMPORT_ERROR);
            }
        }
    }

    /**
     * A metadata package is complete when it has the root METS and every file of its metadata
     * sections - the files are taken from the METS, so their names are whatever the package
     * gives them.
     */
    static void checkMetadataFiles(Path root) {
        Path metsFile = AipPackageFiles.mets(root);
        MetsType mets;
        try {
            mets = MetsReaderWriter.unmarshal(metsFile);
        } catch (Exception e) {
            throw AipProblemException.metadata("Soubor METS.xml balíčku se nepodařilo přečíst: " + AipProblem.reason(e),
                                               AipPackageFiles.METS, e);
        }
        for (String href : AipPackageFiles.metadataHrefs(mets)) {
            AipPackageFiles.referenced(root, href);
        }
    }

    static void deleteTempDirectory(Path tempDir) {
        if (tempDir == null) {
            return;
        }
        try {
            PathUtils.deleteDirectory(tempDir);
        } catch (IOException e) {
            logger.warn("Nepodařilo se smazat dočasný adresář {}: {}", tempDir, e.getMessage());
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
        }

        return tempDir;
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

        // The load flags describe the package on disk, so they are written here, where it is
        // stored, and nowhere else: a complete package carries the metadata too, and a metadata
        // package arriving later replaces a complete one.
        DaAipState storedState = aipStateRepository.findById(aipState.getAipStateId()).orElse(aipState);
        storedState.setMetadataLoad(aipType == AipType.METADATA_BASE || aipType == AipType.AIP_BASE);
        storedState.setCompleteAipLoad(aipType == AipType.AIP_BASE);
        aipStateRepository.save(storedState);
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

    /**
     * The caller supplies the transaction: every call is internal to this class, so a
     * {@code @Transactional} here would be bypassed together with the proxy.
     */
    public DaSyncQueueItem createSyncQueueItem(String code, DaAip aip, ArrDigitalRepository digitalRepository,
                                               DaSyncQueueItem.QueueItemState queueItemState, String aipVersion, AipType aipType, boolean active) {
        Validate.isTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                        "Zařazení do fronty vyžaduje otevřenou transakci");

        // The request being queued replaces the ones already waiting for the same AIP.
        deactivateQueueItems(code, aip, digitalRepository, getQueueItemStates(queueItemState),
                             "Požadavek nahradil novější požadavek na tentýž AIP.");

        DaSyncQueueItem syncQueueItem = new DaSyncQueueItem();
        syncQueueItem.setCode(code);
        syncQueueItem.setAip(aip);
        syncQueueItem.setDigitalRepository(digitalRepository);
        syncQueueItem.setAipVersion(aipVersion);
        syncQueueItem.setState(queueItemState);
        syncQueueItem.setAipType(aipType);
        syncQueueItem.setActive(active);
        syncQueueItem.setDate(OffsetDateTime.now());
        return syncQueueItemRepository.save(syncQueueItem);
    }

    /**
     * Withdraws the active queue items of the AIP in the given states. Their action items are
     * closed here, where they lose their queue item - the processors read active items only, so
     * nothing else would ever report on them again.
     *
     * @param reason why the action items end skipped
     */
    private void deactivateQueueItems(String code, @Nullable DaAip aip, ArrDigitalRepository digitalRepository,
                                      Collection<DaSyncQueueItem.QueueItemState> queueItemStates, String reason) {
        // One request of an AIP at a time: the pending one is withdrawn only if it is seen committed
        if (aip != null && aip.getAipId() != null) {
            aipRepository.lockByIds(List.of(aip.getAipId()));
        }

        for (Integer withdrawn : syncQueueItemRepository.findActionItemIdsToSupersede(code, digitalRepository, queueItemStates)) {
            actionService.recordOutcome(withdrawn, DaAipActionItemState.SKIPPED, reason);
        }

        syncQueueItemRepository.updateActiveByCodeAndDigitalRepositoryAndStateInAndActiveIsTrue(code, digitalRepository, queueItemStates);
    }

    private List<DaSyncQueueItem.QueueItemState> getQueueItemStates(DaSyncQueueItem.QueueItemState queueItemState) {
        List<DaSyncQueueItem.QueueItemState> queueItemStates = new ArrayList<>();
        switch (queueItemState) {
            case IMPORT_NEW, DOWNLOAD_REQUESTED, IMPORT_OK, IMPORT_ERROR, UPDATE:
                // a download in flight is superseded as well: the batch is finished without it
                queueItemStates.addAll(getQueueImportStates());
                break;
            case EXPORT_NEW, EXPORT_SENT, EXPORT_OK, EXPORT_ERROR:
                // an export the DA received is never withdrawn, its result is still awaited
                queueItemStates.add(DaSyncQueueItem.QueueItemState.EXPORT_NEW);
                queueItemStates.add(DaSyncQueueItem.QueueItemState.EXPORT_OK);
                queueItemStates.add(DaSyncQueueItem.QueueItemState.EXPORT_ERROR);
                break;
        }
        return queueItemStates;
    }

    /** Requests the DA has not received yet - the only ones a user may withdraw. */
    private static final List<DaSyncQueueItem.QueueItemState> UNDELIVERED_STATES = List.of(
            DaSyncQueueItem.QueueItemState.IMPORT_NEW,
            DaSyncQueueItem.QueueItemState.UPDATE,
            DaSyncQueueItem.QueueItemState.EXPORT_NEW);

    /** Requests waiting for the queue, delivered to the DA or not. */
    private static final List<DaSyncQueueItem.QueueItemState> WAITING_STATES = List.of(
            DaSyncQueueItem.QueueItemState.IMPORT_NEW,
            DaSyncQueueItem.QueueItemState.UPDATE,
            DaSyncQueueItem.QueueItemState.DOWNLOAD_REQUESTED,
            DaSyncQueueItem.QueueItemState.EXPORT_NEW,
            DaSyncQueueItem.QueueItemState.EXPORT_SENT);

    /** The longest pause between two attempts after failures. */
    static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);

    public static Collection<DaSyncQueueItem.QueueItemState> getQueueImportStates() {
        List<DaSyncQueueItem.QueueItemState> states = new ArrayList<>();
        states.add(DaSyncQueueItem.QueueItemState.UPDATE);
        states.add(DaSyncQueueItem.QueueItemState.IMPORT_NEW);
        states.add(DaSyncQueueItem.QueueItemState.DOWNLOAD_REQUESTED);
        states.add(DaSyncQueueItem.QueueItemState.IMPORT_OK);
        states.add(DaSyncQueueItem.QueueItemState.IMPORT_ERROR);
        return states;
    }

    private static Collection<DaSyncQueueItem.QueueItemState> getQueueAllStates() {
        List<DaSyncQueueItem.QueueItemState> states = new ArrayList<>(getQueueImportStates());
        states.addAll(getQueueExportStates());
        return states;
    }

    /** Every state except the export the DA received - that one is awaited whatever happens. */
    private static Collection<DaSyncQueueItem.QueueItemState> getQueueWithdrawableStates() {
        List<DaSyncQueueItem.QueueItemState> states = new ArrayList<>(getQueueAllStates());
        states.remove(DaSyncQueueItem.QueueItemState.EXPORT_SENT);
        return states;
    }

    public static Collection<DaSyncQueueItem.QueueItemState> getQueueExportStates() {
        List<DaSyncQueueItem.QueueItemState> states = new ArrayList<>();
        states.add(DaSyncQueueItem.QueueItemState.EXPORT_NEW);
        states.add(DaSyncQueueItem.QueueItemState.EXPORT_SENT);
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

    /**
     * @return how long to wait before asking the DA again whether it has finished a batch
     */
    public static long statusPollMillis(ArrDigitalRepository digitalRepository) {
        Integer seconds = digitalRepository.getStatusPollInterval();
        int interval = seconds == null ? ArrDigitalRepository.DEFAULT_STATUS_POLL_INTERVAL : Math.max(1, seconds);
        return interval * 1000L;
    }

    public boolean downloadStatusFinished(ArrDigitalRepository digitalRepository, String batchId) {
        DownloadDownloadStatus status = daConnector.downloadStatus(digitalRepository, batchId);
        return status.getState() == RequestState.FINISHED;
    }

    /**
     * Downloads the prepared batch over the DA API; the caller closes the returned content.
     */
    public SpooledContent downloadDownload(ArrDigitalRepository digitalRepository, String batchId) throws ApiException {
        return daConnector.downloadDownload(digitalRepository, batchId);
    }

    /**
     * Downloads the prepared batch over File Transfer; the caller closes the returned content,
     * which deletes the downloaded file.
     */
    public SpooledContent downloadFileTransfer(ArrDigitalRepository digitalRepository, String batchId) throws IOException {
        Path downloadDir = Files.createTempDirectory(batchId);
        try {
            daConnector.downloadFileTransfer(digitalRepository, batchId, downloadDir);
            Path downloaded;
            try (Stream<Path> str = Files.walk(downloadDir)
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".zip"))) {
                downloaded = str.findFirst().orElseThrow(() -> new IllegalStateException("Nenalezen stažený soubor přes Filetransfer"));
            }
            // keep only the package, the download directory is removed below
            Path zip = Files.createTempFile("da-ft-", ".zip");
            Files.move(downloaded, zip, StandardCopyOption.REPLACE_EXISTING);
            return SpooledContent.ofTempFile(zip);
        } finally {
            PathUtils.deleteDirectory(downloadDir);
        }
    }

    public boolean ingestStatusFinished(ArrDigitalRepository digitalRepository, String batchId) {
        IngestIngestStatus status = daConnector.ingestStatus(digitalRepository, batchId);
        return status.getState() == RequestState.FINISHED;
    }

    @Nullable
    public IngestIngestResult ingestResult(ArrDigitalRepository digitalRepository, String batchId) {
        return daConnector.ingestResult(digitalRepository, batchId);
    }

    public DaUploadRequestImpl createDaUploadRequest(Path exportDir) {
        GenericDataType genericDataType = new GenericDataType();
        genericDataType.setId(UUID.randomUUID().toString());
        genericDataType.setType("ingest");
        return new DaUploadRequestImpl(exportDir, genericDataType);
    }

    public Transfer ingestFileTransfer(ArrDigitalRepository digitalRepository, DaUploadRequestImpl daUploadRequest) {
        return daConnector.ingestFileTransfer(digitalRepository, daUploadRequest);
    }

    /**
     * Attaches an AIP, or one part of it, to a unit of description.
     *
     * Every link the digital archive creates goes through here, so that "Vícenásobné napojení" is
     * asked about once. What counts as "already attached" is the object being attached: a whole
     * package is measured against the links of the package, one part against the links of that
     * part, so attaching several parts of one AIP to the same unit of description stays possible.
     *
     * @return the link, existing when the object already hangs on this unit of description
     */
    ArrDaLink linkToNode(DaAip daAip, @Nullable DaDao daDao, ArrNode arrNode,
                                 ArrDaoLink.LinkType linkType, ArrChange change) {
        // The invalidation holds the lock until it commits, so the state is read either before it
        // started or after it closed the links - a link cannot be created in between.
        aipRepository.lockByIds(List.of(daAip.getAipId()));
        if (aipStateRepository.findByDaAipAndDeleteChangeIsNull(daAip) == null) {
            throw new BusinessException("AIP " + daAip.getCode() + " byl v digitálním archivu zneplatněn, nelze jej připojit.",
                                        BaseCode.INVALID_STATE);
        }
        List<ArrDaLink> liveLinks = daDao == null
                ? daLinkRepository.findByAip_AipIdAndDaDaoIsNullAndDeleteChangeIsNull(daAip.getAipId())
                : daLinkRepository.findByDaDaoInAndDeleteChangeIsNull(List.of(daDao));
        Optional<ArrDaoLink> existing = daoLinkPolicy.checkCanLink(liveLinks, arrNode.getNodeId(),
                                                                   daAip.getDigitalRepository());
        if (existing.isPresent()) {
            return (ArrDaLink) existing.get();
        }

        ArrDaLink arrDaoLink = new ArrDaLink();
        arrDaoLink.setAip(daAip);
        arrDaoLink.setNode(arrNode);
        arrDaoLink.setDaDao(daDao);
        arrDaoLink.setLinkType(linkType);
        arrDaoLink.setCreateChange(change);
        daoLinkRepository.save(arrDaoLink);
        linkStateResolver.refreshFor(daAip);
        return arrDaoLink;
    }



    /**
     * Linking an AIP to a unit of description changes the archival description of the fund, so it
     * takes the same permission as arranging it. The check cannot be left to {@link Authorization}:
     * the fund is only known once the node is read, and an @AuthParam of type NODE resolves no fund
     * id, so a FUND_ARR check made from it would deny everyone.
     */
    private void checkArrPermission(ArrNode node) {
        UserDetail userDetail = userService.getLoggedUserDetail();
        AuthorizationRequest request = AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.FUND_ARR_ALL)
                .or(Permission.FUND_ARR, node.getFundId());
        if (userDetail == null || !request.matches(userDetail)) {
            throw Authorization.createAccessDeniedException(request.getPermissions());
        }
    }

    @Transactional
    public void createDaoLink(Integer aipId, Integer daoId, Integer nodeId, ArrDaoLink.LinkType linkType) {
        ArrNode node = nodeRepository.getOneCheckExist(nodeId);
        checkArrPermission(node);
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, node);
        DaAip aip = findAipById(aipId);
        DaDao daDao = null;

        if (daoId != null) {
            daDao = findDaoById(daoId);
        }

        linkToNode(aip, daDao, node, linkType, change);
    }

    @Transactional
    public ArrDaLink connectToJP(Integer nodeId, Integer daAipId) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaAip daAip = findAipById(daAipId);
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
        return linkToNode(daAip, null, arrNode, ArrDaoLink.LinkType.AIP, change);
    }

    @Transactional
    public void connectPartListToJP(Integer nodeId, Integer daAipId, List<Integer> daDaoIdList) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaAip daAip = findAipById(daAipId);
        for (Integer daDaoId : daDaoIdList) {
            DaDao daDao = findDaoById(daDaoId);
            connectPartToJP(arrNode, daAip, daDao);
        }
    }

    @Transactional
    public void connectPartToJP(ArrNode arrNode,  DaAip daAip, DaDao daDao) {
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
        linkToNode(daAip, daDao, arrNode, ArrDaoLink.LinkType.PART_AIP, change);
    }

    @Transactional
    public void createJPFromSelectedList(Integer nodeId, Integer daAipId, List<Integer> daDaoIdList) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaAip daAip = findAipById(daAipId);
        for (Integer daDaoId : daDaoIdList) {
            DaDao daDao = findDaoById(daDaoId);
            createJPFromSelected(arrNode, daAip, daDao);
        }
    }

    @Transactional
    public void createJPFromSelected(ArrNode arrNode, DaAip daAip, DaDao daDao) {

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
        ArrNode newNode = createChildNode(arrNode, change);
        if (daDao != null) {
            connectPartToJP(newNode, daAip, daDao);
        } else {
            connectToJP(newNode.getNodeId(), daAip.getAipId());
        }
    }

    @Transactional
    public void connectSelectedListToJP(Integer nodeId, Integer daAipId, List<Integer> daDaoIdList) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaAip daAip = findAipById(daAipId);
        for (Integer daDaoId : daDaoIdList) {
            DaDao daDao = findDaoById(daDaoId);
            connectSelectedToJP(arrNode, daAip, daDao);
        }
    }

    @Transactional
    public void connectSelectedToJP(ArrNode arrNode, DaAip daAip, DaDao daDao) {
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
        linkToNode(daAip, daDao, arrNode, ArrDaoLink.LinkType.COMPONENT_AIP, change);
    }

    @Transactional
    public void createAndLinkFromSelectedList(Integer nodeId, Integer daAipId, List<Integer> daDaoIdList) {
        ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
        DaAip daAip = findAipById(daAipId);
        for (Integer daDaoId : daDaoIdList) {
            DaDao daDao = findDaoById(daDaoId);
            createAndLinkFromSelected(arrNode, daAip, daDao);
        }
    }

    @Transactional
    public void createAndLinkFromSelected(ArrNode arrNode, DaAip daAip, DaDao daDao) {
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
        ArrNode newNode = createChildNode(arrNode, change);
        linkToNode(daAip, daDao, newNode, ArrDaoLink.LinkType.PART_AIP, change);
    }

    /**
     * Creates a new child node and its level under {@code parentNode} at the given
     * position, registers the node in the cache (only once the level exists), locks
     * the parent and publishes the add-level event.
     *
     * @return the newly created child node
     */
    private ArrNode createChildNode(ArrNode parentNode, ArrChange change, int position) {
        return createChildNode(parentNode, change, position, generateUuid());
    }

    /** See {@link #createChildNode(ArrNode, ArrChange, int)}; the new node gets the given UUID. */
    ArrNode createChildNode(ArrNode parentNode, ArrChange change, int position, String uuid) {
        // checked here, inside the transaction creating the level: the level tree cache follows
        // the change only after commit, through secured services, and cannot undo it
        checkArrPermission(parentNode);
        ArrNode newNode = arrangementService.createNode(parentNode.getFund(), uuid, change);

        ArrLevel arrLevel = new ArrLevel();
        arrLevel.setNodeParent(parentNode);
        arrLevel.setNode(newNode);
        arrLevel.setCreateChange(change);
        arrLevel.setPosition(position);
        levelRepository.save(arrLevel);

        // the node now has an active level
        nodeCacheService.addNodeToCache(newNode);

        ArrFundVersion fundVersion = fundVersionRepository
                .findByFundIdAndLockChangeIsNull(parentNode.getFund().getFundId());
        ArrLevel parentLevel = arrangementService.lockNode(parentNode, fundVersion, change);
        eventNotificationService.publishEvent(EventFactory.createAddNodeEvent(EventType.ADD_LEVEL_UNDER, fundVersion,
                parentLevel, arrLevel));
        return newNode;
    }

    /**
     * Creates a new child node and its level appended as the last child of
     * {@code parentNode}. See {@link #createChildNode(ArrNode, ArrChange, int)}.
     */
    private ArrNode createChildNode(ArrNode parentNode, ArrChange change) {
        Integer maxPosition = levelRepository.findMaxPositionUnderParent(parentNode);
        if (maxPosition == null) {
            maxPosition = 0;
        }
        return createChildNode(parentNode, change, maxPosition + 1);
    }


    /**
     * What the steps of a connect action need beyond the AIP they act on.
     *
     * @param nodeId      the unit of description to attach to, or the one to create under
     * @param changeId    the change shared by everything the action creates, when the prologue
     *                    made one
     * @param levelViewId the level view whose digital entities are attached, for the actions that
     *                    build a logical structure
     * @param fileplanAsRoot for the import of the description: whether the file plan becomes the
     *                    root series; null (in actions submitted earlier) is false
     * @param daoId       for the import of the description of one package: the level of its logical
     *                    structure below which the description is taken; null for the level view or
     *                    the whole package
     */
    public record ConnectParams(Integer nodeId, @Nullable Integer changeId, @Nullable Integer levelViewId,
                                @Nullable Boolean fileplanAsRoot, @Nullable Integer daoId) {

        public ConnectParams(Integer nodeId, @Nullable Integer changeId, @Nullable Integer levelViewId) {
            this(nodeId, changeId, levelViewId, null, null);
        }

        public ConnectParams(Integer nodeId, @Nullable Integer changeId, @Nullable Integer levelViewId,
                             @Nullable Boolean fileplanAsRoot) {
            this(nodeId, changeId, levelViewId, fileplanAsRoot, null);
        }
    }


    /** One AIP that cannot be attached, and why. */
    public record BlockedAip(Integer aipId, String aipCode, String reason) {
    }

    /**
     * What stands in the way of attaching the given AIPs, without attaching anything.
     *
     * Asks {@link DaoLinkPolicy} the same question the attaching itself asks, so what the user is
     * told beforehand and what happens afterwards cannot disagree. Like the check the submission
     * makes, it looks at the links of the whole package only.
     *
     * @param newNode true when the AIPs are to hang on a unit of description that does not exist
     *                yet, where nothing can already be attached
     */
    @Transactional
    public List<BlockedAip> checkConnect(Integer nodeId, List<Integer> aipIds, boolean newNode) {
        List<BlockedAip> blocked = new ArrayList<>();
        for (DaAip aip : aipRepository.findAllById(aipIds)) {
            List<ArrDaLink> liveLinks =
                    daLinkRepository.findByAip_AipIdAndDaDaoIsNullAndDeleteChangeIsNull(aip.getAipId());
            boolean refused = newNode
                    ? daoLinkPolicy.wouldRefuseANewNode(liveLinks, aip.getDigitalRepository())
                    : daoLinkPolicy.wouldBeRefused(liveLinks, nodeId, aip.getDigitalRepository());
            if (refused) {
                blocked.add(new BlockedAip(aip.getAipId(), aip.getCode(),
                        "AIP je již připojen k jiné jednotce popisu a úložiště neumožňuje více vazeb."));
            }
        }
        return blocked;
    }
    /**
     * Refuses the whole request when any of the AIPs cannot be attached where it is asked to go.
     *
     * Only the links of the whole package are looked at. Finding the links of its parts means
     * walking the digital entities of every AIP, which is the per-AIP work these actions exist to
     * take off the request; a conflict on a part is reported on that AIP when its step reaches it.
     */
    private void checkAllCanBeAttached(List<DaAip> aipList, @Nullable Integer targetNodeId) {
        for (DaAip aip : aipList) {
            List<ArrDaLink> liveLinks =
                    daLinkRepository.findByAip_AipIdAndDaDaoIsNullAndDeleteChangeIsNull(aip.getAipId());
            boolean refused = targetNodeId == null
                    ? daoLinkPolicy.wouldRefuseANewNode(liveLinks, aip.getDigitalRepository())
                    : daoLinkPolicy.wouldBeRefused(liveLinks, targetNodeId, aip.getDigitalRepository());
            if (refused) {
                throw new BusinessException("AIP " + aip.getCode()
                        + " je již připojen k jiné jednotce popisu; opakované napojení není povoleno.",
                        ArrangementCode.DAO_ALREADY_LINKED).level(Level.WARNING);
            }
        }
    }

    /**
     * Opens a connect action and queues one step per AIP.
     *
     * What is bounded stays in the request - checking the AIPs and building the unit of description
     * to attach to - and what grows with the number of AIPs is carried out one AIP at a time,
     * afterwards, so the request is answered without waiting for it.
     */
    private DaAipAction submitConnect(DaAipActionType actionType, List<Integer> aipIds, ConnectParams params) {
        List<DaAip> aipList = aipRepository.findAllById(aipIds);
        DaAipAction action = actionService.start(actionType, aipList, writeParams(params));
        actionService.enqueueSteps(action.getAipActionId());
        return action;
    }

    private String writeParams(ConnectParams params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new SystemException("Zadání akce nad AIPy se nepodařilo uložit", e, BaseCode.INVALID_STATE);
        }
    }

    public ConnectParams readConnectParams(String params) {
        try {
            return objectMapper.readValue(params, ConnectParams.class);
        } catch (JsonProcessingException e) {
            throw new SystemException("Zadání akce nad AIPy se nepodařilo přečíst", e, BaseCode.INVALID_STATE);
        }
    }

    /** Attaches whole packages to an existing unit of description. */
    public DaAipAction submitBulkConnectToJP(Integer nodeId, List<Integer> aipIds) {
        inTransaction(() -> {
            nodeRepository.getOneCheckExist(nodeId);
            checkAllCanBeAttached(aipRepository.findAllById(aipIds), nodeId);
            return null;
        });
        return inTransaction(() -> submitConnect(DaAipActionType.CONNECT_TO_NODE, aipIds,
                                                 new ConnectParams(nodeId, null, null)));
    }

    /**
     * Imports the description the packages carry into the archival description below a unit of
     * description, one package after another - a later package finds the levels an earlier one
     * created, so the packages of one file plan share its groups.
     *
     * The packages are imported in the background, where nobody is logged in, so the permission
     * to arrange the fund is checked here. Whether a package can be imported is decided for each
     * package when it is imported, so one that cannot does not stop the others.
     */
    public DaAipAction submitImportDescription(Integer nodeId, List<Integer> aipIds, @Nullable Integer levelViewId,
                                               @Nullable Integer daoId, boolean fileplanAsRoot) {
        return submitImport(DaAipActionType.IMPORT_DESCRIPTION, nodeId, aipIds, levelViewId, daoId, fileplanAsRoot);
    }

    /**
     * Creates, below a unit of description, a level for each level of the logical structure
     * directly below the given one (or the top of the packages) and attaches to it its part of the
     * packages. With the rules of the fund the levels get their items from the EAD; without them
     * they carry only the attached parts.
     */
    public DaAipAction submitCreateSublevels(Integer nodeId, List<Integer> aipIds, @Nullable Integer levelViewId,
                                             @Nullable Integer daoId) {
        return submitImport(DaAipActionType.CREATE_SUBLEVELS, nodeId, aipIds, levelViewId, daoId, false);
    }

    private DaAipAction submitImport(DaAipActionType actionType, Integer nodeId, List<Integer> aipIds,
                                     @Nullable Integer levelViewId, @Nullable Integer daoId, boolean fileplanAsRoot) {
        inTransaction(() -> {
            checkArrPermission(nodeRepository.getOneCheckExist(nodeId));
            return null;
        });
        return inTransaction(() -> submitConnect(actionType, aipIds,
                                                 new ConnectParams(nodeId, null, levelViewId, fileplanAsRoot, daoId)));
    }

    /** Creates a unit of description per package and attaches the package there. */
    public DaAipAction submitBulkCreateFromSelected(Integer nodeId, List<Integer> aipIds) {
        inTransaction(() -> {
            nodeRepository.getOneCheckExist(nodeId);
            checkAllCanBeAttached(aipRepository.findAllById(aipIds), null);
            return null;
        });
        return inTransaction(() -> submitConnect(DaAipActionType.CREATE_NODES, aipIds,
                                                 new ConnectParams(nodeId, null, null)));
    }

    /**
     * Attaches one level of the logical structure of the packages to an existing unit of
     * description; the levels below it are attached with it. Nothing is created.
     */
    public DaAipAction submitBulkConnectLogicalStructure(Integer nodeId, List<Integer> aipIds, Integer levelViewId) {
        inTransaction(() -> {
            checkArrPermission(nodeRepository.getOneCheckExist(nodeId));
            daLevelViewRepository.findById(levelViewId).orElseThrow(
                    () -> new ObjectNotFoundException("Nebylo nalezeno level view s předaným ID. ID=" + levelViewId,
                                                      BaseCode.ID_NOT_EXIST));
            return null;
        });
        return inTransaction(() -> submitConnect(DaAipActionType.CONNECT_LOGICAL_STRUCTURE, aipIds,
                                                 new ConnectParams(nodeId, null, levelViewId)));
    }

    /** Creates a unit of description, builds the logical structure under it and attaches the packages. */
    public DaAipAction submitBulkCreateFromSelectedToJP(Integer nodeId, List<Integer> aipIds, Integer levelViewId) {
        return submitLogicalStructure(DaAipActionType.CREATE_NODES_AND_CONNECT, nodeId, aipIds, levelViewId, true);
    }

    /**
     * The two actions that build a logical structure differ only in whether a unit of description is
     * created for it first; what they then do to each AIP is the same.
     */
    private DaAipAction submitLogicalStructure(DaAipActionType actionType, Integer nodeId, List<Integer> aipIds,
                                               Integer levelViewId, boolean createOwnNode) {
        ConnectParams params = inTransaction(() -> {
            ArrNode arrNode = nodeRepository.getOneCheckExist(nodeId);
            checkAllCanBeAttached(aipRepository.findAllById(aipIds), null);

            DaLevelView levelView = daLevelViewRepository.findById(levelViewId).orElseThrow(
                    () -> new ObjectNotFoundException("Nebylo nalezeno level view s předaným ID. ID=" + levelViewId,
                                                      BaseCode.ID_NOT_EXIST));
            ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);

            ArrNode parent = createOwnNode ? createChildNode(arrNode, change) : arrNode;
            int position = createOwnNode ? 1 : positionUnder(arrNode) + 1;
            ArrNode nodeToConnect = arrNode;
            for (DaLevelView child : levelView.getChildren()) {
                nodeToConnect = createNextLevel(parent, change, position, child);
            }
            return new ConnectParams(nodeToConnect.getNodeId(), change.getChangeId(),
                                     deepestLevelView(levelView).getLevelViewId());
        });
        return inTransaction(() -> submitConnect(actionType, aipIds, params));
    }

    private int positionUnder(ArrNode arrNode) {
        Integer maxPosition = levelRepository.findMaxPositionUnderParent(arrNode);
        return maxPosition == null ? 0 : maxPosition;
    }

    private static DaLevelView deepestLevelView(DaLevelView levelView) {
        DaLevelView deepest = levelView;
        while (deepest.getChildren() != null && !deepest.getChildren().isEmpty()) {
            deepest = deepest.getChildren().get(0);
        }
        return deepest;
    }

    /**
     * Carries out a connect action for one AIP. Called from the worker, one AIP per transaction.
     */
    @Transactional
    public void connectOneAip(DaAipActionType actionType, Integer aipId, ConnectParams params) {
        DaAip daAip = findAipById(aipId);
        ArrNode arrNode = nodeRepository.getOneCheckExist(params.nodeId());
        switch (actionType) {
            case CONNECT_TO_NODE -> {
                ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
                linkToNode(daAip, null, arrNode, ArrDaoLink.LinkType.AIP, change);
            }
            case CREATE_NODES -> {
                ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);
                ArrNode newNode = createChildNode(arrNode, change);
                linkToNode(daAip, null, newNode, ArrDaoLink.LinkType.AIP, change);
            }
            case CONNECT_LOGICAL_STRUCTURE, CREATE_NODES_AND_CONNECT ->
                    connectLogicalDaos(daAip, arrNode, params);
            default -> throw new SystemException("Typ akce " + actionType + " není napojení",
                                                 BaseCode.INVALID_STATE);
        }
    }

    /**
     * Attaches the level of the logical structure of one AIP that belongs to the level view of the
     * action. The levels below it are attached with it.
     */
    private void connectLogicalDaos(DaAip daAip, ArrNode nodeToConnect, ConnectParams params) {
        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.CREATE_DAO_LINK, nodeToConnect);
        DaLevelView levelViewToConnect = daLevelViewRepository.findById(params.levelViewId()).orElseThrow(
                () -> new ObjectNotFoundException("Nebylo nalezeno level view s předaným ID. ID="
                        + params.levelViewId(), BaseCode.ID_NOT_EXIST));

        List<DaDao> daoList = daoRepository.findAllByLevelViewInAndDeleteChangeIsNull(
                Collections.singletonList(levelViewToConnect));
        boolean linked = false;
        for (DaDao daDao : daoList) {
            if (daDao.getType() == DaDao.DaoType.LOGICAL && daDao.getAip().equals(daAip)) {
                linkToNode(daAip, daDao, nodeToConnect, ArrDaoLink.LinkType.PART_AIP, change);
                linked = true;
            }
        }
        if (!linked) {
            throw new BusinessException("AIP " + daAip.getCode() + " vybranou úroveň logické struktury neobsahuje.",
                                        BaseCode.INVALID_STATE);
        }
    }
    private ArrNode createNextLevel(ArrNode arrNode, ArrChange change, int position, DaLevelView levelView) {
        ArrNode newNode = createChildNode(arrNode, change, position);
        for (DaLevelView child : levelView.getChildren()) {
            newNode = createNextLevel(newNode, change, 1, child);
        }
        return newNode;
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
        checkArrPermission(arrDaoLink.getNode());

        ArrChange change = arrangementInternalService.createChange(ArrChange.Type.DELETE_DAO_LINK, arrDaoLink.getNode());
        arrDaoLink.setDeleteChange(change);
        daoLinkRepository.save(arrDaoLink);
        if (arrDaoLink instanceof ArrDaLink daLink) {
            linkStateResolver.refreshFor(daLink.getAip());
        }
    }


    @Transactional
    public ResponseEntity<Resource> getComponent(Integer fileId) {
        DaDaoFile daoFile = daoFileRepository.findById(fileId).orElseThrow(() -> new IllegalStateException("Nenalezen soubor s id " + fileId));
        DaAip aip = daoFile.getDao().getAip();
        DaLocalCache localCache = daLocalCacheRepository.findByAipAndQueueItemStatesIn(aip, getQueueImportStates());

        try {
            Path zip = Paths.get(localCache.getFilePath());

            // the stored name is the original name of the file when PREMIS gives one
            String storedName = daoFile.getFileName().replace(File.separator, "/");
            String fileName = storedName.substring(storedName.lastIndexOf('/') + 1);

            // Only the requested entry leaves the package; the content is spooled to a temporary
            // file when large and released once the response body is written.
            SpooledContent content;
            try (ZipFile zipFile = new ZipFile(zip.toFile())) {
                ZipEntry entry = componentEntry(zipFile, daoFile.getDao().getCode());
                try (InputStream in = zipFile.getInputStream(entry)) {
                    content = SpooledContent.readFrom(in);
                }
            }
            InputStreamResource fsr = new InputStreamResource(content.openStreamAndCloseOnEnd());

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

    /**
     * Finds a file of a stored package by the path its root METS gives it. The DAO of the file
     * is coded by the ID the file has in the METS; its stored name may be the original name of
     * the file, which need not be the name in the package, nor unique in it.
     */
    static ZipEntry componentEntry(ZipFile zipFile, String daoCode) throws IOException {
        ZipEntry metsEntry = zipFile.stream()
                .filter(e -> !e.isDirectory() && isRootMets(e.getName()))
                .findFirst()
                .orElseThrow(() -> AipProblemException.metadata("Balíček neobsahuje soubor " + AipPackageFiles.METS));
        String root = metsEntry.getName().substring(0, metsEntry.getName().length() - AipPackageFiles.METS.length());
        MetsType mets;
        try (InputStream in = zipFile.getInputStream(metsEntry)) {
            mets = MetsReaderWriter.unmarshal(in);
        } catch (JAXBException e) {
            throw AipProblemException.metadata("Soubor METS.xml balíčku se nepodařilo přečíst: " + AipProblem.reason(e),
                                               AipPackageFiles.METS, e);
        }
        String href = AipPackageFiles.hrefOf(mets, daoCode);
        if (href == null) {
            throw AipProblemException.metadata("METS.xml balíčku neobsahuje soubor " + daoCode);
        }
        String relative = href.startsWith("./") ? href.substring(2) : href;
        ZipEntry entry = zipFile.getEntry(root + relative);
        String decoded = AipPackageFiles.decodedHref(relative);
        if (entry == null && decoded != null) {
            entry = zipFile.getEntry(root + decoded);
        }
        if (entry == null || entry.isDirectory()) {
            throw AipProblemException.metadata("Balíček neobsahuje soubor " + href + ", na který odkazuje METS.xml",
                                               href, null);
        }
        return entry;
    }

    /** The root METS lies at the top of the package directory, which a stored package keeps. */
    static boolean isRootMets(String entryName) {
        if (!entryName.endsWith(AipPackageFiles.METS)) {
            return false;
        }
        String dir = entryName.substring(0, entryName.length() - AipPackageFiles.METS.length());
        return dir.isEmpty() || (dir.length() > 1 && dir.indexOf('/') == dir.length() - 1);
    }

    /**
     * Lists the files of the package downloaded for the AIP, as it arrived from the digital
     * archive. Serves the inspection of a package whose processing failed, so it must not
     * depend on anything the processing produces.
     *
     * @throws ObjectNotFoundException when no package is stored for the AIP
     */
    @Transactional
    public List<AipPackageEntry> getPackageEntries(Integer aipId) {
        Path zip = getPackagePath(aipId);
        List<AipPackageEntry> entries = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            zipFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .sorted(Comparator.comparing(ZipEntry::getName))
                    .forEach(entry -> {
                        AipPackageEntry vo = new AipPackageEntry();
                        vo.setPath(entry.getName());
                        vo.setSize(entry.getSize() < 0 ? 0L : entry.getSize());
                        entries.add(vo);
                    });
        } catch (IOException e) {
            throw new SystemException("Nepodařilo se přečíst balíček AIP=" + aipId, e, BaseCode.INVALID_STATE);
        }
        return entries;
    }

    /**
     * Sends the downloaded package of the AIP as it arrived from the digital archive, so it
     * can be examined outside ELZA - with the tools of the archive that produced it, which is
     * what a package ELZA cannot process usually calls for.
     *
     * @throws ObjectNotFoundException when no package is stored for the AIP
     */
    @Transactional
    public ResponseEntity<Resource> getPackage(Integer aipId) {
        Path zip = getPackagePath(aipId);
        DaAip aip = findAipById(aipId);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + aip.getCode() + ".zip\"");
        return new ResponseEntity<>(new FileSystemResource(zip), headers, HttpStatus.OK);
    }

    /**
     * Reads one file out of the downloaded package; the caller closes the returned content.
     */
    @Transactional
    public SpooledContent getPackageEntry(Integer aipId, String path) {
        Path zip = getPackagePath(aipId);
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            ZipEntry entry = zipFile.getEntry(path);
            if (entry == null || entry.isDirectory()) {
                throw new ObjectNotFoundException("Balíček AIP=" + aipId + " neobsahuje soubor " + path,
                        AIP_NOT_FOUND);
            }
            try (InputStream in = zipFile.getInputStream(entry)) {
                return SpooledContent.readFrom(in);
            }
        } catch (IOException e) {
            throw new SystemException("Nepodařilo se přečíst soubor " + path + " balíčku AIP=" + aipId, e,
                    BaseCode.INVALID_STATE);
        }
    }

    private Path getPackagePath(Integer aipId) {
        DaAip aip = findAipById(aipId);
        DaLocalCache localCache = daLocalCacheRepository.findByAipAndQueueItemStatesIn(aip, getQueueImportStates());
        if (localCache == null || localCache.getFilePath() == null) {
            throw new ObjectNotFoundException("Pro AIP=" + aipId + " není stažený žádný balíček", AIP_NOT_FOUND);
        }
        Path zip = Paths.get(localCache.getFilePath());
        if (!Files.isRegularFile(zip)) {
            throw new ObjectNotFoundException("Balíček AIP=" + aipId + " není v lokální cache", AIP_NOT_FOUND);
        }
        return zip;
    }

    public DaoLinksResult getDaoLinks(Integer nodeId) {
        List<DaoLink> daoLinkList = new ArrayList<>();
        List<ArrDaLink> arrDaoLinks = daLinkRepository.findByNodeIdAndDeleteChangeIsNullFetchAip(nodeId);

        List<DaAip> aipList = arrDaoLinks.stream()
                .filter(d -> d.getLinkType() == ArrDaoLink.LinkType.AIP || d.getLinkType() == ArrDaoLink.LinkType.PART_AIP)
                .map(ArrDaLink::getAip)
                .toList();

        List<ArrDaLink> aipDaoLinks = arrDaoLinks.stream().filter(d -> d.getLinkType() == ArrDaoLink.LinkType.AIP).toList();
        List<ArrDaLink> partDaoLinks = arrDaoLinks.stream().filter(d -> d.getLinkType() == ArrDaoLink.LinkType.PART_AIP).toList();
        List<ArrDaLink> componentDaoLinks = arrDaoLinks.stream().filter(d -> d.getLinkType() == ArrDaoLink.LinkType.COMPONENT_AIP).collect(Collectors.toList());

        Map<Integer, Map<Integer, List<DaDao>>> aipDaoMap = daoRelationRepository.findByAipsAndDeleteChangeIsNull(aipList).stream()
                .collect(Collectors.groupingBy(r -> r.getParentDao().getAip().getAipId(),
                        Collectors.groupingBy(r -> r.getParentDao().getDaoId(), Collectors.mapping(DaDaoRelation::getDao, Collectors.toList()))));

        Map<Integer, List<DaDao>> aipParentDaoMap = daoRelationRepository.findParentDaosByAipsAndDeleteChangeIsNull(aipList).stream()
                .sorted(Comparator.comparing(DaDao::getType))
                .collect(Collectors.groupingBy(r -> r.getAip().getAipId()));

        List<DaoLink> resultPartDaoLinks = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(partDaoLinks)) {
            for (ArrDaLink partDaoLink : partDaoLinks) {
                Integer aipId = partDaoLink.getAip().getAipId();
                Map<Integer, List<DaDao>> daoMap = aipDaoMap.getOrDefault(aipId, Map.of());
                Map<Integer, ArrDaLink> daoLinkMap = componentDaoLinks.stream()
                        .filter(d -> d.getAip().getAipId().equals(aipId))
                        .collect(Collectors.toMap(d -> d.getDaDao().getDaoId(), d -> d));

                resultPartDaoLinks.add(createPartAipDaoLink(partDaoLink, daoLinkMap, daoMap));
            }
        }

        if (CollectionUtils.isNotEmpty(aipDaoLinks)) {
            for (ArrDaLink aipDaoLink : aipDaoLinks) {
                Integer aipId = aipDaoLink.getAip().getAipId();
                Map<Integer, List<DaDao>> daoMap = aipDaoMap.getOrDefault(aipId, Map.of());
                Map<Integer, ArrDaLink> daoLinkMap = componentDaoLinks.stream()
                        .filter(d -> d.getAip().getAipId().equals(aipId))
                        .collect(Collectors.toMap(d -> d.getDaDao().getDaoId(), d -> d));
                // an AIP linked before its metadata arrived has no structure yet, only the link itself
                List<DaDao> parentDaoList = aipParentDaoMap.getOrDefault(aipId, List.of());

                daoLinkList.add(createAipDaoLink(aipId, aipDaoLink, daoLinkMap, daoMap, parentDaoList));
            }
        }

        daoLinkList.addAll(resultPartDaoLinks);

        if (CollectionUtils.isNotEmpty(componentDaoLinks)) {
            for (ArrDaLink componentDaoLink : componentDaoLinks) {
                daoLinkList.add(createDaoLink(componentDaoLink, false));
            }
        }

        DaoLinksResult daoLinksResult = new DaoLinksResult();
        daoLinksResult.setItems(daoLinkList);
        return daoLinksResult;
    }

    private DaoLink createPartAipDaoLink(ArrDaLink partDaoLink, Map<Integer, ArrDaLink> daoLinkMap, Map<Integer, List<DaDao>> daoMap) {
        DaoLink daoLink = createDaoLink(partDaoLink, partDaoLink.getDaDao().getType() == DaDao.DaoType.LOGICAL);

        processDao(partDaoLink.getDaDao(), daoLink, daoLinkMap, daoMap, null, new HashMap<>());

        return daoLink;
    }

    private DaoLink createAipDaoLink(Integer aipId, ArrDaLink aipDaoLink, Map<Integer, ArrDaLink> daoLinkMap, Map<Integer, List<DaDao>> daoMap, List<DaDao> parentDaoList) {
        DaoLink daoLink = aipDaoLink == null ? createDaoLink(aipId) : createDaoLink(aipDaoLink, false);

        Map<Integer, DaoLink> childrenMap = new HashMap<>();

        for (DaDao parentDao : parentDaoList) {
            String path = parentDao.getLabel() + " / ";
            processDao(parentDao, daoLink, daoLinkMap, daoMap, path, childrenMap);
        }

        return daoLink;
    }

    private void processDao(DaDao parentDao,
                            DaoLink parentDaoLink,
                            Map<Integer, ArrDaLink> daoLinkMap,
                            Map<Integer, List<DaDao>> daoMap,
                            @Nullable String path,
                            Map<Integer, DaoLink> childrenMap) {
        List<DaDao> daoList = daoMap.getOrDefault(parentDao.getDaoId(), new ArrayList<>());

        for (DaDao dao : daoList) {
            if (dao.getType() == DaDao.DaoType.FILE
                    || dao.getType() == DaDao.DaoType.METAAMD
                    || dao.getType() == DaDao.DaoType.METADMDINHERENT
                    || dao.getType() == DaDao.DaoType.METADMDCONTEXTUAL) {
                boolean processed = childrenMap.get(dao.getDaoId()) != null;
                if (parentDao.getType() == DaDao.DaoType.REPRESENTATION && processed) {
                    continue;
                }

                if (parentDaoLink.getChildrenCount() < 100) {
                    ArrDaLink arrDaoLink = daoLinkMap.getOrDefault(dao.getDaoId(), null);
                    boolean logical = parentDao.getType() == DaDao.DaoType.LOGICAL;
                    DaoLink daoLink = arrDaoLink == null ? createDaoLink(dao, logical) : createDaoLink(arrDaoLink, logical);
                    daoLink.setPath(path);
                    parentDaoLink.addChildrenItem(daoLink);
                    childrenMap.put(dao.getDaoId(), daoLink);
                }
                if (!processed) {
                    parentDaoLink.setChildrenCount(parentDaoLink.getChildrenCount() + 1);
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                if (path != null) {
                    stringBuilder.append(path);
                }
                stringBuilder.append(dao.getLabel())
                        .append(" / ");
                String newPath = stringBuilder.toString();

                processDao(dao, parentDaoLink, daoLinkMap, daoMap, newPath, childrenMap);
            }
        }
    }

    private DaoLink createDaoLink(Integer aipId) {

        DaoLink daoLink = new DaoLink();
        daoLink.setDaoLinkUuid(UUID.nameUUIDFromBytes(("AIP" + aipId).getBytes()).toString());
        daoLink.setAipId(aipId);
        daoLink.setName(aipId.toString());
        daoLink.setChildrenCount(0);

        return daoLink;
    }

    private DaoLink createDaoLink(DaDao dao, boolean logical) {
        DaoLink daoLink = new DaoLink();
        daoLink.setDaoLinkUuid(UUID.nameUUIDFromBytes(("DAO" + dao.getDaoId()).getBytes()).toString());
        daoLink.setAipId(dao.getAip().getAipId());

        daoLink.setDaoId(dao.getDaoId());
        daoLink.setDaoCode(logical ? dao.getCode() + "logical" : dao.getCode());
        daoLink.setDaoType(DaDaoType.fromValue(dao.getType().name()));
        daoLink.setName(dao.getLabel());
        daoLink.setChildrenCount(0);

        return daoLink;
    }

    private DaoLink createDaoLink(ArrDaLink arrDaoLink, boolean logical) {
        DaDao dao = arrDaoLink.getDaDao();

        DaoLink daoLink = new DaoLink();
        daoLink.setDaoLinkUuid(UUID.nameUUIDFromBytes(arrDaoLink.getDaoLinkId().toString().getBytes()).toString());
        daoLink.setDaoLinkId(arrDaoLink.getDaoLinkId());
        daoLink.setAipId(arrDaoLink.getAip().getAipId());
        daoLink.setChildrenCount(0);

        if (dao != null) {
            daoLink.setDaoId(dao.getDaoId());
            daoLink.setDaoCode(logical ? dao.getCode() + "logical" : dao.getCode());
            daoLink.setDaoType(DaDaoType.fromValue(dao.getType().name()));
            daoLink.setName(dao.getLabel());
        } else {
            daoLink.setName(arrDaoLink.getAip().getAipId().toString());
        }

        return daoLink;
    }
}
