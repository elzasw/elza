package cz.tacr.elza.domain.bridge;

import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.service.SpringContext;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;

import java.util.HashMap;
import java.util.Map;

public class ApCachedAccessPointClassBinder implements TypeBinder {

    public static final String NOT_ANALYZED = "";
    public static final String ANALYZED = "_analyzed";
    public static final String STORED_SORTABLE = "_sortable";
    IndexConfigurationReader configurationReader = SpringContext.getBean(IndexConfigurationReader.class);

    @Override
    public void bind(TypeBindingContext context) {

        //při změně pole data přepočti index
        context.dependencies().use("data");

        Map<String, IndexFieldReference<String>> fields = new HashMap<>();

        for (String typeCode : configurationReader.getImportedItemTypeCodes()) {
            String name = "data_pref_" + typeCode;
            IndexFieldReference<String> field = context.indexSchemaElement().field(name + STORED_SORTABLE, indexFieldTypeFactory -> indexFieldTypeFactory.asString().sortable(Sortable.YES).projectable(Projectable.YES)).multiValued().toReference();
            IndexFieldReference<String> field2 = context.indexSchemaElement().field(name + ANALYZED, indexFieldTypeFactory -> indexFieldTypeFactory.asString().analyzer("cz")).multiValued().toReference();
            IndexFieldReference<String> field3 = context.indexSchemaElement().field(name + NOT_ANALYZED, IndexFieldTypeFactory::asString).multiValued().toReference();

            fields.put(name + STORED_SORTABLE, field);
            fields.put(name + ANALYZED, field2);
            fields.put(name + NOT_ANALYZED, field3);
        }

        context.bridge(ApCachedAccessPoint.class, new ApCachedAccessPointClassBridge(fields));
    }
}
