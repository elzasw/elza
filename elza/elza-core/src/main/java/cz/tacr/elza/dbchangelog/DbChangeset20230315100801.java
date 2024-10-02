package cz.tacr.elza.dbchangelog;

import cz.tacr.elza.domain.bridge.IndexConfigReaderImpl;
import cz.tacr.elza.service.StartupService;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;

public class DbChangeset20230315100801 extends BaseTaskChange {

	@Override
	public void execute(Database database) throws CustomChangeException {
        // nastavení příznaků pro úplnou aktualizaci indexových souborů
		IndexConfigReaderImpl.cleanIndexDir = true;
		StartupService.fullTextReindex = true;
    }
}
