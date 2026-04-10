package cz.tacr.elza.dataexchange.output.filters.conditions;

import java.util.List;

import cz.tacr.elza.dataexchange.output.filters.FilterRuleContext;
import cz.tacr.elza.service.cache.CachedAccessPoint;

public class And implements EntityCondition {
	protected List<EntityCondition> conditions;
	
	public And() {
		
	}
	
	public And(final List<EntityCondition> conditions) {
		this.conditions = conditions;
	}

	public boolean isTrue(FilterRuleContext frCtx, CachedAccessPoint ap) {
		for(EntityCondition condition : conditions) { 
			if (!condition.isTrue(frCtx, ap)) 
				return false; 
		}
		return true;
	}
}
