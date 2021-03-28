package cz.tacr.elza.domain.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;
import cz.tacr.elza.service.cache.ApVisibilityChecker;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;

public class ApCachedAccessPointClassBridge implements FieldBridge, StringBridge {

    public static final String PREFIX_PREF = "pref";
    public static final String SEPARATOR = "_";
    public static final String INDEX = "index";
    public static final String SCOPE_ID = "scope_id";
    public static final String STATE = "state";
    public static final String AP_TYPE_ID = "ap_type_id";
    public static final String USERNAME = "username";

    public static final String PREF_INDEX = "pref_index";
    public static final String PREF_NM_MAIN = "pref_nm_main";
    public static final String PREF_NM_MINOR = "pref_nm_minor";
    public static final String NM_MAIN = "nm_main";
    public static final String NM_MINOR = "nm_minor";

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        ApCachedAccessPoint apCachedAccessPoint = (ApCachedAccessPoint) value;
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setVisibility(new ApVisibilityChecker(AccessPointCacheSerializable.class,
                String.class, Number.class, Boolean.class, Iterable.class,
                LocalDate.class, LocalDateTime.class));

        try {
            CachedAccessPoint cachedAccessPoint = mapper.readValue(apCachedAccessPoint.getData(), CachedAccessPoint.class);
            addField(name + SEPARATOR + CachedAccessPoint.ACCESS_POINT_ID, cachedAccessPoint.getAccessPointId().toString().toLowerCase(), document, luceneOptions, name);
            addField(name + SEPARATOR + STATE, cachedAccessPoint.getApState().getStateApproval().name().toLowerCase(), document, luceneOptions, name);
            addField(name + SEPARATOR + AP_TYPE_ID, cachedAccessPoint.getApState().getApTypeId().toString(), document, luceneOptions, name);
            addField(name + SEPARATOR + SCOPE_ID, cachedAccessPoint.getApState().getScopeId().toString(), document, luceneOptions, name);

            if (CollectionUtils.isNotEmpty(cachedAccessPoint.getParts())) {
                for (CachedPart part : cachedAccessPoint.getParts()) {
                    addItemFields(name, part, cachedAccessPoint, document, luceneOptions);
                    addIndexFields(name, part, cachedAccessPoint, document, luceneOptions);
                }
            }

        } catch (IOException e) {
            throw new SystemException("Nastal problém při deserializaci objektu", e);
        }
    }



    private void addItemFields(String name, CachedPart part, CachedAccessPoint cachedAccessPoint, Document document, LuceneOptions luceneOptions) {
        if (CollectionUtils.isNotEmpty(part.getItems())) {
            StaticDataProvider sdp = StaticDataProvider.getInstance();

            for (ApItem item : part.getItems()) {
                ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());
                RulItemSpec itemSpec = item.getItemSpecId() != null ? sdp.getItemSpecById(item.getItemSpecId()) : null;
                DataType dataType = DataType.fromCode(itemType.getEntity().getDataType().getCode());

                if (dataType == DataType.COORDINATES) {
                    continue;
                }

                String value;

                if (dataType == DataType.RECORD_REF) {
                    ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) item.getData();
                    if (dataRecordRef == null || dataRecordRef.getRecordId() == null) {
                        continue;
                    }
                    value = dataRecordRef.getRecordId().toString();
                } else {
                    value = item.getData().getFulltextValue();
                }

                if (value == null) {
                    if (itemSpec == null) {
                        continue;
                    }
                    value = itemSpec.getCode();
                }

                if (part.getPartId().equals(cachedAccessPoint.getPreferredPartId())) {
                    addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase(), value.toLowerCase(), document, luceneOptions, name);

                    if (itemSpec != null) {
                        addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase() + SEPARATOR + itemSpec.getCode().toLowerCase(),
                                value.toLowerCase(), document, luceneOptions, name);
                    }
                }

                addField(name + SEPARATOR + itemType.getCode().toLowerCase(), value.toLowerCase(), document, luceneOptions, name);

                if (itemSpec != null) {
                    addField(name + SEPARATOR + itemType.getCode().toLowerCase() + SEPARATOR + itemSpec.getCode().toLowerCase(), value.toLowerCase(), document, luceneOptions, name);
                }
            }
        }
    }

    private void addIndexFields(String name, CachedPart part, CachedAccessPoint cachedAccessPoint, Document document, LuceneOptions luceneOptions) {
        if (CollectionUtils.isNotEmpty(part.getIndices())) {
            for (ApIndex index : part.getIndices()) {
                if (index.getIndexType().equals(DISPLAY_NAME)) {
                    StringBuilder fieldName = new StringBuilder(part.getPartTypeCode());
                    fieldName.append(SEPARATOR).append(INDEX);

                    if (part.getPartId().equals(cachedAccessPoint.getPreferredPartId())) {
                        addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX, index.getValue().toLowerCase(), document, luceneOptions, name);
                    }

                    addField(name + SEPARATOR + fieldName.toString().toLowerCase(), index.getValue().toLowerCase(), document, luceneOptions, name);
                    addField(name + SEPARATOR + INDEX, index.getValue().toLowerCase(), document, luceneOptions, name);
                }
            }
        }
    }

    private void addField(String name, String value, Document document, LuceneOptions luceneOptions, String prefixName) {
        Field field = new Field(name, value, luceneOptions.getStore(), luceneOptions.getIndex(), luceneOptions.getTermVector());
        field.setBoost(getBoost(name, prefixName));
        document.add(field);
    }

    private float getBoost(String name, String prefixName) {
        float boost = 1.0f;

        name = StringUtils.removeStart(name, prefixName + SEPARATOR);

        switch (name) {
            case PREF_INDEX: boost = 1.5f; break;
            case PREF_NM_MAIN: boost = 2.0f; break;
            case PREF_NM_MINOR: boost = 1.7f; break;
            case NM_MAIN: boost = 1.2f; break;
            case NM_MINOR: boost = 1.1f; break;
        }

        return boost;
    }

    @Override
    public String objectToString(Object object) {
        return (String) object;
    }
}
