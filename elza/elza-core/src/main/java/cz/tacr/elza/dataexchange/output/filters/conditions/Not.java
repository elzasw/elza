package cz.tacr.elza.dataexchange.output.filters.conditions;

import cz.tacr.elza.dataexchange.output.filters.FilterRuleContext;
import cz.tacr.elza.service.cache.CachedAccessPoint;

public class Not implements EntityCondition {
	
	protected EntityCondition not;
	
	public Not() {	
	}
	
	public Not(final EntityCondition not) {
		this.not = not;
	}
	
	public boolean isTrue(FilterRuleContext frCtx, CachedAccessPoint ap) {
		return !not.isTrue(frCtx, ap); 
	}

	public EntityCondition getNot() {
		return not;
	}

	public void setNot(EntityCondition not) {
		this.not = not;
	}
	
}
