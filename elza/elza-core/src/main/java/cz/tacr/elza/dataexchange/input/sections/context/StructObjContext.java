package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;

public class StructObjContext {

    private final Map<ItemKey, Integer> structItemCount = new HashMap<>();

    private final SectionContext sectionCtx;

    private final EntityIdHolder<ArrStructuredObject> idHolder;

    private final StructObjStorageDispatcher storageDispatcher;

    public StructObjContext(SectionContext sectionCtx,
            EntityIdHolder<ArrStructuredObject> idHolder,
            StructObjStorageDispatcher storageDispatcher) {
        this.sectionCtx = Validate.notNull(sectionCtx);
        this.idHolder = Validate.notNull(idHolder);
        this.storageDispatcher = storageDispatcher;
    }

    public EntityIdHolder<ArrStructuredObject> getIdHolder() {
        return idHolder;
    }

    public void addStructItem(ArrStructuredItem structItem, ArrData data) {
        ArrStructItemWrapper structItemWrapper = createItemWrapper(structItem);
        if (data == null) {
            storageDispatcher.addStructItem(structItemWrapper);
        } else {
            Validate.isTrue(data.getDataType() == structItem.getItemType().getDataType());
            ArrDataWrapper dataWrapper = new ArrDataWrapper(data);
            structItemWrapper.setDataIdHolder(dataWrapper.getIdHolder());
            storageDispatcher.addStructItem(structItemWrapper, dataWrapper);
        }
    }

    /**
     * Method is used when referenced structured object is not processed yet.
     */
    public void addStructObjRef(ArrStructuredItem entity, String structObjImportId) {
        ArrStructItemWrapper structItemWrapper = createItemWrapper(entity);
        // create data without reference
        ArrDataStructureRef data = new ArrDataStructureRef();
        data.setDataType(entity.getItemType().getDataType());
        // create data wrapper for reference
        ArrDataStructureRefWrapper dataWrapper = new ArrDataStructureRefWrapper(data, structObjImportId, sectionCtx);
        structItemWrapper.setDataIdHolder(dataWrapper.getIdHolder());
        storageDispatcher.addStructObjRef(structItemWrapper, dataWrapper);
    }

    private ArrStructItemWrapper createItemWrapper(ArrStructuredItem structItem) {
        Validate.isTrue(structItem.isUndefined());
        // set item position
        Integer count = structItemCount.compute(ItemKey.of(structItem), (k, v) -> v == null ? 1 : ++v);
        structItem.setPosition(count);
        // store item & data
        return new ArrStructItemWrapper(structItem, idHolder);
    }
}
