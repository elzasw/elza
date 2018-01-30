package cz.tacr.elza.dataexchange.output.sections;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.RulStructureType;

public class StructObjectInfoLoader extends AbstractEntityLoader<StructObjectInfo> {

    private final StructItemLoader structItemLoader;

    private final RuleSystem ruleSystem;

    public StructObjectInfoLoader(EntityManager em, int batchSize, RuleSystem ruleSystem) {
        super(ArrStructureData.class, ArrStructureData.STRUCTURE_DATA_ID, em, batchSize);
        this.structItemLoader = new StructItemLoader(em, batchSize);
        this.ruleSystem = Validate.notNull(ruleSystem);
    }

    @Override
    protected void onBatchEntryLoad(LoadDispatcher<StructObjectInfo> dispatcher, StructObjectInfo result) {
        StructItemDispatcher itemDispatcher = new StructItemDispatcher(result, dispatcher);
        structItemLoader.addRequest(result.getId(), itemDispatcher);
    }

    @Override
    protected StructObjectInfo createResult(Object entity) {
        ArrStructureData structObj = (ArrStructureData) entity;

        RulStructureType structType = ruleSystem.getStructuredTypeById(structObj.getStructureTypeId());
        return new StructObjectInfo(structObj.getStructureDataId(), structType);
    }

    @Override
    public void flush() {
        super.flush();
        structItemLoader.flush();
    }
}
