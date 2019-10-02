package cz.tacr.elza.bulkaction;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

public class BrokenActionConfig extends BaseActionConfig {

    private final Exception innerException;

    BrokenActionConfig(Exception innerException) {
        this.innerException = innerException;
        this.name = "Broken action";
        this.description = "Broken action, reload newer version of source package";
    }

    @Override
    public BulkAction createBulkAction() {
        throw new BusinessException("Broken action, try to upgrade package with action", innerException, BaseCode.SYSTEM_ERROR);
    }
}
