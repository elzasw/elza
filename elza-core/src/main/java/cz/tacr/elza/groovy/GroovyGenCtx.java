package cz.tacr.elza.groovy;

import java.util.List;

public class GroovyGenCtx {

	private final GroovyFund fund;

	private final GroovyAe groovyAe;

	private final List<GroovyItem> items;

	public GroovyGenCtx(GroovyFund fund, GroovyAe groovyAe, List<GroovyItem> items) {
		this.fund = fund;
		this.groovyAe = groovyAe;
		this.items = items;
	}

	public GroovyFund getFund() {
		return fund;
	}

	public GroovyAe getGroovyAe() {
		return groovyAe;
	}

	public List<GroovyItem> getItems() {
		return items;
	}

}
