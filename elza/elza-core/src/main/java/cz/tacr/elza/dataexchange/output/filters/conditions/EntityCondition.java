package cz.tacr.elza.dataexchange.output.filters.conditions;

import cz.tacr.elza.dataexchange.output.filters.FilterRuleContext;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.service.cache.CachedAccessPoint;

public interface EntityCondition {

	boolean isTrue(FilterRuleContext frCtx, CachedAccessPoint ap);
}
