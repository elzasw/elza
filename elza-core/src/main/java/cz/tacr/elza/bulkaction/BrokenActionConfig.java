package cz.tacr.elza.bulkaction;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

public class BrokenActionConfig extends BaseActionConfig {
	Exception innerException;

	BrokenActionConfig(Exception innerException) {
		this.innerException = innerException;
		name = "Broken action";
		description = "Broken action, reload newer version of source package";
		ruleCode = "";
		codeTypeBulkAction = "";
	}

	@Override
	public BulkAction createBulkAction() {
		throw new BusinessException("Broken action, try to upgrade package with action", innerException,
		        BaseCode.SYSTEM_ERROR);
	}
}
