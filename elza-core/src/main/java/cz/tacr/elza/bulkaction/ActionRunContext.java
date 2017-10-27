package cz.tacr.elza.bulkaction;

import java.util.List;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;

/**
 * Context data for running action
 * 
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

	public ArrFundVersion getFundVersion() {
		ArrFundVersion fundVersion = bulkActionRun.getFundVersion();
		Validate.notNull(fundVersion);
		return fundVersion;
	}

	public ArrChange getChange() {
		ArrChange arrChange = bulkActionRun.getChange();
		Validate.notNull(arrChange);
		return arrChange;
	}
}
