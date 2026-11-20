package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkActionTransactional;
import cz.tacr.elza.bulkaction.generator.multiple.ActionConfig;

public class MultiActionConfig extends BaseActionConfig {

	List<ActionConfig> actions;

	/**
	 * See {@link #isApplyOutputFilter()}
	 */
	boolean applyOutputFilter = false;

	@Override
	public boolean isApplyOutputFilter() {
		return applyOutputFilter;
	}

	public void setApplyOutputFilter(boolean applyOutputFilter) {
		this.applyOutputFilter = applyOutputFilter;
	}

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
