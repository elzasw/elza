package cz.tacr.elza.dataexchange.output.sections;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ArrStructuredItem;

public class StructItemDispatcher extends NestedLoadDispatcher<ArrStructuredItem> {

    private final StructObjectInfoImpl structObjectInfo;

    StructItemDispatcher(StructObjectInfoImpl structObjectInfo, LoadDispatcher<StructObjectInfoImpl> structObjectDispatcher) {
        super(structObjectDispatcher);
        this.structObjectInfo = structObjectInfo;
    }

    @Override
    public void onLoad(ArrStructuredItem result) {
        structObjectInfo.addItem(result);
    }

    @Override
    public void onCompleted() {
    }
}
