package cz.tacr.elza.deimport.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.util.Assert;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.deimport.parties.context.PartyWrapper;
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

    public ParPartyStorage(Session session,
                           StorageListener storageListener,
                           PartyRepository partyRepository,
                           PartyNameRepository nameRepository,
                           PartyNameComplementRepository nameComplementRepository,
                           PartyGroupIdentifierRepository groupIdentifierRepository,
                           UnitdateRepository unitdateRepository) {
        super(session, storageListener);
        this.partyRepository = partyRepository;
        this.nameRepository = nameRepository;
        this.nameComplementRepository = nameComplementRepository;
        this.groupIdentifierRepository = groupIdentifierRepository;
        this.unitdateRepository = unitdateRepository;
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
            Integer recordId = item.getInfo().getRecordId();
            Assert.notNull(recordId);
            recordIdMap.put(recordId, item);
        }
        // find all current parties
        List<ParPartyInfo> currentParties = partyRepository.findInfoByRecordRecordIdIn(recordIdMap.keySet());
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
            Assert.notNull(party.getPartyId());
            parties.add(party);
            if (item.getInfo().getPartyType() == PartyType.GROUP_PARTY) {
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
