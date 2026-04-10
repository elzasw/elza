package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrDescItem.FIELD_ITEM_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_NODE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_ITEM_SPEC;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_ITEM_SPEC_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_DESC_ITEM_TYPE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_CREATE_CHANGE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_DELETE_CHANGE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_DELETE_CHANGE;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;
import static cz.tacr.elza.domain.ArrDescItem.INTEGER_ATT;
import static cz.tacr.elza.domain.ArrDescItem.DECIMAL_ATT;
import static cz.tacr.elza.domain.ArrDescItem.NORM_FROM;
import static cz.tacr.elza.domain.ArrDescItem.NORM_TO;
import static cz.tacr.elza.domain.ArrDescItem.REL_AP_ID;
import static cz.tacr.elza.domain.ArrItem.FIELD_DATA;
import static cz.tacr.elza.domain.bridge.LuceneAnalyzerConfigurer.KEYWORD_TOKENIZER_CZ;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.domain.ArrDescItem;

public class ArrDescItemBinder implements TypeBinder {

    private final static Logger log = LoggerFactory.getLogger(ArrDescItemBinder.class);

    private TypeBindingContext context;

    @Override
    public void bind(TypeBindingContext context) {
		log.debug("Bind ArrDescItemBinder");

		this.context = context;

    	// při změně pole data, itemSpec nebo deleteChange přepočti index
        context.dependencies().use(FIELD_DATA).use(FIELD_ITEM_SPEC).use(FIELD_DELETE_CHANGE);

        createIntegerField(FIELD_ITEM_ID);
        createIntegerField(FIELD_NODE_ID);
        createIntegerField(FIELD_FUND_ID);

        createIntegerField(FIELD_ITEM_SPEC_ID);
        createIntegerField(FIELD_DESC_ITEM_TYPE_ID);
        createIntegerField(FIELD_CREATE_CHANGE_ID);
        createIntegerField(FIELD_DELETE_CHANGE_ID);

        createAnalyzedField(FULLTEXT_ATT);

        createIntegerField(INTEGER_ATT);
        createDoubleField(DECIMAL_ATT);
        createLongField(NORM_FROM);
        createLongField(NORM_TO);

        createIntegerField(REL_AP_ID);

        context.bridge(ArrDescItem.class, new ArrDescItemBridge());
    }

    private IndexFieldReference<String> createAnalyzedField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asString().analyzer(KEYWORD_TOKENIZER_CZ))
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
