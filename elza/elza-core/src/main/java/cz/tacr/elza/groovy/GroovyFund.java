package cz.tacr.elza.groovy;

import cz.tacr.elza.domain.ArrFund;

public class GroovyFund {

	private final ArrFund fund;

	public GroovyFund(ArrFund fund) {
		this.fund = fund;
	}

	public String getName() {
		return fund.getName();
	}

	public String getMark() {
		return fund.getMark();
	}
}
