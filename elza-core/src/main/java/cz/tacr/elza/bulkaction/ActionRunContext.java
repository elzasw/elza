package cz.tacr.elza.bulkaction;

import java.util.List;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
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

    /**
     * Return fund version
     * 
     * @return
     */
	public ArrFundVersion getFundVersion() {
		ArrFundVersion fundVersion = bulkActionRun.getFundVersion();
		Validate.notNull(fundVersion);
		return fundVersion;
	}

    /**
     * Return fund
     * 
     * @return
     */
    public ArrFund getFund() {
        ArrFundVersion fundVersion = bulkActionRun.getFundVersion();
        Validate.notNull(fundVersion);
        ArrFund fund = fundVersion.getFund();
        Validate.notNull(fund);
        return fund;

    }

	public ArrChange getChange() {
		ArrChange arrChange = bulkActionRun.getChange();
		Validate.notNull(arrChange);
		return arrChange;
	}
}
