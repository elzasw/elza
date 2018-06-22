package cz.tacr.elza.dataexchange.output.sections;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulStructuredType;

public class StructObjectInfoLoader extends AbstractEntityLoader<StructObjectInfoImpl> {

    private final StructItemLoader structItemLoader;

    private final RuleSystem ruleSystem;

    public StructObjectInfoLoader(EntityManager em, int batchSize, RuleSystem ruleSystem) {
        super(ArrStructuredObject.class, ArrStructuredObject.STRUCTURED_OBJECT_ID, em, batchSize);
        this.structItemLoader = new StructItemLoader(em, batchSize);
        this.ruleSystem = Validate.notNull(ruleSystem);
    }

    @Override
    protected void onBatchEntryLoad(LoadDispatcher<StructObjectInfoImpl> dispatcher, StructObjectInfoImpl result) {
        StructItemDispatcher itemDispatcher = new StructItemDispatcher(result, dispatcher);
        structItemLoader.addRequest(result.getId(), itemDispatcher);
    }

    @Override
    protected StructObjectInfoImpl createResult(Object entity) {
        ArrStructuredObject structObj = (ArrStructuredObject) entity;

        RulStructuredType structType = ruleSystem.getStructuredTypeById(structObj.getStructuredTypeId());
        return new StructObjectInfoImpl(structObj.getStructuredObjectId(), structType);
    }

    @Override
    public void flush() {
        super.flush();
        structItemLoader.flush();
    }
}
