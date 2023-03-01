package cz.tacr.elza.domain.bridge;

import cz.tacr.elza.domain.ApCachedAccessPoint;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;

import java.util.HashMap;
import java.util.Map;

public class ApCachedAccessPointClassBinder implements TypeBinder {

    public static final int MAX_FIELDS = 100;
    public static final String NOT_ANALYZED = "";
    public static final String ANALYZED = "_analyzed";
    public static final String STORED_SORTABLE = "_sortable";

    @Override
    public void bind(TypeBindingContext context) {
        //při změně pole data přepočti index
        context.dependencies().use("data");

        Map<String, IndexFieldReference<String>> fields = new HashMap<>();

        //dynamicky přidáme 50 polí do lucene indexu
        for(int i = 0; i< MAX_FIELDS; i++) {
            String name = "data_" + i;
            IndexFieldReference<String> field = context.indexSchemaElement()
                    .field(name + STORED_SORTABLE, indexFieldTypeFactory -> indexFieldTypeFactory.asString().sortable(Sortable.YES).projectable(Projectable.YES))
                    .multiValued()
                    .toReference();
            IndexFieldReference<String> field2 = context.indexSchemaElement()
                    .field(name + ANALYZED, indexFieldTypeFactory -> indexFieldTypeFactory.asString().analyzer("cz"))
                    .multiValued()
                    .toReference();
            IndexFieldReference<String> field3 = context.indexSchemaElement()
                    .field(name + NOT_ANALYZED, indexFieldTypeFactory -> indexFieldTypeFactory.asString())
                    .multiValued()
                    .toReference();

            fields.put(name + STORED_SORTABLE, field);
            fields.put(name + ANALYZED, field2);
            fields.put(name + NOT_ANALYZED, field3);
        }
        context.bridge(ApCachedAccessPoint.class, new ApCachedAccessPointClassBridge(fields));
    }
}
