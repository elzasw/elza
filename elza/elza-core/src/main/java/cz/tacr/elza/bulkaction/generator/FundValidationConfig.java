package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkActionTransactional;

/**
 * Configuration for FundValidation
 * 
 */
public class FundValidationConfig extends BaseActionConfig {


	@Override
	public BulkActionTransactional createBulkAction() {
		return new FundValidation();
	}

}
