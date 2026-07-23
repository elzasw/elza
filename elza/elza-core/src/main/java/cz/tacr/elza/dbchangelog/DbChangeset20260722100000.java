package cz.tacr.elza.dbchangelog;

import cz.tacr.elza.service.StartupService;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;

public class DbChangeset20260722100000 extends BaseTaskChange {

	@Override
	public void execute(Database database) throws CustomChangeException {
		// one-time validation of inhibited items, runs during startup
		StartupService.inhibitedItemsCleanup = true;
	}
}
