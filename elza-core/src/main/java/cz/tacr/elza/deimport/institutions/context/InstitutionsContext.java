package cz.tacr.elza.deimport.institutions.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportPhase;
import cz.tacr.elza.deimport.context.ImportPhaseChangeListener;
import cz.tacr.elza.deimport.context.ObservableImport;
import cz.tacr.elza.deimport.parties.context.PartyImportInfo;
import cz.tacr.elza.deimport.storage.StorageManager;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.InstitutionTypeRepository;

public class InstitutionsContext {

    private final StorageManager storageManager;

    private final int batchSize;

    private final InstitutionImportManager institutionsImportManager;

    private final Map<String, ParInstitutionType> institutionTypeCodeMap;

    private final List<InstitutionWrapper> institutionQueue = new ArrayList<>();

    public InstitutionsContext(StorageManager storageManager,
                               int batchSize,
                               InstitutionRepository institutionRepository,
                               InstitutionTypeRepository institutionTypeRepository) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.institutionsImportManager = new InstitutionImportManager(batchSize, institutionRepository);
        this.institutionTypeCodeMap = loadInstitutionTypeCodeMap(institutionTypeRepository);
    }

    public void init(ObservableImport observableImport) {
        observableImport.registerPhaseChangeListener(institutionsImportManager);
        observableImport.registerPhaseChangeListener(new InstitutionsPhaseEndListener());
    }

    public ParInstitutionType getInstitutionTypeByCode(String code) {
        return institutionTypeCodeMap.get(code);
    }

    public void addInstitution(ParInstitution entity, PartyImportInfo partyInfo) {
        InstitutionWrapper wrapper = institutionsImportManager.createWrapper(entity, partyInfo);
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
