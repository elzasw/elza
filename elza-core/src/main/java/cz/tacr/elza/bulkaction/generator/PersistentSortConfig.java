package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkActionTransactional;

public class PersistentSortConfig extends BaseActionConfig {

    @Override
    public BulkActionTransactional createBulkAction() {
        return new PersistentSortBulkAction(this);
    }
}
