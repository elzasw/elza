package cz.tacr.elza.dataexchange.input.parties.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.service.AccessPointDataService;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.util.Assert;

import cz.tacr.elza.core.data.PartyType;
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
import cz.tacr.elza.dataexchange.input.parties.aps.PartiesAccessPointsBuilder;
import cz.tacr.elza.dataexchange.input.parties.aps.PartyAccessPointWrapper;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.service.GroovyScriptService;

/**
 * Context for data exchange parties.
 */
public class PartiesContext {

    private final Map<String, PartyInfo> partyImportIdMap = new HashMap<>();

    private final StorageManager storageManager;

    private final int batchSize;

    private final AccessPointsContext accessPointContext;

    private final Session session;

    private final Map<PartyType, PartyTypeGroup> partyTypeGroupQueueMap = new EnumMap<>(PartyType.class);

    private final List<PartyUnitDateWrapper> unitDateQueue = new ArrayList<>();

    private final List<PartyGroupIdentifierWrapper> groupIdentifierQueue = new ArrayList<>();

    private final List<PartyNameWrapper> nameQueue = new ArrayList<>();

    private final List<PartyNameComplementWrapper> nameComplementQueue = new ArrayList<>();

    private final List<PartyPreferredNameWrapper> preferredNameQueue = new ArrayList<>();

    private final GroovyScriptService groovyScriptService;

    private final AccessPointDataService accessPointDataService;

    public PartiesContext(StorageManager storageManager,
                          int batchSize,
                          AccessPointsContext accessPointContext,
                          Session session,
                          ImportInitHelper initHelper) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.accessPointContext = accessPointContext;
        this.session = session;
        this.groovyScriptService = initHelper.getGroovyScriptService();
        this.accessPointDataService = initHelper.getAccessPointDataService();
    }

    public void init(ObservableImport observableImport) {
        observableImport.registerPhaseChangeListener(new PartiesRelatedPhaseChangeListener());
    }

    public Collection<PartyInfo> getAllPartyInfo() {
        return Collections.unmodifiableCollection(partyImportIdMap.values());
    }

    public PartyInfo getPartyInfo(String importId) {
        return partyImportIdMap.get(importId);
    }

    public PartyInfo addParty(ParParty party, String importId, AccessPointInfo apInfo, PartyType partyType) {
        PartyInfo info = new PartyInfo(importId, apInfo, partyType);
        if (partyImportIdMap.putIfAbsent(importId, info) != null) {
            throw new DEImportException("Party has duplicate id, partyId:" + importId);
        }
        PartyTypeGroup group = partyTypeGroupQueueMap.get(partyType);
        if (group == null) {
            group = new PartyTypeGroup(partyType);
            partyTypeGroupQueueMap.put(partyType, group);
        }
        PartyWrapper wrapper = new PartyWrapper(party, info);
        group.add(wrapper);
        if (group.getSize() >= batchSize) {
            group.storeParties();
        }
        return info;
    }

    public PartyNameWrapper addName(ParPartyName partyName, PartyInfo partyInfo, boolean preferred) {
        PartyNameWrapper wrapper = new PartyNameWrapper(partyName, partyInfo);
        nameQueue.add(wrapper);
        if (nameQueue.size() >= batchSize) {
            storeNames();
        }
        if (preferred) {
            preferredNameQueue.add(new PartyPreferredNameWrapper(partyInfo, wrapper.getIdHolder()));
            if (preferredNameQueue.size() >= batchSize) {
                storePreferredNames();
            }
        }
        return wrapper;
    }

    public EntityIdHolder<ParUnitdate> addUnitDate(ParUnitdate unitDate, PartyInfo partyInfo) {
        PartyUnitDateWrapper wrapper = new PartyUnitDateWrapper(unitDate, partyInfo);
        unitDateQueue.add(wrapper);
        if (unitDateQueue.size() >= batchSize) {
            storeUnitDates();
        }
        return wrapper.getIdHolder();
    }

    public void addNameComplement(ParPartyNameComplement partyNameComplement, PartyRelatedIdHolder<ParPartyName> partyNameIdHolder) {
        PartyNameComplementWrapper wrapper = new PartyNameComplementWrapper(partyNameComplement, partyNameIdHolder);
        nameComplementQueue.add(wrapper);
        if (nameComplementQueue.size() >= batchSize) {
            storeNameComplements();
        }
    }

    public PartyGroupIdentifierWrapper addGroupIdentifier(ParPartyGroupIdentifier entity, PartyInfo partyInfo) {
        PartyGroupIdentifierWrapper wrapper = new PartyGroupIdentifierWrapper(entity, partyInfo);
        groupIdentifierQueue.add(wrapper);
        if (groupIdentifierQueue.size() >= batchSize) {
            storeGroupIdentifiers();
        }
        return wrapper;
    }

    public void storeAll() {
        storePartyTypeGroups();
        storeUnitDates();
        storeGroupIdentifiers();
        storeNames();
        storeNameComplements();
        storePreferredNames();
    }

    /**
     * @param partyTypes not-null, if empty all present type groups are stored.
     */
    private void storePartyTypeGroups(PartyType... partyTypes) {
        if (partyTypes.length == 0) {
            partyTypeGroupQueueMap.values().forEach(PartyTypeGroup::storeParties);
        } else {
            for (PartyType pt : partyTypes) {
                PartyTypeGroup group = partyTypeGroupQueueMap.get(pt);
                if (group != null) {
                    group.storeParties();
                }
            }
        }
    }

    private void storeUnitDates() {
        if (unitDateQueue.isEmpty()) {
            return;
        }
        storageManager.savePartyUnitDates(unitDateQueue);
        unitDateQueue.clear();
    }

    private void storeGroupIdentifiers() {
        storePartyTypeGroups(PartyType.GROUP_PARTY);
        storeUnitDates();
        storageManager.savePartyGroupIdentifiers(groupIdentifierQueue);
        groupIdentifierQueue.clear();
    }

    private void storeNames() {
        if (nameQueue.isEmpty()) {
            return;
        }
        storePartyTypeGroups();
        storeUnitDates();
        storageManager.savePartyNames(nameQueue);
        nameQueue.clear();
    }

    private void storeNameComplements() {
        storeNames();
        storageManager.savePartyNameComplements(nameComplementQueue);
        nameComplementQueue.clear();
    }

    private void storePreferredNames() {
        storeNames();
        storageManager.savePartyPreferredNames(preferredNameQueue);
        preferredNameQueue.clear();
    }

    private class PartyTypeGroup {

        private final List<PartyWrapper> partyQueue = new ArrayList<>();

        private final PartyType partyType;

        public PartyTypeGroup(PartyType partyType) {
            this.partyType = Validate.notNull(partyType);
        }

        public int getSize() {
            return partyQueue.size();
        }

        public void add(PartyWrapper item) {
            Assert.equals(partyType, item.getPartyInfo().getPartyType());
            partyQueue.add(item);
        }

        public void storeParties() {
            if (partyQueue.isEmpty()) {
                return;
            }
            accessPointContext.storeAccessPoints();
            storageManager.saveParties(partyQueue);
            partyQueue.clear();
        }
    }

    /**
     * Listens for parties related phases.
     */
    private static class PartiesRelatedPhaseChangeListener implements ImportPhaseChangeListener {

        @Override
        public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
            boolean partyRelatedEnds = ImportPhase.RELATIONS.isSubsequent(nextPhase);
            PartiesContext partiesContext = context.getParties();

            if (partyRelatedEnds || previousPhase == ImportPhase.PARTIES) {
                // store remaining access points and parties
                context.getAccessPoints().storeAll();
                partiesContext.storeAll();
            }
            if (partyRelatedEnds) {
                // execute builder
                buildPartiesAccessPoints(partiesContext, context.getStaticData());
                // clear all party related entities
                partiesContext.storageManager.clear();
                return false;
            }
            return true;
        }

        private void buildPartiesAccessPoints(PartiesContext context, StaticDataProvider staticData) {
            PartiesAccessPointsBuilder builder = new PartiesAccessPointsBuilder(staticData, context.groovyScriptService,
                    context.session, context.accessPointDataService);
            Collection<PartyInfo> partiesInfo = context.partyImportIdMap.values();
            for (List<PartyInfo> batch : Iterables.partition(partiesInfo, context.batchSize)) {
                List<PartyAccessPointWrapper> items = builder.build(batch);
                context.storageManager.savePartyAccessPoints(items);
            }
        }
    }
}
