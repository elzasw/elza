package cz.tacr.elza.dataexchange.input.parties.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.domain.*;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.party.ApConvResult;

/**
 * Context for data exchange parties.
 */
public class PartiesContext {

    private static final Logger logger = LoggerFactory.getLogger(PartiesContext.class);

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

    private final List<PartyPrefNameWrapper> prefNameQueue = new ArrayList<>();

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
        if (logger.isDebugEnabled()) {
            logger.debug("Add party to the context, importId = {}", importId);
        }
        PartyInfo info = new PartyInfo(importId, apInfo, partyType, this);
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
        if (logger.isDebugEnabled()) {
            logger.debug("Add name to the context, importId = {}, name = {}", partyInfo.getImportId(),
                         entity.getMainPart());
        }
        PartyNameWrapper wrapper = new PartyNameWrapper(entity, partyInfo);
        nameQueue.add(wrapper);
        partyInfo.onEntityQueued();
        if (nameQueue.size() >= batchSize) {
            storeNames(true);
        }
        if (preferred) {
            prefNameQueue.add(new PartyPrefNameWrapper(partyInfo, wrapper.getIdHolder()));
            partyInfo.onEntityQueued();
            if (prefNameQueue.size() >= batchSize) {
                storePreferredNames(true);
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
            storeNameComplements(true);
        }
    }

    public PartyGroupIdentifierWrapper addIdentifier(ParPartyGroupIdentifier entity, PartyInfo partyInfo) {
        PartyGroupIdentifierWrapper wrapper = new PartyGroupIdentifierWrapper(entity, partyInfo);
        groupIdentifierQueue.add(wrapper);
        partyInfo.onEntityQueued();
        if (groupIdentifierQueue.size() >= batchSize) {
            storeGroupIdentifiers(true);
        }
        return wrapper;
    }

    public void onPartyFinished(PartyInfo partyInfo) {
    	// TODO: remove this and PartyInfo logic for sub-entity tracking
    }

    private void updatePartyAp(PartyInfo partyInfo) {
    	// TODO: 
    	// Prepare new async implementation of Party->AP generator
    	
    	// We have to load current party
    	// Party from partyInfo cannot be used because
    	// it could be only partially initialized
    	// 
        
    	// Get entity from party info
    	Session session = storageManager.getSession();
    	ParParty entity = partyInfo.getEntityRef(session);
    	    	
    	// Force to refresh -> get live object
    	session.refresh(entity);    	
    	
        // get supported complement types
        String partyTypeCode = partyInfo.getPartyType().getCode();
        PartyTypeCmplTypes cmplTypes = staticData.getCmplTypesByPartyTypeCode(partyTypeCode);
        // execute groovy script
        // todo[dataexchange]: ApState je null!!!
        ApState state = partyInfo.getApInfo().getState();
        ApConvResult convResult = gsService.convertPartyToAp(entity, state, cmplTypes.getCmplTypes());
        // TODO: clear loaded entities by groovy
        // add converted description and names
        AccessPointInfo apInfo = partyInfo.getApInfo();
        ApDescription apDesc = convResult.createDesc();
        if (apDesc != null) {
            apDesc.setCreateChange(apContext.getCreateChange());
            apContext.addDescription(apDesc, apInfo);
        }
        for (ApName name : convResult.createNames()) {
            name.setCreateChange(apContext.getCreateChange());
            name.setObjectId(apContext.nextNameObjectId());
            apContext.addName(name, apInfo);
        }
    }

    public void storeAll() {
        storeParties();
        storeUnitDates();
        storeGroupIdentifiers(false);
        storeNames(false);
        storeNameComplements(false);
        storePreferredNames(false);
    }
    
    /**
     * Prepare acess points for parties
     * 
     * Method can be called only once !!!
     */
    public void prepareAps() {
    	for(PartyInfo partyInfo : importIdPartyInfoMap.values()) {
            updatePartyAp(partyInfo);
            // AP is now processed
            partyInfo.getApInfo().onProcessed();    		
    	}
    }

    private void storeParties() {
        typeGroupedPartyQueue.values().forEach(this::storeParties);
    }

    private void storeParties(Collection<PartyWrapper> partyQueue) {
        apContext.storeAccessPoints();
        storageManager.storeParties(partyQueue);
        partyQueue.clear();
    }

    private void storeUnitDates() {
        storageManager.storeGeneric(unitDateQueue);
        unitDateQueue.clear();
    }

    private void storeGroupIdentifiers(boolean storeReferenced) {
        if (storeReferenced) {
            List<PartyWrapper> partyQueue = typeGroupedPartyQueue.get(PartyType.GROUP_PARTY);
            if (partyQueue != null) {
                storeParties(partyQueue);
            }
            storeUnitDates();
        }
        storageManager.storeGeneric(groupIdentifierQueue);
        groupIdentifierQueue.clear();
    }

    private void storeNames(boolean storeReferenced) {
        if (logger.isDebugEnabled()) {
            logger.debug("Store names, count: {}", nameQueue.size());
        }
        if (storeReferenced) {
            storeParties();
            storeUnitDates();
        }
        storageManager.storeGeneric(nameQueue);
        nameQueue.clear();
    }

    private void storeNameComplements(boolean storeReferenced) {
        if (storeReferenced) {
            storeNames(true);
        }
        storageManager.storeGeneric(nameCmplQueue);
        nameCmplQueue.clear();
    }

    private void storePreferredNames(boolean storeReferenced) {
        if (logger.isDebugEnabled()) {
            logger.debug("Store prefer names, count: {}", prefNameQueue.size());
        }
        if (storeReferenced) {
            storeNames(true);
        }
        storageManager.storeRefUpdates(prefNameQueue);
        prefNameQueue.clear();
    }

    /**
     * Listens for parties related phases.
     */
    private static class PartiesRelatedPhaseChangeListener implements ImportPhaseChangeListener {

        @Override
        public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
        	if (previousPhase == ImportPhase.PARTIES) {
        		context.getAccessPoints().storeAll();
        		context.getParties().storeAll();
        	}
            // after last party related phase
            if (ImportPhase.RELATIONS.isSubsequent(nextPhase)) {
            	// temporary solution
            	// TODO: groovy script after commit (outside import)
            	context.getParties().prepareAps();
            	context.getAccessPoints().storeAll();
                // parties were stored -> unregister listener
                return false;
            }
            return true;
        }
    }
}
