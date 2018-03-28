package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkAction;

/**
 * @author <a href="mailto:jiri.vanek@marbes.cz">Jiří Vaněk</a>
 */
public class PersistentSortConfig extends BaseActionConfig {

    @Override
    public BulkAction createBulkAction() {
        return new PersistentSortBulkAction(this);
    }
}
