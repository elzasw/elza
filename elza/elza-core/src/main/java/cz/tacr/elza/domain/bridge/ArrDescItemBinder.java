package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrDescItem.FIELD_ITEM_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_NODE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.SPECIFICATION_ATT;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_DESC_ITEM_TYPE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_CREATE_CHANGE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;
import static cz.tacr.elza.domain.ArrDescItem.INTGER_ATT;
import static cz.tacr.elza.domain.ArrItem.FIELD_DATA;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;

import cz.tacr.elza.domain.ArrDescItem;

public class ArrDescItemBinder implements TypeBinder {

    private TypeBindingContext context;

    @Override
    public void bind(TypeBindingContext context) {
    	this.context = context;
        Map<String, IndexFieldReference<String>> fields = new HashMap<>();

    	// při změně pole data přepočti index
        context.dependencies().use(FIELD_DATA);

        fields.put(FIELD_ITEM_ID, createNotAnalyzedField(FIELD_ITEM_ID));
        fields.put(FIELD_NODE_ID, createNotAnalyzedField(FIELD_NODE_ID));
        fields.put(FIELD_FUND_ID, createNotAnalyzedField(FIELD_FUND_ID));

        fields.put(SPECIFICATION_ATT, createNotAnalyzedField(SPECIFICATION_ATT));
        fields.put(FIELD_DESC_ITEM_TYPE_ID, createNotAnalyzedField(FIELD_DESC_ITEM_TYPE_ID));
        fields.put(FIELD_CREATE_CHANGE_ID, createNotAnalyzedField(FIELD_CREATE_CHANGE_ID));

        fields.put(FULLTEXT_ATT, createAnalyzedField(FULLTEXT_ATT));

        createIntegerField(INTGER_ATT);

        context.bridge(ArrDescItem.class, new ArrDescItemBridge(fields));
    }

    private IndexFieldReference<String> createAnalyzedField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asString().analyzer("cz"))
        		.multiValued()
        		.toReference();
    }

    private IndexFieldReference<String> createNotAnalyzedField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asString())
        		.multiValued()
        		.toReference();
    }

    private IndexFieldReference<Integer> createIntegerField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asInteger())
        		.multiValued()
        		.toReference();
    }
}
