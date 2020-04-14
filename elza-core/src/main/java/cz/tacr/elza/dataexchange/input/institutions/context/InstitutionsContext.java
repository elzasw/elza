package cz.tacr.elza.dataexchange.input.institutions.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.context.ImportPhaseChangeListener;
import cz.tacr.elza.dataexchange.input.context.ObservableImport;
import cz.tacr.elza.dataexchange.input.storage.SaveMethod;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.repository.InstitutionTypeRepository;

public class InstitutionsContext {

    private final StorageManager storageManager;

    private final int batchSize;

    private final InstitutionWrapperBuilder wrapperBuilder;

    private final Map<String, ParInstitutionType> instTypeCodeMap;

    private final List<InstitutionWrapper> instQueue = new ArrayList<>();

    public InstitutionsContext(StorageManager storageManager, int batchSize, ImportInitHelper initHelper) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.wrapperBuilder = new InstitutionWrapperBuilder(batchSize, initHelper.getInstitutionRepository());
        this.instTypeCodeMap = loadInstitutionTypeCodeMap(initHelper.getInstitutionTypeRepository());
    }

    public void init(ObservableImport observableImport) {
        observableImport.registerPhaseChangeListener(wrapperBuilder);
        observableImport.registerPhaseChangeListener(new InstitutionsPhaseEndListener());
    }

    public ParInstitutionType getInstitutionTypeByCode(String code) {
        return instTypeCodeMap.get(code);
    }

    public void addInstitution(ParInstitution entity) {
        InstitutionWrapper wrapper = wrapperBuilder.build(entity);
        instQueue.add(wrapper);
        if (instQueue.size() >= batchSize) {
            store();
        }
    }

    private void store() {
        storageManager.storeGeneric(instQueue);
        instQueue.clear();
    }

    private static Map<String, ParInstitutionType> loadInstitutionTypeCodeMap(InstitutionTypeRepository instTypeRepository) {
        List<ParInstitutionType> instTypes = instTypeRepository.findAll();
        Map<String, ParInstitutionType> instTypeCodeMap = new HashMap<>(instTypes.size());
        instTypes.forEach(it -> instTypeCodeMap.put(it.getCode(), it));
        return instTypeCodeMap;
    }

    /**
     * Listens for end of institutions phase and stores all remaining entities.
     */
    private static class InstitutionsPhaseEndListener implements ImportPhaseChangeListener {

        @Override
        public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
            if (previousPhase == ImportPhase.INSTITUTIONS) {
                context.getInstitutions().store();
                return false;
            }
            return !ImportPhase.INSTITUTIONS.isSubsequent(nextPhase);
        }
    }
}
