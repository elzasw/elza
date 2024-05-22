package cz.tacr.elza.service;

import cz.tacr.da.controller.vo.UpdatedAips;
import cz.tacr.da.controller.vo.UpdatedInfo;
import cz.tacr.elza.connector.DaConnector;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaRemoteRepositorySync;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.DaRemoteRepositorySyncRepository;
import cz.tacr.elza.repository.DaSyncQueueItemRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DaService {

    private static final Integer DA_UPDATE_PAGE_SIZE = 1000;

    @Autowired
    private DaConnector daConnector;

    @Autowired
    private DaSyncQueueItemRepository syncQueueItemRepository;

    @Autowired
    private DaRemoteRepositorySyncRepository remoteRepositorySyncRepository;

    @Transactional
    public void synchronizaceDA(ArrDigitalRepository digitalRepository) {
        DaRemoteRepositorySync daRemoteRepositorySync = getDaRemoteRepositorySync(digitalRepository);
        UpdatedAips updatesAips = daConnector.updates(digitalRepository, DA_UPDATE_PAGE_SIZE, daRemoteRepositorySync.getNextQuery());

        if (CollectionUtils.isNotEmpty(updatesAips.getAipIds())) {
            List<DaSyncQueueItem> syncQueueItemList = new ArrayList<>();
            //todo fantis update
            for (UpdatedInfo updatedInfo : updatesAips.getAipIds()) {
                DaSyncQueueItem syncQueueItem = new DaSyncQueueItem();
                syncQueueItem.setCode(updatedInfo.getAipId());
                syncQueueItem.setAipVersion(updatedInfo.getAipVersion());
                syncQueueItem.setState(DaSyncQueueItem.QueueItemState.IMPORT_NEW);
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
    public Iterable<DaSyncQueueItem> getNextItems(int pageSize, DaSyncQueueItem.QueueItemState... states) {
        Pageable pageable = PageRequest.of(0, pageSize);

        //todo fantis vracet pouze itemy, které mají stejný externí systém jako první
        return syncQueueItemRepository.findByStates(Arrays.asList(states), pageable);
    }


}
