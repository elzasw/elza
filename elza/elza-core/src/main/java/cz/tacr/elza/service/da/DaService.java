package cz.tacr.elza.service.da;

import cz.tacr.da.ApiException;
import cz.tacr.da.controller.vo.DownloadDownloadAips;
import cz.tacr.da.controller.vo.DownloadDownloadStatus;
import cz.tacr.da.controller.vo.RequestState;
import cz.tacr.da.controller.vo.UpdatedAips;
import cz.tacr.da.controller.vo.UpdatedInfo;
import cz.tacr.elza.connector.DaConnector;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoFile;
import cz.tacr.elza.domain.DaDaoFileFolder;
import cz.tacr.elza.domain.DaDaoRelation;
import cz.tacr.elza.domain.DaRemoteRepositorySync;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaDaoFileFolderRepository;
import cz.tacr.elza.repository.DaDaoFileRepository;
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DaRemoteRepositorySyncRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import cz.tacr.elza.service.UserService;
import gov.loc.mets.v1_11.schema.MetsType;
import gov.loc.premis.v3.PremisComplexType;
import jakarta.transaction.Transactional;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DaService {

    private static final Integer DA_UPDATE_PAGE_SIZE = 1000;

    private static final String DIP_TYPE_PACKAGE_INFO = "package_info";

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private UserService userService;
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

    @Transactional
    public void fillDaoStructure(DaAip aip, MetsType metsType, PremisComplexType premisComplexType) {
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


    public String downloadAips(ArrDigitalRepository digitalRepository, List<DaSyncQueueItem> syncQueueItemList) {
        List<String> aipIds = syncQueueItemList.stream()
                .map(DaSyncQueueItem::getCode)
                .toList();

        DownloadDownloadAips downloadDownloadAips = new DownloadDownloadAips();
        downloadDownloadAips.setDipType(DIP_TYPE_PACKAGE_INFO);
        downloadDownloadAips.setAipIds(aipIds);

        return daConnector.downloadAips(digitalRepository, downloadDownloadAips);
    }

    public boolean downloadStatusFinished(ArrDigitalRepository digitalRepository, String batchId) {
        DownloadDownloadStatus status = daConnector.downloadStatus(digitalRepository, batchId);
        return status.getState() == RequestState.FINISHED;
    }

    public byte[] downloadDownload(ArrDigitalRepository digitalRepository, String batchId) throws ApiException {
        return daConnector.downloadDownload(digitalRepository, batchId);
    }

    public byte[] downloadFileTransfer(ArrDigitalRepository digitalRepository, String batchId) {
        try {
            return daConnector.downloadDownload(digitalRepository, batchId); //todo
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
