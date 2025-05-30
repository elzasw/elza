package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrCachedNode.DATA;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;
import static cz.tacr.elza.domain.ArrDescItem.NORM_FROM;
import static cz.tacr.elza.domain.ArrDescItem.NORM_TO;
import static cz.tacr.elza.domain.ArrDescItem.REL_AP_ID;
import static cz.tacr.elza.domain.bridge.LuceneAnalyzerConfigurer.CLASSIC_TOKENIZER_CZ;

import java.util.Objects;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.service.SpringContext;

public class ArrCachedNodeBinder implements TypeBinder {

    private IndexConfigReader configurationReader = SpringContext.getBean(IndexConfigReader.class);

    private TypeBindingContext context;

	@Override
	public void bind(TypeBindingContext context) {
		this.context = context;

		// při změně pole data přepočti index
        context.dependencies().use(DATA);

        createIntField(FIELD_FUND_ID);

        // pro type RECORD_REF
        createIntField(REL_AP_ID);

        context.indexSchemaElement()
			.field(FULLTEXT_ATT, f -> f.asString().analyzer(CLASSIC_TOKENIZER_CZ))
			.multiValued()
			.toReference();

        // item type codes
        for (String itemTypeCode : configurationReader.getItemTypeCodes()) {
        	DataType dataType = configurationReader.getDataTypeByItemTypeCode(itemTypeCode);
        	Objects.requireNonNull(dataType);
    		switch (dataType) {
    		case INT:
    	        createIntField(itemTypeCode.toLowerCase());
    	        break;
    		case ENUM:
				createStringField(itemTypeCode.toLowerCase());
				break;
    		case RECORD_REF:
    		case STRUCTURED:
			case STRING:
			case TEXT:
				createStringField(itemTypeCode.toLowerCase());
				// added field itemType_itemSpec -> value
	            for (String itemSpecCode : configurationReader.getItemSpecCodesByTypeCode(itemTypeCode)) {
					createStringField(itemTypeCode.toLowerCase() + "_" + itemSpecCode.toLowerCase());
	            }
				break;
			case UNITDATE:
				createLongField(itemTypeCode.toLowerCase() + "_" + NORM_FROM);
				createLongField(itemTypeCode.toLowerCase() + "_" + NORM_TO);
				break;
    		}
        }

        context.bridge(ArrCachedNode.class, new ArrCachedNodeBridge());
	}

    private IndexFieldReference<String> createStringField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asString())
        		.multiValued()
        		.toReference();
    }    

    private IndexFieldReference<Long> createLongField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asLong())
        		.multiValued()
        		.toReference();
    }    

    private IndexFieldReference<Integer> createIntField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asInteger())
        		.multiValued()
        		.toReference();
    }    
}
