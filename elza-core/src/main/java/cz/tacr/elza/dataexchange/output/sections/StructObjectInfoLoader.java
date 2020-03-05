package cz.tacr.elza.dataexchange.output.sections;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ArrStructuredObject;

public class StructObjectInfoLoader extends AbstractEntityLoader<StructObjectInfoImpl, ArrStructuredObject> {

    private final StructItemLoader structItemLoader;

    private final StaticDataProvider staticData;

    public StructObjectInfoLoader(EntityManager em, int batchSize, StaticDataProvider staticData) {
        super(ArrStructuredObject.class, ArrStructuredObject.FIELD_STRUCTURED_OBJECT_ID, em, batchSize);
        this.structItemLoader = new StructItemLoader(em, batchSize);
        this.staticData = Validate.notNull(staticData);
    }

    @Override
    protected void onBatchEntryLoad(LoadDispatcher<StructObjectInfoImpl> dispatcher, StructObjectInfoImpl result) {
        StructItemDispatcher itemDispatcher = new StructItemDispatcher(result, dispatcher);
        structItemLoader.addRequest(result.getId(), itemDispatcher);
    }

    @Override
    protected StructObjectInfoImpl createResult(Object entity) {
        ArrStructuredObject structObj = (ArrStructuredObject) entity;

        StructType structType = staticData.getStructuredTypeById(structObj.getStructuredTypeId());
        return new StructObjectInfoImpl(structObj.getStructuredObjectId(), structType.getStructuredType());
    }

    @Override
    public void flush() {
        super.flush();
        structItemLoader.flush();
    }
}
