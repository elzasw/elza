package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.ArrStructureItem;

public class ContextStructObject {

    private final Map<ItemKey, Integer> structItemCount = new HashMap<>();

    private final ContextSection section;

    private final EntityIdHolder<ArrStructureData> idHolder;

    private final StructObjectStorageDispatcher storageDispatcher;

    public ContextStructObject(ContextSection section,
                               EntityIdHolder<ArrStructureData> idHolder,
                               StructObjectStorageDispatcher storageDispatcher) {
        this.section = Validate.notNull(section);
        this.idHolder = Validate.notNull(idHolder);
        this.storageDispatcher = storageDispatcher;
    }

    public EntityIdHolder<ArrStructureData> getIdHolder() {
        return idHolder;
    }

    public void addStructItem(ArrStructureItem structItem, ArrData data) {
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

    public void addStructItem(ArrStructureItem structItem, String refStructObjectImportId) {
        ArrStructItemWrapper structItemWrapper = createItemWrapper(structItem);
        // create data without reference
        ArrDataStructureRef data = new ArrDataStructureRef();
        data.setDataType(structItem.getItemType().getDataType());
        // create delayed data wrapper
        DelayedStructObjectRefWrapper dataWrapper = new DelayedStructObjectRefWrapper(data, refStructObjectImportId, section);
        structItemWrapper.setDataIdHolder(dataWrapper.getIdHolder());
        storageDispatcher.addDelayedStructItem(structItemWrapper, dataWrapper);
    }

    private ArrStructItemWrapper createItemWrapper(ArrStructureItem structItem) {
        Validate.isTrue(structItem.isUndefined());
        // set item position
        Integer count = structItemCount.compute(ItemKey.of(structItem), (k, v) -> v == null ? 1 : ++v);
        structItem.setPosition(count);
        // store item & data
        return new ArrStructItemWrapper(structItem, idHolder);
    }
}
