package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ApCachedAccessPoint.DATA;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApCachedAccessPointBinder implements TypeBinder {

    public static final String NOT_ANALYZED = "";
    public static final String ANALYZED = "_analyzed";
    public static final String STORED_SORTABLE = "_sortable";
    IndexConfigurationReader configurationReader = SpringContext.getBean(IndexConfigurationReader.class);
    //private JdbcTemplate jdbcTemplate = SpringContext.getBean(JdbcTemplate.class);

    @Override
    public void bind(TypeBindingContext context) {
        Map<String, IndexFieldReference<String>> fields = new HashMap<>();

    	// při změně pole data přepočti index
        context.dependencies().use(DATA);

        // čtení kódů pro indexační pole
//        String itemTypeSql = "SELECT * FROM rul_item_type";
//        List<String> itemTypeCodes = jdbcTemplate.query(itemTypeSql, (rs, rowNum) -> rs.getString("code"));

        // přidání dodatečných polí
//        for (String name : Arrays.asList(AP_TYPE_ID, SCOPE_ID, STATE, 
//        		"data_cre_class", "data_cre_class_crc_birth", "data_cre_date", "data_pt_cre_index", "data_index", "data_index_trans", 
//        		"data_nm_main", "data_nm_main_trans", "data_nm_sup_chro", "data_pt_name_index", "data_pt_name_index_trans",
//        		"data_brief_desc", "data_pt_body_index", "data_nm_lang", "data_nm_lang_lng_cze", "data_nm_type",
//        		"data_nm_type_nt_acronym", "data_idn_type", "data_idn_type_archnum", "data_idn_value", "data_idn_verified",
//        		"data_pt_ident_index", "data_nm_sup_gen")) {
//        	fields.put(name + STORED_SORTABLE, context.indexSchemaElement()
//            		.field(name + STORED_SORTABLE, f -> f.asString().sortable(Sortable.YES).projectable(Projectable.YES))
//            		.multiValued()
//            		.toReference());
//        	fields.put(name + ANALYZED, context.indexSchemaElement()
//            		.field(name + ANALYZED, f -> f.asString().analyzer("cz"))
//            		.multiValued()
//            		.toReference());
//        	fields.put(name + NOT_ANALYZED, context.indexSchemaElement()
//            		.field(name + NOT_ANALYZED, f -> f.asString())
//            		.multiValued()
//            		.toReference());
//        }

        for (String typeCode : configurationReader.getImportedItemTypeCodes()) {
        	for (String name : {"data_" + typeCode.toLowerCase(), "data_pref_" + typeCode.toLowerCase()) {
	            //String name = "data_" + typeCode.toLowerCase();
	            IndexFieldReference<String> fieldS = context.indexSchemaElement()
	            		.field(name + STORED_SORTABLE, f -> f.asString().sortable(Sortable.YES).projectable(Projectable.YES))
	            		.multiValued()
	            		.toReference();
	            IndexFieldReference<String> fieldA = context.indexSchemaElement()
	            		.field(name + ANALYZED, f -> f.asString().analyzer("cz"))
	            		.multiValued()
	            		.toReference();
	            IndexFieldReference<String> fieldNA = context.indexSchemaElement()
	            		.field(name + NOT_ANALYZED, f -> f.asString())
	            		.multiValued()
	            		.toReference();
	
	            fields.put(name + STORED_SORTABLE, fieldS);
	            fields.put(name + ANALYZED, fieldA);
	            fields.put(name + NOT_ANALYZED, fieldNA);
        	}
        }

        context.bridge(ApCachedAccessPoint.class, new ApCachedAccessPointBridge(fields));
    }
}
