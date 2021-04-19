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
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.packageimport.xml.SettingIndexSearch;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;
import cz.tacr.elza.service.cache.ApVisibilityChecker;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.RamUsageEstimator;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;

@Component
public class ApCachedAccessPointClassBridge implements StringBridge, MetadataProvidingFieldBridge, ApplicationContextAware {

    @Autowired
    private static SettingsService settingsService;

    public static final String PREFIX_PREF = "pref";
    public static final String SEPARATOR = "_";
    public static final String INDEX = "index";
    public static final String SCOPE_ID = "scope_id";
    public static final String STATE = "state";
    public static final String AP_TYPE_ID = "ap_type_id";
    public static final String USERNAME = "username";
    public static final String TRANS = "trans";
    public static final String SORT = "sort";

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
                        addStringField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX + SEPARATOR + SORT, index.getValue().toLowerCase(), document);
                    }

                    addField(name + SEPARATOR + fieldName.toString().toLowerCase(), index.getValue().toLowerCase(), document, luceneOptions, name);
                    addField(name + SEPARATOR + INDEX, index.getValue().toLowerCase(), document, luceneOptions, name);
                }
            }
        }
    }

    private void addStringField(String name, String value, Document document) {
        String valueTrans = removeDiacritic(value);
        document.add(new StringField(name, valueTrans, Field.Store.YES));
        document.add(new SortedDocValuesField(name, new BytesRef(valueTrans)));
    }

    private void addField(String name, String value, Document document, LuceneOptions luceneOptions, String prefixName) {
        Field field = new Field(name, value, luceneOptions.getStore(), Field.Index.NOT_ANALYZED, luceneOptions.getTermVector());
        document.add(field);

        if (isFieldForTransliteration(name, prefixName)) {
            Field transField = new Field(name + SEPARATOR + TRANS, value, luceneOptions.getStore(), Field.Index.ANALYZED, luceneOptions.getTermVector());
            document.add(transField);
        }
    }

    private String removeDiacritic(String value) {
        char[] chars = new char[512];
        final int maxSizeNeeded = 4 * value.length();
        if (chars.length < maxSizeNeeded) {
            chars = new char[ArrayUtil.oversize(maxSizeNeeded, RamUsageEstimator.NUM_BYTES_CHAR)];
        }
        ASCIIFoldingFilter.foldToASCII(value.toCharArray(), 0, chars, 0, value.length());

        return String.valueOf(chars).trim();
    }

    private boolean isFieldForTransliteration(String name, String prefixName) {
        boolean transliterate = false;

        name = StringUtils.removeStart(name, prefixName + SEPARATOR);

        SettingIndexSearch elzaSearchConfig = getElzaSearchConfig();
        if (elzaSearchConfig != null) {
            SettingIndexSearch.Field fieldSearchConfig = getFieldSearchConfigByName(elzaSearchConfig.getFields(), name);
            if (fieldSearchConfig != null && fieldSearchConfig.getTransliterate() != null) {
                transliterate = fieldSearchConfig.getTransliterate();
            }
        }

        return transliterate;
    }

    @Nullable
    private SettingIndexSearch.Field getFieldSearchConfigByName(List<SettingIndexSearch.Field> fields, String name) {
        if (CollectionUtils.isNotEmpty(fields)) {
            for (SettingIndexSearch.Field field : fields) {
                if (field.getName().equals(name)) {
                    return field;
                }
            }
        }
        return null;
    }

    @Nullable
    private SettingIndexSearch getElzaSearchConfig() {
        if (settingsService != null) {
            UISettings.SettingsType indexSearch = UISettings.SettingsType.INDEX_SEARCH;
            List<UISettings> uiSettings = settingsService.getGlobalSettings(indexSearch.toString(), indexSearch.getEntityType());
            if (CollectionUtils.isNotEmpty(uiSettings)) {
                return SettingIndexSearch.newInstance(uiSettings.get(0));
            }
        }
        return null;
    }

    @Override
    public String objectToString(Object object) {
        return (String) object;
    }

    @Override
    public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
        builder.field(name + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX + SEPARATOR + SORT, FieldType.STRING).sortable(true);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        settingsService = applicationContext.getBean(SettingsService.class);
    }
}
