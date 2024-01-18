package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ApCachedAccessPoint.DATA;
import static cz.tacr.elza.domain.ApCachedAccessPoint.FIELD_ACCESSPOINT_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.AP_TYPE_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.SCOPE_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.STATE;

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

public class ApCachedAccessPointBinder implements TypeBinder {

    public static final String NOT_ANALYZED = "";
    public static final String ANALYZED = "_analyzed";
    public static final String SORTABLE = "_sortable";

    private IndexConfigurationReader configurationReader = SpringContext.getBean(IndexConfigurationReader.class);
    private TypeBindingContext context;

    @Override
    public void bind(TypeBindingContext context) {
    	this.context = context;
        Map<String, IndexFieldReference<String>> fields = new HashMap<>();

    	// při změně pole data přepočti index
        context.dependencies().use(DATA);

        // číselné pole accessPointId
        //fields.put(FIELD_ACCESSPOINT_ID, context.indexSchemaElement().field(FIELD_ACCESSPOINT_ID, f -> f.asInteger()).multiValued().toReference());

        // přidání dodatečných polí
        for (String name : Arrays.asList(AP_TYPE_ID, SCOPE_ID, STATE, FIELD_ACCESSPOINT_ID)) {
            fields.put(name + NOT_ANALYZED, createNotAnalyzedField(name));
        }

        // hlavní indexové pole
        for (String name : Arrays.asList("data_index", "data_pref_index")) {
	        fields.put(name + SORTABLE, createSortableField(name));
	        fields.put(name + ANALYZED, createAnalyzedField(name));
	        fields.put(name + NOT_ANALYZED, createNotAnalyzedField(name));
        }

        // part type codes
        for (String partCode : configurationReader.getPartTypeCodes()) {
        	for (String suffix : Arrays.asList("", "_index")) {
	            String name = "data_" + partCode.toLowerCase() + suffix;
	            fields.put(name + SORTABLE, createSortableField(name));
	            fields.put(name + ANALYZED, createAnalyzedField(name));
	            fields.put(name + NOT_ANALYZED, createNotAnalyzedField(name));
        	}
        }

        // item type codes
        for (String itemCode : configurationReader.getItemTypeCodes()) {
        	for (String pref : Arrays.asList("", "pref_")) {
	            String name = "data_" + pref + itemCode.toLowerCase();
	            fields.put(name + SORTABLE, createSortableField(name));
	            fields.put(name + ANALYZED, createAnalyzedField(name));
	            fields.put(name + NOT_ANALYZED, createNotAnalyzedField(name));
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
        		.field(name + ANALYZED, f -> f.asString().analyzer("cz"))
        		.multiValued()
        		.toReference();
    }

    private IndexFieldReference<String> createNotAnalyzedField(String name) {
    	return context.indexSchemaElement()
        		.field(name + NOT_ANALYZED, f -> f.asString())
        		.multiValued()
        		.toReference();
    }
}
