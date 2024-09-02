package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ApCachedAccessPoint.DATA;
import static cz.tacr.elza.domain.ApCachedAccessPoint.FIELD_ACCESSPOINT_ID;
import static cz.tacr.elza.domain.LuceneAnalyzerConfigurer.CLASSIC_TOKENIZER_CZ;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.AP_TYPE_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.SCOPE_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.STATE;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.REV_STATE;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.USERNAME;

import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.service.SpringContext;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
/**
 * Třída ApCachedAccessPointBinder je odpovědná za vytváření polí pro indexování.
 * 
 * Část názvů polí jsou jednoduše zadány jako konstanty:
 * - AP_TYPE_ID ("ap_type_id")
 * - SCOPE_ID ("scope_id")
 * - STATE ("state")
 * - FIELD_ACCESSPOINT_ID ("accessPointId")
 * - data_index
 * - data_pref_index
 * 
 * Část názvů polí získáme ze seznamu kódů PartType a ItemType:
 * - getPartTypeCodes()
 * - getItemTypeCodes()
 * a také jako kombinací kódů ItemType + ItemSpec
 * - getItemSpecCodesByTypeCode(itemTypeCode)
 */
public class ApCachedAccessPointBinder implements TypeBinder {

    public static final String NOT_ANALYZED = "";
    public static final String ANALYZED = "_analyzed";
    public static final String SORTABLE = "_sortable";

    public static final String REL_AP_ID = "rel_accesspoint_id";

    public static final String NORM_FROM = "_from";
    public static final String NORM_TO = "_to";

    private IndexConfigReader configurationReader = SpringContext.getBean(IndexConfigReader.class);
    private TypeBindingContext context;

    @Override
    public void bind(TypeBindingContext context) {
    	this.context = context;
        Map<String, IndexFieldReference<String>> fields = new HashMap<>();

    	// při změně pole data přepočti index
        context.dependencies().use(DATA);

        // přidání dodatečných polí
        for (String name : Arrays.asList(AP_TYPE_ID, SCOPE_ID, STATE, REV_STATE, FIELD_ACCESSPOINT_ID, USERNAME)) {
            fields.put(name + NOT_ANALYZED, createNotAnalyzedField(name));
        }

        // hlavní indexové pole
        for (String name : Arrays.asList("data_index", "data_pref_index")) {
	        fields.put(name + SORTABLE, createSortableField(name));
	        fields.put(name + ANALYZED, createAnalyzedField(name));
	        fields.put(name + NOT_ANALYZED, createNotAnalyzedField(name));
        }

        // pole obsahující odkaz na jinou entitu
        createIntField(REL_AP_ID);

        // part type codes
        for (String partCode : configurationReader.getPartTypeCodes()) {
        	for (String suffix : Arrays.asList("", "_index")) {
	            String name = "data_" + partCode.toLowerCase() + suffix;
	            fields.put(name + SORTABLE, createSortableField(name));
	            fields.put(name + ANALYZED, createAnalyzedField(name));
	            fields.put(name + NOT_ANALYZED, createNotAnalyzedField(name));
        	}
        }

        // TODO avoid creating combinations that will not be indexed
        
        // item type codes
        for (String itemTypeCode : configurationReader.getItemTypeCodes()) {
        	for (String pref : Arrays.asList("", "pref_")) {
	            String name = "data_" + pref + itemTypeCode.toLowerCase();
	            fields.put(name + SORTABLE, createSortableField(name));
	            fields.put(name + ANALYZED, createAnalyzedField(name));
	            fields.put(name + NOT_ANALYZED, createNotAnalyzedField(name));
	            createLongField(name + NORM_FROM);
	            createLongField(name + NORM_TO);
	            for (String itemSpecCode : configurationReader.getItemSpecCodesByTypeCode(itemTypeCode)) {
	            	String nameAddSpec = "data_" + pref + itemTypeCode.toLowerCase() + "_" + itemSpecCode.toLowerCase();
		            fields.put(nameAddSpec + ANALYZED, createAnalyzedField(nameAddSpec));
		            fields.put(nameAddSpec + NOT_ANALYZED, createNotAnalyzedField(nameAddSpec));
	            }
        	}
        }

        context.bridge(ApCachedAccessPoint.class, new ApCachedAccessPointBridge(fields));
    }

    private IndexFieldReference<String> createSortableField(String name) {
    	return context.indexSchemaElement()
        		.field(name + SORTABLE, f -> f.asString().sortable(Sortable.YES).projectable(Projectable.YES))
        		.multiValued()
        		.toReference();
    }

    private IndexFieldReference<String> createAnalyzedField(String name) {
    	return context.indexSchemaElement()
        		.field(name + ANALYZED, f -> f.asString().analyzer(CLASSIC_TOKENIZER_CZ))
        		.multiValued()
        		.toReference();
    }

    private IndexFieldReference<String> createNotAnalyzedField(String name) {
    	return context.indexSchemaElement()
        		.field(name + NOT_ANALYZED, f -> f.asString())
        		.multiValued()
        		.toReference();
    }

    private IndexFieldReference<Integer> createIntField(String name) {
    	return context.indexSchemaElement()
        		.field(name, f -> f.asInteger())
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
