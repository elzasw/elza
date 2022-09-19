package cz.tacr.elza.dataexchange.output.filters;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.sections.StructObjectInfoImpl;

public class SoiLoadDispatcher implements LoadDispatcher<StructObjectInfoImpl> {

    private StructObjectInfoImpl result;

    @Override
    public void onLoadBegin() {
    }

    @Override
    public void onLoad(StructObjectInfoImpl result) {
        this.result = result;
    }

    @Override
    public void onLoadEnd() {
    }

    public StructObjectInfoImpl getResult() {
        return result;
    }

}
