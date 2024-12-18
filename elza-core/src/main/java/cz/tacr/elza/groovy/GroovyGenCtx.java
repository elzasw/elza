package cz.tacr.elza.groovy;

import java.util.List;

public class GroovyGenCtx {

	private final GroovyFund fund;

	private final List<Integer> nodeIds;
	
	public GroovyGenCtx(GroovyFund fund, List<Integer> nodeIds) {
		this.fund = fund;
		this.nodeIds = nodeIds;
	}

	public GroovyFund getFund() {
		return fund;
	}

	public List<Integer> getNodeIds() {
		return nodeIds;
	}

}
