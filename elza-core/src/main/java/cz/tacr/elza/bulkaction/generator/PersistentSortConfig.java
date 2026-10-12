package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkActionTransactional;

public class PersistentSortConfig extends BaseActionConfig {

    /** Kind of the persistent sort, see {@link BulkActionConfig#getType()}. */
    public static final String TYPE = "PersistentSort";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public BulkActionTransactional createBulkAction() {
        return new PersistentSortBulkAction(this);
    }
}
