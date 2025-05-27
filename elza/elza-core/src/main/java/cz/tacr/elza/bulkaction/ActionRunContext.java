package cz.tacr.elza.bulkaction;

import java.util.List;
import java.util.Objects;

import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;

/**
 * Context data for running action
 */
public class ActionRunContext {

	final List<Integer> inputNodeIds;

	final ArrBulkActionRun bulkActionRun;

	public ActionRunContext(List<Integer> inputNodeIds, ArrBulkActionRun bulkActionRun) {
		this.inputNodeIds = inputNodeIds;
		this.bulkActionRun = bulkActionRun;
	}

	public List<Integer> getInputNodeIds() {
		return inputNodeIds;
	}

	public ArrBulkActionRun getBulkActionRun() {
		return bulkActionRun;
	}

    public Integer getFundVersionId() {
        return bulkActionRun.getFundVersionId();
    }

	public ArrChange getChange() {
		ArrChange change = bulkActionRun.getChange();
		Objects.requireNonNull(change);
		return change;
	}
}
