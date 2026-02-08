package cz.tacr.elza.dataexchange.output.filters.conditions;

import cz.tacr.elza.dataexchange.output.filters.FilterRuleContext;
import cz.tacr.elza.service.cache.CachedAccessPoint;

/**
 * Condition to validate entity class
 */
public class EntityClass implements EntityCondition {
	/** 
	 * Expected entity class
	 */
	protected String classType;

	public String getClassType() {
		return classType;
	}

	public void setClassType(String classType) {
		this.classType = classType;
	}

	@Override
	public boolean isTrue(FilterRuleContext frCtx, CachedAccessPoint ap) {
		var apType = frCtx.getStaticDataProvider().getApTypeById(ap.getApState().getApTypeId());

		return classType.equals(apType.getCode());
	}	
}
