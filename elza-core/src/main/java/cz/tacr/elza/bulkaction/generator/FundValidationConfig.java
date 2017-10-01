package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkAction;

/**
 * Configuration for FundValidation
 * 
 */
public class FundValidationConfig extends BaseActionConfig {


	@Override
	public BulkAction createBulkAction() {
		return new FundValidation();
	}

}
