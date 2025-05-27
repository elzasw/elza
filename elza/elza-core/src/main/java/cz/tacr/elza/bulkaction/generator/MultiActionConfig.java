package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkActionTransactional;
import cz.tacr.elza.bulkaction.generator.multiple.ActionConfig;

public class MultiActionConfig extends BaseActionConfig {

	List<ActionConfig> actions;

	public List<ActionConfig> getActions() {
		return actions;
	}

	public void setActions(List<ActionConfig> actions) {
		this.actions = actions;
	}

	@Override
	public BulkActionTransactional createBulkAction() {
		MultipleBulkAction mba = new MultipleBulkAction(this);
		return mba;
	}

}
