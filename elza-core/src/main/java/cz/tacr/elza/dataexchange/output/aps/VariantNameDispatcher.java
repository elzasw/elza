package cz.tacr.elza.dataexchange.output.aps;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.ApVariantRecord;

public class VariantNameDispatcher implements LoadDispatcher<ApVariantRecord> {

    private final List<ApVariantRecord> variantNames = new ArrayList<>();

    private final ApRecord ap;

    public VariantNameDispatcher(ApRecord ap) {
        this.ap = ap;
    }

    @Override
    public void onLoadBegin() {
    }

    @Override
    public void onLoad(ApVariantRecord result) {
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
