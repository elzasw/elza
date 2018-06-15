package cz.tacr.elza.dataexchange.input.parties.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.core.data.PartyTypeCmplTypes;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.context.ImportPhaseChangeListener;
import cz.tacr.elza.dataexchange.input.context.ObservableImport;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.party.ApConvResult;

/**
 * Context for data exchange parties.
 */
public class PartiesContext {

    private final Map<String, PartyInfo> importIdPartyInfoMap = new HashMap<>();

    private final StorageManager storageManager;

    private final int batchSize;

    private final AccessPointsContext apContext;

    private final StaticDataProvider staticData;

    private final GroovyScriptService gsService;

    private final Map<PartyType, List<PartyWrapper>> typeGroupedPartyQueue = new EnumMap<>(PartyType.class);

    private final List<PartyUnitDateWrapper> unitDateQueue = new ArrayList<>();

    private final List<PartyGroupIdentifierWrapper> groupIdentifierQueue = new ArrayList<>();

    private final List<PartyNameWrapper> nameQueue = new ArrayList<>();

    private final List<PartyNameCmplWrapper> nameCmplQueue = new ArrayList<>();

    private final List<PartyPreferredNameWrapper> prefNameQueue = new ArrayList<>();

    private long scriptMemoryScore;

    public PartiesContext(StorageManager storageManager, int batchSize, AccessPointsContext apContext,
            StaticDataProvider staticData, ImportInitHelper initHelper) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.apContext = apContext;
        this.staticData = staticData;
        this.gsService = initHelper.getGroovyScriptService();
    }

    public void init(ObservableImport observableImport) {
        observableImport.registerPhaseChangeListener(new PartiesRelatedPhaseChangeListener());
    }

    public Collection<PartyInfo> getAllPartyInfo() {
        return Collections.unmodifiableCollection(importIdPartyInfoMap.values());
    }

    public PartyInfo getPartyInfo(String importId) {
        return importIdPartyInfoMap.get(importId);
    }

    public PartyInfo addParty(ParParty entity, String importId, AccessPointInfo apInfo, PartyType partyType) {
        PartyInfo info = new PartyInfo(apInfo, partyType, this);
        if (importIdPartyInfoMap.putIfAbsent(importId, info) != null) {
            throw new DEImportException("Party has duplicate id, partyId:" + importId);
        }
        List<PartyWrapper> partyQueue = typeGroupedPartyQueue.get(partyType);
        if (partyQueue == null) {
            partyQueue = new ArrayList<>();
            typeGroupedPartyQueue.put(partyType, partyQueue);
        }
        partyQueue.add(new PartyWrapper(entity, info));
        info.onEntityQueued();
        if (partyQueue.size() >= batchSize) {
            storeParties(partyQueue);
        }
        return info;
    }

    public PartyNameWrapper addName(ParPartyName entity, PartyInfo partyInfo, boolean preferred) {
        PartyNameWrapper wrapper = new PartyNameWrapper(entity, partyInfo);
        nameQueue.add(wrapper);
        partyInfo.onEntityQueued();
        if (nameQueue.size() >= batchSize) {
            storeNames();
        }
        if (preferred) {
            prefNameQueue.add(new PartyPreferredNameWrapper(partyInfo, wrapper.getIdHolder()));
            partyInfo.onEntityQueued();
            if (prefNameQueue.size() >= batchSize) {
                storePreferredNames();
            }
        }
        return wrapper;
    }

    public EntityIdHolder<ParUnitdate> addUnitDate(ParUnitdate entity, PartyInfo partyInfo) {
        PartyUnitDateWrapper wrapper = new PartyUnitDateWrapper(entity, partyInfo);
        unitDateQueue.add(wrapper);
        partyInfo.onEntityQueued();
        if (unitDateQueue.size() >= batchSize) {
            storeUnitDates();
        }
        return wrapper.getIdHolder();
    }

    public void addNameComplement(ParPartyNameComplement entity, EntityIdHolder<ParPartyName> nameIdHolder,
            PartyInfo partyInfo) {
        nameCmplQueue.add(new PartyNameCmplWrapper(entity, nameIdHolder, partyInfo));
        partyInfo.onEntityQueued();
        if (nameCmplQueue.size() >= batchSize) {
            storeNameComplements();
        }
    }

    public PartyGroupIdentifierWrapper addIdentifier(ParPartyGroupIdentifier entity, PartyInfo partyInfo) {
        PartyGroupIdentifierWrapper wrapper = new PartyGroupIdentifierWrapper(entity, partyInfo);
        groupIdentifierQueue.add(wrapper);
        partyInfo.onEntityQueued();
        if (groupIdentifierQueue.size() >= batchSize) {
            storeGroupIdentifiers();
        }
        return wrapper;
    }

    public void onPartyFinished(PartyInfo partyInfo) {
        updatePartyAp(partyInfo);
        // clear all entities potentially loaded by groovy script
        scriptMemoryScore += partyInfo.getMaxMemoryScore();
        if (scriptMemoryScore > storageManager.getAvailableMemoryScore()) {
            storageManager.flushAndClear(true);
            scriptMemoryScore = 0;
        }
        // AP is now processed
        partyInfo.getApInfo().onProcessed();
    }

    private void updatePartyAp(PartyInfo partyInfo) {
        ParParty entity = partyInfo.getEntityRef(storageManager.getSession());
        // get supported complement types
        String partyTypeCode = partyInfo.getPartyType().getCode();
        PartyTypeCmplTypes cmplTypes = staticData.getCmplTypesByPartyTypeCode(partyTypeCode);
        // execute groovy script
        ApConvResult convResult = gsService.convertPartyToAp(entity, cmplTypes.getTypes());
        // add AP description and names
        AccessPointInfo apInfo = partyInfo.getApInfo();
        ApDescription apDesc = convResult.createDesc(apContext.getCreateChange());
        apContext.addDescription(apDesc, apInfo);
        convResult.createNames(apContext.getCreateChange(), n -> apContext.addName(n, apInfo));
    }

    public void storeAll() {
        storeParties();
        storeUnitDates();
        storeGroupIdentifiers();
        storeNames();
        storeNameComplements();
        storePreferredNames();
    }

    private void storeParties() {
        typeGroupedPartyQueue.values().forEach(this::storeParties);
    }

    private void storeParties(Collection<PartyWrapper> partyQueue) {
        if (partyQueue.isEmpty()) {
            return;
        }
        apContext.storeAccessPoints();
        storageManager.saveParties(partyQueue);
        partyQueue.clear();
    }

    private void storeUnitDates() {
        if (unitDateQueue.isEmpty()) {
            return;
        }
        storageManager.saveGeneric(unitDateQueue);
        unitDateQueue.clear();
    }

    private void storeGroupIdentifiers() {
        List<PartyWrapper> partyGroupQueue = typeGroupedPartyQueue.get(PartyType.GROUP_PARTY);
        if (partyGroupQueue != null) {
            storeParties(partyGroupQueue);
        }
        storeUnitDates();
        storageManager.saveGeneric(groupIdentifierQueue);
        groupIdentifierQueue.clear();
    }

    private void storeNames() {
        if (nameQueue.isEmpty()) {
            return;
        }
        storeParties();
        storeUnitDates();
        storageManager.saveGeneric(nameQueue);
        nameQueue.clear();
    }

    private void storeNameComplements() {
        storeNames();
        storageManager.saveGeneric(nameCmplQueue);
        nameCmplQueue.clear();
    }

    private void storePreferredNames() {
        storeNames();
        storageManager.saveGeneric(prefNameQueue);
        prefNameQueue.clear();
    }

    /**
     * Listens for parties related phases.
     */
    private static class PartiesRelatedPhaseChangeListener implements ImportPhaseChangeListener {

        @Override
        public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
            boolean lastPartyModifiablePhase = ImportPhase.RELATIONS.isSubsequent(nextPhase);
            if (lastPartyModifiablePhase || previousPhase == ImportPhase.PARTIES) {
                // store remaining APs and parties
                context.getAccessPoints().storeAll();
                context.getParties().storeAll();
            }
            return true;
        }
    }
}
