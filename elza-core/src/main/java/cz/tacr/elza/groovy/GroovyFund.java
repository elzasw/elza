package cz.tacr.elza.groovy;

import cz.tacr.elza.domain.ArrFund;

public class GroovyFund {

	private final String name;

	private final String mark;

	private final Integer fundNumber;

	private final GroovyInstitution institution;

	public GroovyFund(ArrFund fund, GroovyInstitution institution) {
		this.name = fund.getName();
		this.mark = fund.getMark();
		this.fundNumber = fund.getFundNumber();
		this.institution = institution;
	}

	public String getName() {
		return name;
	}

	public String getMark() {
		return mark;
	}

	public Integer getNumber() {
		return fundNumber;
	}

	GroovyInstitution getInstitution() {
		return institution;
	}
}
