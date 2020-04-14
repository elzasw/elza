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
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.domain.ParInstitution;
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

    public InstitutionWrapper build(ParInstitution entity) {
        InstitutionWrapper wrapper = new InstitutionWrapper(entity);
        return wrapper;
    }

    @Override
    public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
        if (previousPhase == ImportPhase.INSTITUTIONS) {
            // end -> delete not paired institutions and clean up resources
            deleteNotPairedInstitutions(partyIdInstInfoMap.values());
            partyIdInstInfoMap = null;
            referencedPartyIds.clear();
            return false;
        }
        return !ImportPhase.INSTITUTIONS.isSubsequent(nextPhase);
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
