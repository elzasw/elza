package cz.tacr.elza.controller.vo.usage;

import java.util.List;

public class RecordUsageVO {
	public List<FundVO> funds;

    public List<PartyVO> parties;

	public RecordUsageVO() {
    }

	public RecordUsageVO(final List<FundVO> funds, final List<PartyVO> parties) {
		this.funds = funds;
		this.parties = parties;
	}

}
