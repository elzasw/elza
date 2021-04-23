package cz.tacr.elza.dbchangelog;

import cz.tacr.elza.service.StartupService;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;

public class DbChangeSet20210419133000 extends BaseTaskChange {

    @Override
    public void execute(Database database) throws CustomChangeException {
        StartupService.fullTextReindex = true;
    }

}
