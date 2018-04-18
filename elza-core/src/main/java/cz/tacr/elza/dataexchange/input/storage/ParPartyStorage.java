package cz.tacr.elza.dataexchange.input.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.parties.context.PartyWrapper;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.projection.ParPartyInfo;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.UnitdateRepository;

/**
 * Specialization of wrapper storage for imported parties.
 */
class ParPartyStorage extends EntityStorage<PartyWrapper> {

    private final PartyRepository partyRepository;

    private final UnitdateRepository unitdateRepository;

    private final PartyGroupIdentifierRepository groupIdentifierRepository;

    private final PartyNameRepository nameRepository;

    private final PartyNameComplementRepository nameComplementRepository;

    public ParPartyStorage(StorageListener storageListener, Session session, ImportInitHelper initHelper) {
        super(session, storageListener);
        this.partyRepository = initHelper.getPartyRepository();
        this.nameRepository = initHelper.getNameRepository();
        this.nameComplementRepository = initHelper.getNameComplementRepository();
        this.groupIdentifierRepository = initHelper.getGroupIdentifierRepository();
        this.unitdateRepository = initHelper.getUnitdateRepository();
    }

    @Override
    protected void update(Collection<PartyWrapper> items) {
        prepareCollectionForUpdate(items);
        super.update(items);
    }

    private void prepareCollectionForUpdate(Collection<PartyWrapper> items) {
        readCurrentPartyIds(items);
        deleteSubEntities(items);
    }

    private void readCurrentPartyIds(Collection<PartyWrapper> items) {
        Map<Integer, PartyWrapper> recordIdMap = new HashMap<>(items.size());
        // init record id - party wrapper map
        for (PartyWrapper item : items) {
            Integer recordId = item.getPartyInfo().getAPId();
            Validate.notNull(recordId);
            recordIdMap.put(recordId, item);
        }
        // find all current parties
        List<ParPartyInfo> currentParties = partyRepository.findInfoByRecordAccessPointIdIn(recordIdMap.keySet());
        if (currentParties.size() != recordIdMap.size()) {
            throw new IllegalStateException(
                    "Not all parties for update found, recordIds:" + StringUtils.join(recordIdMap.keySet(), ','));
        }
        // update wrapper by current party
        for (ParPartyInfo info : currentParties) {
            ParParty entity = recordIdMap.get(info.getRecordId()).getEntity();
            entity.setPartyId(info.getPartyId());
            entity.setVersion(info.getVersion());
        }
    }

    /**
     * Delete current sub-entities for each party except for institutions.
     */
    private void deleteSubEntities(Collection<PartyWrapper> items) {
        List<ParParty> parties = new ArrayList<>(items.size());
        List<ParPartyGroup> partyGroups = new ArrayList<>(items.size());
        List<ParUnitdate> unitdates = new LinkedList<>();

        // fill party search collections
        for (PartyWrapper item : items) {
            ParParty party = item.getEntity();
            Validate.notNull(party.getPartyId());
            parties.add(party);
            if (item.getPartyInfo().getPartyType().equals(PartyType.GROUP_PARTY)) {
                partyGroups.add((ParPartyGroup) party);
            }
        }

        // find all names and their intervals
        List<ParPartyName> names = nameRepository.findByPartyIn(parties);
        List<Integer> nameIds = new ArrayList<>(names.size());
        for (ParPartyName name : names) {
            if (name.getValidFrom() != null) {
                unitdates.add(name.getValidFrom());
            }
            if (name.getValidTo() != null) {
                unitdates.add(name.getValidTo());
            }
            nameIds.add(name.getPartyNameId());
        }

        // delete references to party names
        nameRepository.deletePreferredNameReferencesByPartyNameIdIn(nameIds);
        nameRepository.deleteComplementReferencesByPartyNameIdIn(nameIds);
        // delete all names
        nameRepository.deleteInBatch(names);
        // delete all complements
        nameComplementRepository.deleteByPartyNameIn(names);

        // find all group identifiers and their intervals
        if (partyGroups.size() > 0) {
            List<ParPartyGroupIdentifier> groupIdentifiers = groupIdentifierRepository.findByPartyGroupIn(partyGroups);
            for (ParPartyGroupIdentifier gid : groupIdentifiers) {
                if (gid.getFrom() != null) {
                    unitdates.add(gid.getFrom());
                }
                if (gid.getTo() != null) {
                    unitdates.add(gid.getTo());
                }
            }
            // delete all group identifiers
            groupIdentifierRepository.deleteInBatch(groupIdentifiers);
        }

        // delete all unit dates
        unitdateRepository.deleteInBatch(unitdates);
    }
}
