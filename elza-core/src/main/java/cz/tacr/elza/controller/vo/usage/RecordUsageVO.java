package cz.tacr.elza.controller.vo.usage;

import java.util.List;

public class RecordUsageVO {

	private List<FundVO> funds;

	private List<PartyVO> parties;

	public RecordUsageVO(final List<FundVO> funds, final List<PartyVO> parties) {
		this.funds = funds;
		this.parties = parties;
	}

	public List<FundVO> getFunds() {
		return funds;
	}

	public List<PartyVO> getParties() {
		return parties;
	}
}
