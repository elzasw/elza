package cz.tacr.elza.dataexchange.input.parts.context;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.ObjectIdHolder;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.*;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.party.ApConvResult;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PartsContext {

    private static final Logger logger = LoggerFactory.getLogger(PartsContext.class);

    private final Map<String, PartInfo> importIdPartInfoMap = new HashMap<>();

    private final StorageManager storageManager;

    private final int batchSize;

    private final AccessPointsContext apContext;

    private final ObjectIdHolder objectIdHolder;

    private final StaticDataProvider staticData;

    private final GroovyScriptService gsService;

    private Integer currentImportId;

    private final List<PartWrapper> partQueue = new ArrayList<>();

    private final List<PrefferedPartWrapper> prefferedPartQueue = new ArrayList<>();

    private Set<AccessPointInfo> prefferedPartApInfos = new HashSet<>();

    public PartsContext(StorageManager storageManager, int batchSize, AccessPointsContext apContext,
                        StaticDataProvider staticData, ImportInitHelper initHelper, ObjectIdHolder objectIdHolder) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.apContext = apContext;
        this.staticData = staticData;
        this.gsService = initHelper.getGroovyScriptService();
        this.objectIdHolder = objectIdHolder;
        currentImportId = 0;
    }

    public void init(ObservableImport observableImport) {
        observableImport.registerPhaseChangeListener(new PartsPhaseEndListener());
    }

    public Collection<PartInfo> getAllPartInfo() {
        return Collections.unmodifiableCollection(importIdPartInfoMap.values());
    }

    public PartInfo getPartInfo(String importId) {
        return importIdPartInfoMap.get(importId);
    }

    public Integer getCurrentImportId() {
        return currentImportId++;
    }

    public PartInfo addPart(ApPart entity, String importId, AccessPointInfo apInfo, RulPartType partType, List<ItemWrapper> itemWrapperList) {
        if (logger.isDebugEnabled()) {
            logger.debug("Add part to the context, importID = {}", importId);
        }
        PartInfo info = new PartInfo(importId, apInfo, partType, this);
        if (importIdPartInfoMap.putIfAbsent(importId, info) != null) {
            throw new DEImportException("Part has duplicate id, partId:" + importId);
        }

        PartWrapper partWrapper = new PartWrapper(entity, info, itemWrapperList);
        partQueue.add(partWrapper);
        info.onEntityQueued();
        if (partQueue.size() >= batchSize) {
            storeParts(true);
        }

        RulPartType defaultPartType = staticData.getDefaultPartType();
        if (!prefferedPartApInfos.contains(apInfo) && info.getRulPartType().getCode().equals(defaultPartType.getCode())) {
            PrefferedPartWrapper prefferedPartWrapper = new PrefferedPartWrapper(apInfo, info);
            prefferedPartQueue.add(prefferedPartWrapper);
            prefferedPartApInfos.add(apInfo);
            apInfo.onEntityQueued();
        }


        return info;
    }

    public ItemWrapper addItem(ApItem entity, PartInfo partInfo) {
        ItemWrapper wrapper = new ItemWrapper(entity, partInfo);
        return wrapper;
    }

    public void storeAll() {
        apContext.storeAccessPoints();
        storeParts(false);
        storePrefferedParts(false);

    }

    public void prepareAps() {
        for (PartInfo info : importIdPartInfoMap.values()) {
            updatePartAp(info);
            info.getApInfo().onProcessed();
        }
    }

    private void storeParts(boolean storeReferenced) {
        if(storeReferenced) {
            apContext.storeAccessPoints();
        }
        storageManager.storeParts(partQueue);
        for (PartWrapper wrapper : partQueue) {
            wrapper.getPartInfo().setEntityId(wrapper.getEntity().getPartId());
        }
        partQueue.clear();
    }

    private void storePrefferedParts(boolean storeReferenced) {
        if (storeReferenced) {
            storeParts(true);
        }
        storageManager.storeRefUpdates(prefferedPartQueue);
        prefferedPartQueue.clear();
    }

    public ObjectIdHolder getObjectIdHolder() {
        return objectIdHolder;
    }

    private void updatePartAp(PartInfo partInfo) {
        Session session = storageManager.getSession();
        ApPart entity = partInfo.getEntityRef(session);
        session.refresh(entity);
    }

    public void onPartFinished(PartInfo partInfo) {
        // TODO: remove this and PartyInfo logic for sub-entity tracking
    }


    private static class PartsPhaseEndListener implements ImportPhaseChangeListener {

        @Override
        public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
            if (previousPhase == ImportPhase.PARTS) {
                context.getAccessPoints().storeAll();
                context.getParts().storeAll();

                return false;
            }

            if (ImportPhase.FINISHED.isSubsequent(nextPhase)) {
                // temporary solution
                // TODO: groovy script after commit (outside import)
                context.getParts().prepareAps();
                context.getAccessPoints().storeAll();
                // parties were stored -> unregister listener
                return false;
            }
            return true;
            //return !ImportPhase.PARTS.isSubsequent(nextPhase);
        }
    }
}
