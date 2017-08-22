package cz.tacr.elza.deimport.parties.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.Session;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.util.Assert;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.aps.context.AccessPointsContext;
import cz.tacr.elza.deimport.aps.context.RecordImportInfo;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportContext.ImportPhase;
import cz.tacr.elza.deimport.context.ImportObserver;
import cz.tacr.elza.deimport.context.ImportPhaseChangeListener;
import cz.tacr.elza.deimport.context.StatefulIdHolder;
import cz.tacr.elza.deimport.parties.PartiesAccessPointsBuilder;
import cz.tacr.elza.deimport.storage.StorageManager;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.InstitutionTypeRepository;
import cz.tacr.elza.service.GroovyScriptService;

/**
 * Context for data exchange parties.
 */
public class PartiesContext {

    private final Map<String, PartyImportInfo> partyImportIdMap = new HashMap<>();

    private final StorageManager storageManager;

    private final int batchSize;

    private final AccessPointsContext accessPointContext;

    private final Map<PartyType, PartyTypeGroup> partyTypeGroupQueueMap = new EnumMap<>(PartyType.class);

    private final List<PartyUnitDateWrapper> unitDateQueue = new ArrayList<>();

    private final List<PartyGroupIdentifierWrapper> groupIdentifierQueue = new ArrayList<>();

    private final List<PartyNameWrapper> nameQueue = new ArrayList<>();

    private final List<PartyNameComplementWrapper> nameComplementQueue = new ArrayList<>();

    private final List<PartyPreferredNameWrapper> preferredNameQueue = new ArrayList<>();

    private final GroovyScriptService groovyScriptService;

    public PartiesContext(StorageManager storageManager,
                          int batchSize,
                          AccessPointsContext accessPointContext,
                          InstitutionRepository institutionRepository,
                          InstitutionTypeRepository institutionTypeRepository,
                          GroovyScriptService groovyScriptService) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.accessPointContext = accessPointContext;
        this.groovyScriptService = groovyScriptService;
    }

    public void init(ImportObserver importObserver) {
        importObserver.registerPhaseChangeListener(new PartiesRelatedPhaseChangeListener());
    }

    public Collection<PartyImportInfo> getAllPartyInfo() {
        return Collections.unmodifiableCollection(partyImportIdMap.values());
    }

    public PartyImportInfo getPartyInfo(String importId) {
        return partyImportIdMap.get(importId);
    }

    public PartyImportInfo addParty(ParParty party, String importId, RecordImportInfo recordInfo, PartyType partyType) {
        PartyImportInfo info = new PartyImportInfo(importId, recordInfo, partyType);
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

    public PartyNameWrapper addName(ParPartyName partyName, PartyImportInfo partyIdHolder, boolean preferred) {
        PartyNameWrapper wrapper = new PartyNameWrapper(partyName, partyIdHolder);
        nameQueue.add(wrapper);
        if (nameQueue.size() >= batchSize) {
            storeNames();
        }
        if (preferred) {
            preferredNameQueue.add(new PartyPreferredNameWrapper(partyIdHolder, wrapper.getIdHolder()));
            if (preferredNameQueue.size() >= batchSize) {
                storePreferredNames();
            }
        }
        return wrapper;
    }

    public StatefulIdHolder addUnitDate(ParUnitdate unitDate, RecordImportInfo recordInfo) {
        PartyUnitDateWrapper wrapper = new PartyUnitDateWrapper(unitDate, recordInfo);
        unitDateQueue.add(wrapper);
        if (unitDateQueue.size() >= batchSize) {
            storeUnitDates();
        }
        return wrapper.getHolderId();
    }

    public void addNameComplement(ParPartyNameComplement partyNameComplement, StatefulIdHolder partyNameIdHolder) {
        PartyNameComplementWrapper wrapper = new PartyNameComplementWrapper(partyNameComplement, partyNameIdHolder);
        nameComplementQueue.add(wrapper);
        if (nameComplementQueue.size() >= batchSize) {
            storeNameComplements();
        }
    }

    public PartyGroupIdentifierWrapper addGroupIdentifier(ParPartyGroupIdentifier entity, PartyImportInfo partyInfo) {
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
            this.partyType = Objects.requireNonNull(partyType);
        }

        public int getSize() {
            return partyQueue.size();
        }

        public void add(PartyWrapper item) {
            Assert.equals(partyType, item.getInfo().getPartyType());
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
                // run builder and clear all changes
                buildPartiesAccessPoints(partiesContext, context.getSession(), context.getStaticData());
                partiesContext.storageManager.clear();
                return false;
            }
            return true;
        }

        private void buildPartiesAccessPoints(PartiesContext context, Session session, StaticDataProvider staticData) {
            PartiesAccessPointsBuilder builder = new PartiesAccessPointsBuilder(session, staticData, context.groovyScriptService);
            Collection<PartyImportInfo> partiesInfo = context.partyImportIdMap.values();
            for (List<PartyImportInfo> batch : Iterables.partition(partiesInfo, context.batchSize)) {
                List<PartyAccessPointWrapper> items = builder.build(batch);
                context.storageManager.savePartyAccessPoints(items);
            }
        }
    }
}
