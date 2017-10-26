package cz.tacr.elza.dataexchange.output.aps;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegVariantRecord;

public class VariantNameDispatcher implements LoadDispatcher<RegVariantRecord> {

    private final List<RegVariantRecord> variantNames = new ArrayList<>();

    private final RegRecord ap;

    public VariantNameDispatcher(RegRecord ap) {
        this.ap = ap;
    }

    @Override
    public void onLoadBegin() {
    }

    @Override
    public void onLoad(RegVariantRecord result) {
        variantNames.add(result);
    }

    @Override
    public void onLoadEnd() {
        if (variantNames.isEmpty()) {
            ap.setVariantRecordList(null);
        } else {
            ap.setVariantRecordList(variantNames);
        }
    }
}
