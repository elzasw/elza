package cz.tacr.elza.dataexchange.input.institutions.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.Iterables;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.context.ImportPhaseChangeListener;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.parties.context.PartiesContext;
import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.InstitutionRepository;

/**
 * Manager for importing institutions.
 */
class InstitutionUpdateProcessor implements ImportPhaseChangeListener {

    private final Set<Integer> partyIdCreateFilter = new HashSet<>();

    private final int batchSize;

    private final InstitutionRepository institutionRepository;

    /**
     * Map contains institutions with updated party.
     */
    private Map<Integer, InstitutionInfo> partyIdInstitutionMap;

    public InstitutionUpdateProcessor(int batchSize, InstitutionRepository institutionRepository) {
        this.batchSize = batchSize;
        this.institutionRepository = institutionRepository;
    }

    public InstitutionWrapper createWrapper(ParInstitution entity, PartyInfo partyInfo) {
        Validate.notNull(partyIdInstitutionMap);
        Validate.isTrue(partyInfo.hasEntityId());

        InstitutionInfo info = partyIdInstitutionMap.get(partyInfo.getEntityId());
        if (info != null) {
            // mark institution as imported
            if (info.isImported()) {
                throw new DEImportException("Party cannot have more than one institution, partyId:" + partyInfo.getImportId());
            }
            entity.setInstitutionId(info.getInstitutionId());
            info.setAsImported();
        } else {
            // only check import duplicate
            if (!partyIdCreateFilter.add(partyInfo.getEntityId())) {
                throw new DEImportException("Party cannot have more than one institution, partyId:" + partyInfo.getImportId());
            }
        }
        return new InstitutionWrapper(entity, partyInfo);
    }

    @Override
    public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
        PartiesContext parties = context.getParties();
        if (nextPhase == ImportPhase.INSTITUTIONS) {
            // begin -> prepare institutions from updated parties
            Validate.isTrue(partyIdInstitutionMap == null);
            partyIdInstitutionMap = createPartyIdMapFromUpdatedParties(parties.getAllPartyInfo());
            return true;
        }
        if (previousPhase == ImportPhase.INSTITUTIONS) {
            // end -> delete not imported institutions with updated party
            Validate.notNull(partyIdInstitutionMap);
            deleteNotImportedInstitutions(partyIdInstitutionMap.values());
            return false;
        }
        return !ImportPhase.INSTITUTIONS.isSubsequent(nextPhase);
    }

    private Map<Integer, InstitutionInfo> createPartyIdMapFromUpdatedParties(Collection<PartyInfo> allPartyInfo) {
        // prepare list of party ids
        List<Integer> updatedPartyIds = new ArrayList<>();
        for (PartyInfo info : allPartyInfo) {
            Validate.isTrue(info.hasEntityId());
            if (info.getPersistMethod().equals(PersistMethod.UPDATE)) {
                updatedPartyIds.add(info.getEntityId());
            }
        }
        // find related institutions
        Map<Integer, InstitutionInfo> map = new HashMap<>();
        for (List<Integer> partyIdBatch : Iterables.partition(updatedPartyIds, batchSize)) {
            List<ParInstitution> institutions = institutionRepository.findByPartyIdIn(partyIdBatch);
            for (ParInstitution inst : institutions) {
                InstitutionInfo info = new InstitutionInfo(inst.getInstitutionId());
                if (map.put(inst.getPartyId(), info) != null) {
                    throw new SystemException("Current party has more than one institution, current partyId:" + inst.getPartyId(),
                            BaseCode.DB_INTEGRITY_PROBLEM);
                }
            }
        }
        return map;
    }

    private void deleteNotImportedInstitutions(Collection<InstitutionInfo> institutionInfoList) {
        List<Integer> deleteInstitutionIds = institutionInfoList.stream()
                .filter(info -> !info.isImported())
                .map(info -> info.getInstitutionId())
                .collect(Collectors.toList());
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
