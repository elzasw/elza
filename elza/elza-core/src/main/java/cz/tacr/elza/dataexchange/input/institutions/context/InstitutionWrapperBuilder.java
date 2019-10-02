package cz.tacr.elza.dataexchange.input.institutions.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.context.ImportPhaseChangeListener;
import cz.tacr.elza.dataexchange.input.parties.context.PartiesContext;
import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.projection.ParInstitutionInfo;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.InstitutionRepository;

/**
 * Manager for importing institutions.
 */
class InstitutionWrapperBuilder implements ImportPhaseChangeListener {

    private final Set<Integer> referencedPartyIds = new HashSet<>();

    private final int batchSize;

    private final InstitutionRepository instRepository;

    // map contains current institutions for updated parties.
    private Map<Integer, InstInfo> partyIdInstInfoMap;

    public InstitutionWrapperBuilder(int batchSize, InstitutionRepository instRepository) {
        this.batchSize = batchSize;
        this.instRepository = instRepository;
    }

    public InstitutionWrapper build(ParInstitution entity, PartyInfo partyInfo) {
        InstitutionWrapper wrapper = new InstitutionWrapper(entity, partyInfo);
        // check existing institution
        InstInfo instInfo = partyIdInstInfoMap.get(partyInfo.getEntityId());
        if (instInfo != null) {
            // mark existing institution as imported
            if (instInfo.isPaired()) {
                throw new DEImportException(
                        "Party with more than one institution, partyId=" + partyInfo.getImportId());
            }
            entity.setInstitutionId(instInfo.getInstitutionId());
            wrapper.setSaveMethod(SaveMethod.UPDATE);
            instInfo.setAsPaired();
            return wrapper;
        }
        // new institution -> check multiple institutions for single party
        if (!referencedPartyIds.add(partyInfo.getEntityId())) {
            throw new DEImportException(
                    "Party with more than one institution, partyId=" + partyInfo.getImportId());
        }
        return wrapper;
    }

    @Override
    public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
        PartiesContext partiesCtx = context.getParties();
        if (nextPhase == ImportPhase.INSTITUTIONS) {
            // begin -> find institutions by updated parties
            partyIdInstInfoMap = findInstitutionsByUpdatedParties(partiesCtx.getAllPartyInfo());
            return true;
        }
        if (previousPhase == ImportPhase.INSTITUTIONS) {
            // end -> delete not paired institutions and clean up resources
            deleteNotPairedInstitutions(partyIdInstInfoMap.values());
            partyIdInstInfoMap = null;
            referencedPartyIds.clear();
            return false;
        }
        return !ImportPhase.INSTITUTIONS.isSubsequent(nextPhase);
    }

    /**
     * @return mapping from entity id of party to its current institution
     */
    private Map<Integer, InstInfo> findInstitutionsByUpdatedParties(Collection<PartyInfo> partyInfoList) {
        // prepare id list of updated parties
        List<Integer> partyIds = new ArrayList<>();
        for (PartyInfo partyInfo : partyInfoList) {
            if (partyInfo.getSaveMethod().equals(SaveMethod.UPDATE)) {
                partyIds.add(partyInfo.getEntityId());
            }
        }
        Map<Integer, InstInfo> partyIdInstInfoMap = new HashMap<>();
        // find existing institutions by its party id
        for (List<Integer> partyIdBatch : Iterables.partition(partyIds, batchSize)) {
            List<ParInstitutionInfo> currInsts = instRepository.findInfoByPartyIdIn(partyIdBatch);
            for (ParInstitutionInfo currInst : currInsts) {
                InstInfo instInfo = new InstInfo(currInst.getInstitutionId());
                if (partyIdInstInfoMap.put(currInst.getPartyId(), instInfo) != null) {
                    throw new SystemException(
                            "Current party with more than one institution, existingPartyId:" + currInst.getPartyId(),
                            BaseCode.DB_INTEGRITY_PROBLEM);
                }
            }
        }
        return partyIdInstInfoMap;
    }

    private void deleteNotPairedInstitutions(Collection<InstInfo> instInfoList) {
        List<Integer> instIds = instInfoList.stream()
                .filter(i -> !i.isPaired())
                .map(i -> i.getInstitutionId())
                .collect(Collectors.toList());
        for (List<Integer> instIdBatch : Iterables.partition(instIds, batchSize)) {
            instRepository.deleteByInstitutionIdIn(instIdBatch);
        }
    }

    private static class InstInfo {

        private final int institutionId;

        private boolean imported;

        public InstInfo(int institutionId) {
            this.institutionId = institutionId;
        }

        public int getInstitutionId() {
            return institutionId;
        }

        public boolean isPaired() {
            return imported;
        }

        public void setAsPaired() {
            imported = true;
        }
    }
}
