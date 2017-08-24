package cz.tacr.elza.deimport.institutions.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

import com.google.common.collect.Iterables;

import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportContext.ImportPhase;
import cz.tacr.elza.deimport.context.ImportPhaseChangeListener;
import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.parties.context.PartiesContext;
import cz.tacr.elza.deimport.parties.context.PartyImportInfo;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.InstitutionRepository;

class InstitutionImportManager implements ImportPhaseChangeListener {

    private final Set<Integer> partyIdCreateFilter = new HashSet<>();

    private final int batchSize;

    private final InstitutionRepository institutionRepository;

    private Map<Integer, InstitutionInfo> partyIdCurrentInstitutionMap;

    public InstitutionImportManager(int batchSize, InstitutionRepository institutionRepository) {
        this.batchSize = batchSize;
        this.institutionRepository = institutionRepository;
    }

    public InstitutionWrapper createWrapper(ParInstitution entity, PartyImportInfo partyInfo) {
        Assert.notNull(partyIdCurrentInstitutionMap);
        Assert.isTrue(partyInfo.isInitialized());

        InstitutionInfo info = partyIdCurrentInstitutionMap.get(partyInfo.getId());
        if (info != null) {
            if (info.isImported()) {
                throw new DEImportException("Party cannot have more than one institution, partyId:" + partyInfo.getImportId());
            }
            entity.setInstitutionId(info.getInstitutionId());
            info.setAsImported();
        } else {
            if (!partyIdCreateFilter.add(partyInfo.getId())) {
                throw new DEImportException("Party cannot have more than one institution, partyId:" + partyInfo.getImportId());
            }
        }
        return new InstitutionWrapper(entity, partyInfo);
    }

    @Override
    public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
        PartiesContext parties = context.getParties();
        if (nextPhase == ImportPhase.INSTITUTIONS) {
            Assert.isNull(partyIdCurrentInstitutionMap);
            partyIdCurrentInstitutionMap = createPartyIdCurrentInstitutionMap(parties.getAllPartyInfo());
            return true;
        }
        if (previousPhase == ImportPhase.INSTITUTIONS) {
            Assert.notNull(partyIdCurrentInstitutionMap);
            deleteNotImportedInstitutions(partyIdCurrentInstitutionMap.values());
            return false;
        }
        return !ImportPhase.INSTITUTIONS.isSubsequent(nextPhase);
    }

    private Map<Integer, InstitutionInfo> createPartyIdCurrentInstitutionMap(Collection<PartyImportInfo> allPartyInfo) {
        // prepare party id list, all parties should be stored
        List<Integer> updatedPartyIds = new ArrayList<>();
        for (PartyImportInfo info : allPartyInfo) {
            Assert.isTrue(info.isInitialized());
            if (info.getState().equals(EntityState.UPDATE)) {
                updatedPartyIds.add(info.getId());
            }
        }
        // find current institutions by party id and fill map
        Map<Integer, InstitutionInfo> map = new HashMap<>();
        for (List<Integer> partyIdBatch : Iterables.partition(updatedPartyIds, batchSize)) {
            // find current institutions
            List<ParInstitution> currentInstitutions = institutionRepository.findByPartyIdIn(partyIdBatch);
            // fill partyId - info map
            for (ParInstitution currInstitution : currentInstitutions) {
                InstitutionInfo currInfo = new InstitutionInfo(currInstitution.getInstitutionId());
                if (map.put(currInstitution.getPartyId(), currInfo) != null) {
                    throw new SystemException(
                            "Current party has more than one institution, current partyId:" + currInstitution.getPartyId(),
                            BaseCode.DB_INTEGRITY_PROBLEM);
                }
            }
        }
        return map;
    }

    private void deleteNotImportedInstitutions(Collection<InstitutionInfo> currentInstitutions) {
        List<Integer> deleteInstitutionIds = new ArrayList<>();
        Iterator<InstitutionInfo> iterator = currentInstitutions.iterator();
        while (iterator.hasNext()) {
            InstitutionInfo info = iterator.next();
            if (!info.isImported()) {
                deleteInstitutionIds.add(info.getInstitutionId());
                iterator.remove();
            }
        }
        for (List<Integer> institutionIdBatch : Iterables.partition(deleteInstitutionIds, batchSize)) {
            institutionRepository.deleteByInstitutionIdIn(institutionIdBatch);
        }
    }

    private static class InstitutionInfo {

        private final int institutionId;

        private boolean imported;

        public InstitutionInfo(int institutionId) {
            this.institutionId = institutionId;
        }

        public int getInstitutionId() {
            return institutionId;
        }

        public boolean isImported() {
            return imported;
        }

        public void setAsImported() {
            imported = true;
        }
    }
}
