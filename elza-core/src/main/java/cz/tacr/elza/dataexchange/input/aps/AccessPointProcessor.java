package cz.tacr.elza.dataexchange.input.aps;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.schema.v2.AccessPoint;

/**
 * Processing access points. Implementation is not thread-safe.
 */
public class AccessPointProcessor extends AccessPointEntryProcessor {

    public AccessPointProcessor(ImportContext context) {
        super(context, false);
    }

    @Override
    public void process(Object item) {
        AccessPoint ap = (AccessPoint) item;
        processEntry(ap.getApe());
        // whole AP processed
        info.onProcessed();
    }
}
