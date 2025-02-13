package cz.tacr.elza.core.data;

import cz.tacr.elza.domain.RulItemSpec;

/**
 * Copy of RulItemSpec with caching.
 */
public class CachedItemSpec extends RulItemSpec {
	public CachedItemSpec(RulItemSpec src) {
		super(src);
	}
}
