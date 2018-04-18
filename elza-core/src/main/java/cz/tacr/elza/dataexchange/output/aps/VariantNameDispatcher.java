package cz.tacr.elza.dataexchange.output.aps;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApName;

public class VariantNameDispatcher implements LoadDispatcher<ApName> {

    private final List<ApName> variantNames = new ArrayList<>();

    private final ApAccessPoint ap;

    public VariantNameDispatcher(ApAccessPoint ap) {
        this.ap = ap;
    }

    @Override
    public void onLoadBegin() {
    }

    @Override
    public void onLoad(ApName result) {
        result.setPreferredName(false);
        variantNames.add(result);
    }

    @Override
    public void onLoadEnd() {
        if (variantNames.isEmpty()) {
            ap.setNameList(null);
        } else {
            ap.setNameList(variantNames);
        }
    }
}
