package cz.tacr.elza.dataexchange.input.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import cz.tacr.elza.domain.projection.ParPartyGroupIdentifierInfo;
import cz.tacr.elza.domain.projection.ParPartyInfo;
import cz.tacr.elza.domain.projection.ParPartyNameInfo;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.UnitdateRepository;

public class ParPartyStorage extends EntityStorage<PartyWrapper> {

    private final PartyRepository partyRepository;

    private final UnitdateRepository unitdateRepository;

    private final PartyGroupIdentifierRepository groupIdentifierRepository;

    private final PartyNameRepository nameRepository;

    private final PartyNameComplementRepository nameCmplRepository;

    public ParPartyStorage(Session session, StoredEntityCallback persistEntityListener,
            ImportInitHelper initHelper) {
        super(session, persistEntityListener);
        this.partyRepository = initHelper.getPartyRepository();
        this.nameRepository = initHelper.getNameRepository();
        this.nameCmplRepository = initHelper.getNameComplementRepository();
        this.groupIdentifierRepository = initHelper.getGroupIdentifierRepository();
        this.unitdateRepository = initHelper.getUnitdateRepository();
    }

    @Override
    protected void mergeEntities(Collection<PartyWrapper> pws) {
        prepareCurrentEntities(pws);
        super.mergeEntities(pws);
    }

    /**
     * Copies partyId and version from existing party to imported entity. All
     * current sub-entities are deleted.
     */
    private void prepareCurrentEntities(Collection<PartyWrapper> pws) {
        Map<Integer, PartyWrapper> apIdMap = new HashMap<>(pws.size());
        // init apId -> party map
        for (PartyWrapper pw : pws) {
            Integer apId = pw.getPartyInfo().getApInfo().getEntityId();
            Validate.notNull(apId);
            apIdMap.put(apId, pw);
        }
        // find current parties by apIds
        List<ParPartyInfo> currParties = partyRepository.findInfoByAccessPointIdIn(apIdMap.keySet());
        if (currParties.size() != apIdMap.size()) {
            throw new IllegalStateException(
                    "Not all party APs found, apIds=" + StringUtils.join(apIdMap.keySet(), ','));
        }
        // update wrapped entity by existing party
        for (ParPartyInfo info : currParties) {
            PartyWrapper wrapper = apIdMap.get(info.getAccessPointId());
            ParParty entity = wrapper.getEntity();
            entity.setPartyId(info.getPartyId());
            entity.setVersion(info.getVersion());
        }
        // delete all sub entities
        deleteSubEntities(pws);
    }

    /**
     * Delete current sub-entities for each party except for institutions.
     */
    private void deleteSubEntities(Collection<PartyWrapper> pws) {
        List<ParParty> parties = new ArrayList<>(pws.size());
        List<ParPartyGroup> partyGroups = new ArrayList<>(pws.size());
        List<Integer> unitdateIds = new ArrayList<>();

        // fill party search collections
        for (PartyWrapper pw : pws) {
            ParParty party = pw.getEntity();
            Validate.notNull(party.getPartyId());
            parties.add(party);
            if (pw.getPartyInfo().getPartyType().equals(PartyType.GROUP_PARTY)) {
                partyGroups.add((ParPartyGroup) party);
            }
        }

        // find all names and its unit dates
        List<ParPartyNameInfo> names = nameRepository.findInfoByPartyIn(parties);
        List<Integer> nameIds = new ArrayList<>(names.size());
        for (ParPartyNameInfo name : names) {
            if (name.getValidFromUnitdateId() != null) {
                unitdateIds.add(name.getValidFromUnitdateId());
            }
            if (name.getValidToUnitdateId() != null) {
                unitdateIds.add(name.getValidToUnitdateId());
            }
            nameIds.add(name.getPartyNameId());
        }

        if (nameIds.size() > 0) {
            // delete references to party names
            nameRepository.deletePreferredNameReferencesByPartyNameIdIn(nameIds);
            nameRepository.deleteComplementReferencesByPartyNameIdIn(nameIds);
            // delete all names
            nameRepository.deleteByPartyNameIdIn(nameIds);
            // delete all complements
            nameCmplRepository.deleteByPartyNameIdIn(nameIds);
        }

        // find all group identifiers and their intervals
        if (partyGroups.size() > 0) {
            List<ParPartyGroupIdentifierInfo> gids = groupIdentifierRepository.findInfoByPartyGroupIn(partyGroups);
            List<Integer> gidIds = new ArrayList<>(gids.size());
            for (ParPartyGroupIdentifierInfo gid : gids) {
                if (gid.getFromUnitdateId() != null) {
                    unitdateIds.add(gid.getFromUnitdateId());
                }
                if (gid.getToUnitdateId() != null) {
                    unitdateIds.add(gid.getToUnitdateId());
                }
                gidIds.add(gid.getPartyGroupIdentifierId());
            }
            // delete all group identifiers
            groupIdentifierRepository.deleteByPartyGroupIdentifierIdIn(gidIds);
        }

        // delete all unit dates
        unitdateRepository.deleteByUnitdateIdIn(unitdateIds);
    }
}
