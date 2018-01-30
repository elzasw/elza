package cz.tacr.elza.dataexchange.output.sections;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ArrStructureItem;

public class StructItemDispatcher extends NestedLoadDispatcher<ArrStructureItem> {

    private final StructObjectInfo structObjectInfo;

    StructItemDispatcher(StructObjectInfo structObjectInfo, LoadDispatcher<StructObjectInfo> structObjectDispatcher) {
        super(structObjectDispatcher);
        this.structObjectInfo = structObjectInfo;
    }

    @Override
    public void onLoad(ArrStructureItem result) {
        structObjectInfo.addItem(result);
    }

    @Override
    public void onCompleted() {
    }
}
