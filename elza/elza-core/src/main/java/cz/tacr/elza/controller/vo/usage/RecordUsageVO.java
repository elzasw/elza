package cz.tacr.elza.controller.vo.usage;

import java.util.List;

public class RecordUsageVO {
    private List<FundVO> funds;

	public RecordUsageVO() {
    }

	public RecordUsageVO(final List<FundVO> funds) {
		this.funds = funds;
	}

    public List<FundVO> getFunds() {
        return funds;
    }

    public void setFunds(List<FundVO> funds) {
        this.funds = funds;
    }

}
