package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkAction;
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
	public BulkAction createBulkAction() {
		MultipleBulkAction mba = new MultipleBulkAction(this);
		return mba;
	}

}
