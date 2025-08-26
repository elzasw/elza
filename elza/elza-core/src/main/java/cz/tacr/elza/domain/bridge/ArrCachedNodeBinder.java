package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrCachedNode.DATA;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;
import static cz.tacr.elza.domain.ArrDescItem.NORM_FROM;
import static cz.tacr.elza.domain.ArrDescItem.NORM_TO;
import static cz.tacr.elza.domain.ArrDescItem.REL_AP_ID;
import static cz.tacr.elza.domain.bridge.LuceneAnalyzerConfigurer.CLASSIC_TOKENIZER_CZ;
import static cz.tacr.elza.domain.bridge.LuceneAnalyzerConfigurer.KEYWORD_TOKENIZER_CZ;

import java.math.BigDecimal;
import java.util.Objects;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.service.SpringContext;

public class ArrCachedNodeBinder implements TypeBinder {
	
    private final static Logger log = LoggerFactory.getLogger(ArrCachedNodeBinder.class);

    public static final String CONFORMITY_ERROR = "conformityError";
	public static final String CONFORMITY_MISSING = "conformityMissing";
	public static final String UUID = "uuid";

    private IndexConfigReader configurationReader = SpringContext.getBean(IndexConfigReader.class);

    private TypeBindingContext context;

	@Override
	public void bind(TypeBindingContext context) {
		log.debug("Bind ArrCachedNodeBinder");

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
    		case DECIMAL:
    	        createBigDecimalField(itemTypeCode.toLowerCase());
				// added field itemType_itemSpec -> value
	            for (String itemSpecCode : configurationReader.getItemSpecCodesByTypeCode(itemTypeCode)) {
	            	createBigDecimalField(itemTypeCode.toLowerCase() + "_" + itemSpecCode.toLowerCase());
	            }
    	        break;
    		case ENUM:
				createStringField(itemTypeCode.toLowerCase());
				break;
    		case RECORD_REF:
				createIntField(itemTypeCode.toLowerCase());
				// added field itemType_itemSpec -> value
	            for (String itemSpecCode : configurationReader.getItemSpecCodesByTypeCode(itemTypeCode)) {
					createIntField(itemTypeCode.toLowerCase() + "_" + itemSpecCode.toLowerCase());
	            }
				break;
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
				// added field itemType_itemSpec -> value
	            for (String itemSpecCode : configurationReader.getItemSpecCodesByTypeCode(itemTypeCode)) {
	            	createLongField(itemTypeCode.toLowerCase() + "_" + itemSpecCode.toLowerCase() + "_" + NORM_FROM);
	            	createLongField(itemTypeCode.toLowerCase() + "_" + itemSpecCode.toLowerCase() + "_" + NORM_TO);
	            }
				break;
    		}
        }

		createStringField(CONFORMITY_ERROR);
		createStringField(CONFORMITY_MISSING);
		createStringField(UUID);

        context.bridge(ArrCachedNode.class, new ArrCachedNodeBridge());
	}

    private IndexFieldReference<String> createStringField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asString().analyzer(KEYWORD_TOKENIZER_CZ))
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

    private IndexFieldReference<BigDecimal> createBigDecimalField(String name) {
    	return context.indexSchemaElement()
    		    // type BigDecimal need to define decimalScale
        		.field(name, f -> f.asBigDecimal().decimalScale(2))
        		.multiValued()
        		.toReference();
    }
}
