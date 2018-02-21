package cz.tacr.elza.dataexchange.input.institutions.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.context.ImportPhaseChangeListener;
import cz.tacr.elza.dataexchange.input.context.ObservableImport;
import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.repository.InstitutionTypeRepository;

public class InstitutionsContext {

    private final StorageManager storageManager;

    private final int batchSize;

    private final InstitutionUpdateProcessor mergeProcessor;

    private final Map<String, ParInstitutionType> institutionTypeCodeMap;

    private final List<InstitutionWrapper> institutionQueue = new ArrayList<>();

    public InstitutionsContext(StorageManager storageManager,
                               int batchSize,
                               ImportInitHelper initHelper) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.mergeProcessor = new InstitutionUpdateProcessor(batchSize, initHelper.getInstitutionRepository());
        this.institutionTypeCodeMap = loadInstitutionTypeCodeMap(initHelper.getInstitutionTypeRepository());
    }

    public void init(ObservableImport observableImport) {
        observableImport.registerPhaseChangeListener(mergeProcessor);
        observableImport.registerPhaseChangeListener(new InstitutionsPhaseEndListener());
    }

    public ParInstitutionType getInstitutionTypeByCode(String code) {
        return institutionTypeCodeMap.get(code);
    }

    public void addInstitution(ParInstitution entity, PartyInfo partyInfo) {
        InstitutionWrapper wrapper = mergeProcessor.createWrapper(entity, partyInfo);
        institutionQueue.add(wrapper);
        if (institutionQueue.size() >= batchSize) {
            storeInstitutions();
        }
    }

    private void storeInstitutions() {
        storageManager.saveInstitutions(institutionQueue);
        institutionQueue.clear();
    }

    private static Map<String, ParInstitutionType> loadInstitutionTypeCodeMap(InstitutionTypeRepository institutionTypeRepository) {
        List<ParInstitutionType> institutionTypes = institutionTypeRepository.findAll();
        Map<String, ParInstitutionType> institutionTypesCodeMap = new HashMap<>(institutionTypes.size());
        institutionTypes.forEach(it -> institutionTypesCodeMap.put(it.getCode(), it));
        return institutionTypesCodeMap;
    }

    /**
     * Listens for end of institutions phase and stores all remaining entities.
     */
    private static class InstitutionsPhaseEndListener implements ImportPhaseChangeListener {

        @Override
        public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
            if (previousPhase == ImportPhase.INSTITUTIONS) {
                context.getInstitutions().storeInstitutions();
                return false;
            }
            return !ImportPhase.INSTITUTIONS.isSubsequent(nextPhase);
        }
    }
}
