package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrDescItem.FIELD_ITEM_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_NODE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.SPECIFICATION_ATT;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_DESC_ITEM_TYPE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_CREATE_CHANGE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_DELETE_CHANGE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;
import static cz.tacr.elza.domain.ArrDescItem.INTGER_ATT;
import static cz.tacr.elza.domain.ArrDescItem.DECIMAL_ATT;
import static cz.tacr.elza.domain.ArrDescItem.NORMALIZED_FROM_ATT;
import static cz.tacr.elza.domain.ArrDescItem.NORMALIZED_TO_ATT;
import static cz.tacr.elza.domain.ArrItem.FIELD_DATA;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;

import cz.tacr.elza.domain.ArrDescItem;

public class ArrDescItemBinder implements TypeBinder {

    private TypeBindingContext context;

    @Override
    public void bind(TypeBindingContext context) {
    	this.context = context;

    	// při změně pole data přepočti index
        context.dependencies().use(FIELD_DATA);

        createIntegerField(FIELD_ITEM_ID);
        createIntegerField(FIELD_NODE_ID);
        createIntegerField(FIELD_FUND_ID);

        createIntegerField(SPECIFICATION_ATT); // itemSpecId
        createIntegerField(FIELD_DESC_ITEM_TYPE_ID);
        createIntegerField(FIELD_CREATE_CHANGE_ID);
        createIntegerField(FIELD_DELETE_CHANGE_ID);

        createAnalyzedField(FULLTEXT_ATT);

        createIntegerField(INTGER_ATT);
        createDoubleField(DECIMAL_ATT);
        createLongField(NORMALIZED_FROM_ATT);
        createLongField(NORMALIZED_TO_ATT);

        context.bridge(ArrDescItem.class, new ArrDescItemBridge());
    }

    private IndexFieldReference<String> createAnalyzedField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asString().analyzer("cz"))
        		.multiValued()
        		.toReference();
    }

    private IndexFieldReference<Integer> createIntegerField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asInteger())
        		.multiValued()
        		.toReference();
    }

    private IndexFieldReference<Double> createDoubleField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asDouble())
        		.multiValued()
        		.toReference();
    }

    private IndexFieldReference<Long> createLongField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asLong())
        		.multiValued()
        		.toReference();
    }
}
