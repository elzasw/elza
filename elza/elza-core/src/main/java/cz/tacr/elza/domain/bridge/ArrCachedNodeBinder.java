package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrCachedNode.DATA;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;

import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;

import cz.tacr.elza.domain.ArrCachedNode;

public class ArrCachedNodeBinder implements TypeBinder {

	@Override
	public void bind(TypeBindingContext context) {

		// při změně pole data přepočti index
        context.dependencies().use(DATA);

        context.indexSchemaElement()
        	.field(FIELD_FUND_ID, f -> f.asInteger())
        	.multiValued()
        	.toReference();

        context.indexSchemaElement()
			.field(FULLTEXT_ATT, f -> f.asString().analyzer("cz"))
			.multiValued()
			.toReference();

        context.bridge(ArrCachedNode.class, new ArrCachedNodeBridge());
	}
}
