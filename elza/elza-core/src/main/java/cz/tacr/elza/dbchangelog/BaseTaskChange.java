package cz.tacr.elza.dbchangelog;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * Base implementation for custom DB change task
 * 
 * @author pyta
 *
 */
public abstract class BaseTaskChange implements CustomTaskChange {

    @Override
    public String getConfirmationMessage() {
        // Not needed
        return null;
    }

    @Override
    public void setUp() throws SetupException {
        // Not needed

    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // Not needed

    }

    @Override
    public ValidationErrors validate(Database database) {
        // Not needed
        return null;
    }
}
