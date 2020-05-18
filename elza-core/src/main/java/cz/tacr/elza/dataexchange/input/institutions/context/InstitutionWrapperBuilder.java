package cz.tacr.elza.dataexchange.input.institutions.context;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.context.ImportPhaseChangeListener;
import cz.tacr.elza.dataexchange.input.parts.context.PartInfo;
import cz.tacr.elza.dataexchange.input.parts.context.PartsContext;
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

    private final Set<Integer> referencedApIds = new HashSet<>();

    private final int batchSize;

    private final InstitutionRepository instRepository;

    // map contains current institutions for updated parties.
    private Map<Integer, InstInfo> apIdInstInfoMap;

    public InstitutionWrapperBuilder(int batchSize, InstitutionRepository instRepository) {
        this.batchSize = batchSize;
        this.instRepository = instRepository;
    }

    public InstitutionWrapper build(ParInstitution entity, AccessPointInfo apInfo) {
        InstitutionWrapper wrapper = new InstitutionWrapper(entity, apInfo);
        // check existing institution
        InstInfo instInfo = apIdInstInfoMap.get(apInfo.getEntityId());
        if (instInfo != null) {
            // mark existing institution as imported
            if (instInfo.isPaired()) {
                throw new DEImportException(
                        "AP with more than one institution, ApId=" + apInfo.getEntityId());
            }
            entity.setInstitutionId(instInfo.getInstitutionId());
            wrapper.setSaveMethod(SaveMethod.UPDATE);
            instInfo.setAsPaired();
            return wrapper;
        }
        // new institution -> check multiple institutions for single party
        if (!referencedApIds.add(apInfo.getEntityId())) {
            throw new DEImportException(
                    "Party with more than one institution, partyId=" + apInfo.getEntityId());
        }
        return wrapper;
    }

    @Override
    public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
        //AccessPointsContext apContext = context.getAccessPoints();
        PartsContext partsContext = context.getParts();
        if(nextPhase == ImportPhase.INSTITUTIONS) {
           apIdInstInfoMap = findInstitutionsByUpdatedAccessPoints(partsContext.getAllPartInfo());
        }
        if (previousPhase == ImportPhase.INSTITUTIONS) {
            // end -> delete not paired institutions and clean up resources
           /* deleteNotPairedInstitutions(apIdInstInfoMap.values());
            apIdInstInfoMap = null;
            referencedApIds.clear();
            return false;*/
        }
        return !ImportPhase.INSTITUTIONS.isSubsequent(nextPhase);
    }

    /**
     * @return mapping from entity id of accesspoint to its current institution
     */
    private Map<Integer, InstInfo> findInstitutionsByUpdatedAccessPoints(Collection<PartInfo> PartInfoList) {
        // prepare id list of updated accesspoints
        List<Integer> apIds = new ArrayList<>();
        for (PartInfo partInfo : PartInfoList) {
            if (partInfo.getSaveMethod().equals(SaveMethod.UPDATE)) {
                apIds.add(partInfo.getApInfo().getEntityId());
            }
        }
        Map<Integer, InstInfo> apIdInstInfoMap = new HashMap<>();
        // find existing institutions by its party id
        for (List<Integer> apIdBatch : Iterables.partition(apIds, batchSize)) {
            List<ParInstitutionInfo> currInsts = instRepository.findInfoByAccessPointIdIn(apIdBatch);
            for (ParInstitutionInfo currInst : currInsts) {
                InstInfo instInfo = new InstInfo(currInst.getInstitutionId());
                if (apIdInstInfoMap.put(currInst.getInstitutionId(), instInfo) != null) {
                    throw new SystemException(
                            "Current party with more than one institution, existingPartyId:" + currInst.getInstitutionId(),
                            BaseCode.DB_INTEGRITY_PROBLEM);
                }
            }
        }
        return apIdInstInfoMap;
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
