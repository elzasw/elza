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
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;
import cz.tacr.elza.service.cache.ApVisibilityChecker;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ApCachedAccessPointClassBridge implements FieldBridge, StringBridge {

    public static final String PREFIX_PREF = "pref";
    public static final String SEPARATOR = "_";
    public static final String INDEX = "index";
    public static final String SCOPE_CODE = "scope_code";
    public static final String STATE = "state";
    public static final String AP_TYPE_CODE = "ap_type_code";
    public static final String USERNAME = "username";

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
            addField(name + SEPARATOR + CachedAccessPoint.ACCESS_POINT_ID, cachedAccessPoint.getAccessPointId().toString().toLowerCase(), document, luceneOptions);
            addField(name + SEPARATOR + STATE, cachedAccessPoint.getApState().getStateApproval().name().toLowerCase(), document, luceneOptions);
            addField(name + SEPARATOR + AP_TYPE_CODE, cachedAccessPoint.getApState().getApType().getCode().toLowerCase(), document, luceneOptions);
            addField(name + SEPARATOR + SCOPE_CODE, cachedAccessPoint.getApState().getScope().getCode().toLowerCase(), document, luceneOptions);
            if (cachedAccessPoint.getApState().getCreateChange().getUser() != null) {
                addField(name + SEPARATOR + USERNAME, cachedAccessPoint.getApState().getCreateChange().getUser().getUsername().toLowerCase(), document, luceneOptions);
            }

            if (CollectionUtils.isNotEmpty(cachedAccessPoint.getParts())) {
                for (CachedPart part : cachedAccessPoint.getParts()) {
                    addItemFields(name, part, cachedAccessPoint, document, luceneOptions);
                    addIndexFields(name, part, document, luceneOptions);
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

                if (DataType.fromCode(itemType.getEntity().getDataType().getCode()) != DataType.COORDINATES) {
                    continue;
                }

                StringBuilder fieldName = new StringBuilder(itemType.getCode());
                if (part.getPartId().equals(cachedAccessPoint.getPreferredPartId())) {
                    fieldName.insert(0, PREFIX_PREF + SEPARATOR);
                }

                if (itemSpec != null) {
                    fieldName.append(SEPARATOR).append(itemSpec.getCode());
                }

                String value = item.getData().getFulltextValue();
                if (value == null) {
                    if (itemSpec == null) {
                        continue;
                    }
                    value = itemSpec.getCode();
                }
                addField(name + SEPARATOR + fieldName.toString().toLowerCase(), value.toLowerCase(), document, luceneOptions);
            }
        }
    }

    private void addIndexFields(String name, CachedPart part, Document document, LuceneOptions luceneOptions) {
        if (CollectionUtils.isNotEmpty(part.getIndices())) {
            for (ApIndex index : part.getIndices()) {
                StringBuilder fieldName = new StringBuilder(part.getPartType().getCode());
                fieldName.append(SEPARATOR).append(INDEX);

                addField(name + SEPARATOR + fieldName.toString().toLowerCase(), index.getValue().toLowerCase(), document, luceneOptions);
                addField(name + SEPARATOR + INDEX, index.getValue().toLowerCase(), document, luceneOptions);
            }
        }
    }

    private void addField(String name, String value, Document document, LuceneOptions luceneOptions) {
        luceneOptions.addFieldToDocument(name, value, document);
    }

    @Override
    public String objectToString(Object object) {
        return (String) object;
    }
}
