package cz.tacr.elza.domain.bridge;

//import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME; //TODO hibernate search 6
//
//import java.io.IOException;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//import javax.annotation.Nullable;
//
//import org.apache.commons.collections4.CollectionUtils;
//import org.apache.commons.lang.Validate;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.document.Field;
//import org.apache.lucene.document.Field.Store;
//import org.apache.lucene.document.IntField;
//import org.apache.lucene.document.SortedDocValuesField;
//import org.apache.lucene.document.StringField;
//import org.apache.lucene.document.TextField;
//import org.apache.lucene.util.ArrayUtil;
//import org.apache.lucene.util.BytesRef;
//import org.apache.lucene.util.RamUsageEstimator;
//import org.hibernate.search.bridge.LuceneOptions;
//import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
//import org.hibernate.search.bridge.StringBridge;
//import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
//import org.hibernate.search.bridge.spi.FieldType;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.BeansException;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//
//import cz.tacr.elza.core.data.DataType;
//import cz.tacr.elza.core.data.ItemType;
//import cz.tacr.elza.core.data.StaticDataProvider;
//import cz.tacr.elza.domain.ApCachedAccessPoint;
//import cz.tacr.elza.domain.ApIndex;
//import cz.tacr.elza.domain.ApItem;
//import cz.tacr.elza.domain.ApState;
//import cz.tacr.elza.domain.ArrDataRecordRef;
//import cz.tacr.elza.domain.RulItemSpec;
//import cz.tacr.elza.domain.UISettings;
//import cz.tacr.elza.exception.SystemException;
//import cz.tacr.elza.packageimport.xml.SettingIndexSearch;
//import cz.tacr.elza.service.SettingsService;
//import cz.tacr.elza.service.cache.AccessPointCacheSerializable;
//import cz.tacr.elza.service.cache.ApVisibilityChecker;
//import cz.tacr.elza.service.cache.CachedAccessPoint;
//import cz.tacr.elza.service.cache.CachedPart;
//
//public class ApCachedAccessPointClassBridge implements StringBridge, MetadataProvidingFieldBridge {
//
//    private final static Logger log = LoggerFactory.getLogger(ApCachedAccessPointClassBridge.class);
//
//    static private SettingsService settingsService;
//
//    public static final String SCOPE_ID = "scope_id";
//    public static final String STATE = "state";
//    public static final String AP_TYPE_ID = "ap_type_id";
//
//    /**
//     * Related access point ID
//     *
//     * Index all related AP
//     */
//    public static final String REL_AP_ID = "rel_accesspoint_id";
//
//    public static final String PREFIX_PREF = "pref";
//    public static final String SEPARATOR = "_";
//    public static final String INDEX = "index";
//    public static final String USERNAME = "username";
//    public static final String TRANS = "trans";
//    public static final String SORT = "sort";
//
//    public static final String PREF_INDEX = "pref_index";
//    public static final String PREF_NM_MAIN = "pref_nm_main";
//    public static final String PREF_NM_MINOR = "pref_nm_minor";
//    public static final String NM_MAIN = "nm_main";
//    public static final String NM_MINOR = "nm_minor";
//
//    /**
//     * Map of field configurations
//     *
//     * Map is not null if configuration was processed
//     */
//    private Map<String, SettingIndexSearch.Field> fieldConfigMap;
//
//    public ApCachedAccessPointClassBridge() {
//        log.debug("Creating ApCachedAccessPointClassBridge");
//    }
//
//    static public void init(SettingsService settingsService) throws BeansException {
//        if (settingsService == null) {
//            throw new IllegalArgumentException("settingsService is null");
//        }
//        ApCachedAccessPointClassBridge.settingsService = settingsService;
//    }
//
//    @Override
//    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
//        ApCachedAccessPoint apCachedAccessPoint = (ApCachedAccessPoint) value;
//
//        final ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JavaTimeModule());
//        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
//        mapper.setVisibility(new ApVisibilityChecker(AccessPointCacheSerializable.class,
//                String.class, Number.class, Boolean.class, Iterable.class,
//                LocalDate.class, LocalDateTime.class));
//
//        try {
//            // TODO: use cache service to deserialize
//            CachedAccessPoint cachedAccessPoint = mapper.readValue(apCachedAccessPoint.getData(), CachedAccessPoint.class);
//            // do not index APs without state or deleted APs
//            ApState apState = cachedAccessPoint.getApState();
//            if (apState == null || apState.getDeleteChangeId() != null) {
//                return;
//            }
//
//            addStringField(STATE, cachedAccessPoint.getApState().getStateApproval().name().toLowerCase(), document);
//            // TODO: rework as int values
//            addStringField(AP_TYPE_ID, cachedAccessPoint.getApState().getApTypeId().toString(), document);
//            addStringField(SCOPE_ID, cachedAccessPoint.getApState().getScopeId().toString(), document);
//
//            if (CollectionUtils.isNotEmpty(cachedAccessPoint.getParts())) {
//                for (CachedPart part : cachedAccessPoint.getParts()) {
//                    addItemFields(name, part, cachedAccessPoint, document, luceneOptions);
//                    addIndexFields(name, part, cachedAccessPoint, document, luceneOptions);
//                }
//            }
//
//        } catch (IOException e) {
//            throw new SystemException("Nastal problém při deserializaci objektu", e);
//        }
//    }
//
//    private void addItemFields(String name, CachedPart part, CachedAccessPoint cachedAccessPoint, Document document, LuceneOptions luceneOptions) {
//        if (CollectionUtils.isNotEmpty(part.getItems())) {
//            StaticDataProvider sdp = StaticDataProvider.getInstance();
//
//            for (ApItem item : part.getItems()) {
//                ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());
//                RulItemSpec itemSpec = item.getItemSpecId() != null ? sdp.getItemSpecById(item.getItemSpecId()) : null;
//                DataType dataType = DataType.fromCode(itemType.getEntity().getDataType().getCode());
//
//                if (dataType == DataType.COORDINATES) {
//                    continue;
//                }
//
//                String value;
//
//                if (dataType == DataType.RECORD_REF) {
//                    ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) item.getData();
//                    if (dataRecordRef == null || dataRecordRef.getRecordId() == null) {
//                        continue;
//                    }
//                    addIntField(REL_AP_ID, dataRecordRef.getRecordId(), document);
//                    value = dataRecordRef.getRecordId().toString();
//                } else {
//                    value = item.getData().getFulltextValue();
//                }
//
//                if (value == null) {
//                    if (itemSpec == null) {
//                        continue;
//                    }
//                    value = itemSpec.getCode();
//                }
//
//                if (part.getPartId().equals(cachedAccessPoint.getPreferredPartId())) {
//                    addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase(), value.toLowerCase(), document, luceneOptions, name);
//
//                    if (itemSpec != null) {
//                        addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase() + SEPARATOR + itemSpec.getCode().toLowerCase(),
//                                value.toLowerCase(), document, luceneOptions, name);
//                    }
//                }
//
//                // indexování polí s více než 32766 znaky
//                if (dataType == DataType.TEXT) {
//                    TextField field = new TextField(name + SEPARATOR + itemType.getCode().toLowerCase(), value, Store.YES);
//                    document.add(field);
//                } else {
//                    addField(name + SEPARATOR + itemType.getCode().toLowerCase(), value.toLowerCase(), document, luceneOptions, name);
//                }
//
//                if (itemSpec != null) {
//                    addField(name + SEPARATOR + itemType.getCode().toLowerCase() + SEPARATOR + itemSpec.getCode().toLowerCase(), value.toLowerCase(), document, luceneOptions, name);
//                }
//            }
//        }
//    }
//
//    private void addIndexFields(String name, CachedPart part, CachedAccessPoint cachedAccessPoint, Document document, LuceneOptions luceneOptions) {
//        if (CollectionUtils.isNotEmpty(part.getIndices())) {
//            for (ApIndex index : part.getIndices()) {
//                if (index.getIndexType().equals(DISPLAY_NAME)) {
//                    StringBuilder fieldName = new StringBuilder(part.getPartTypeCode());
//                    fieldName.append(SEPARATOR).append(INDEX);
//
//                    if (part.getPartId().equals(cachedAccessPoint.getPreferredPartId())) {
//                        addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX, index.getValue().toLowerCase(), document, luceneOptions, name);
//                        addSortField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX + SEPARATOR + SORT, index
//                                .getValue().toLowerCase(), document);
//                    }
//
//                    addField(name + SEPARATOR + fieldName.toString().toLowerCase(), index.getValue().toLowerCase(), document, luceneOptions, name);
//                    addField(name + SEPARATOR + INDEX, index.getValue().toLowerCase(), document, luceneOptions, name);
//                }
//            }
//        }
//    }
//
//    /**
//     * Pridani pole pro razeni
//     *
//     * @param name
//     * @param value
//     * @param document
//     */
//    private void addSortField(String name, String value, Document document) {
//        String valueTrans = removeDiacritic(value);
//        //?? Pole je nutne pridat ve dvou formatech¨
//        // TODO: Proc?
//        document.add(new StringField(name, valueTrans, Field.Store.YES));
//        document.add(new SortedDocValuesField(name, new BytesRef(valueTrans)));
//    }
//
//    private void addIntField(String name, Integer value, Document document) {
//        IntField field = new IntField(name, value, Store.YES);
//        document.add(field);
//    }
//
//    private void addStringField(String name, String value, Document document) {
//        StringField field = new StringField(name, value, Store.YES);
//        document.add(field);
//    }
//
//    private void addField(String name, String value, Document document, LuceneOptions luceneOptions, String prefixName) {
//        // Pridani raw hodnoty fieldu (bez tranliterace - NOT_ANALYZED)
//        Field field = new Field(name, value, luceneOptions.getStore(), Field.Index.NOT_ANALYZED, luceneOptions.getTermVector());
//        document.add(field);
//
//        if (isFieldForTransliteration(name, prefixName)) {
//            Field transField = new Field(name + SEPARATOR + TRANS, value, luceneOptions.getStore(), Field.Index.ANALYZED, luceneOptions.getTermVector());
//            document.add(transField);
//        }
//    }
//
//    private String removeDiacritic(String value) {
//        char[] chars = new char[512];
//        final int maxSizeNeeded = 4 * value.length();
//        if (chars.length < maxSizeNeeded) {
//            chars = new char[ArrayUtil.oversize(maxSizeNeeded, RamUsageEstimator.NUM_BYTES_CHAR)];
//        }
//        ASCIIFoldingFilter.foldToASCII(value.toCharArray(), 0, chars, 0, value.length());
//
//        return String.valueOf(chars).trim();
//    }
//
//    private boolean isFieldForTransliteration(String name, String prefixName) {
//        boolean transliterate = false;
//
//        name = StringUtils.removeStart(name, prefixName + SEPARATOR);
//
//        if (fieldConfigMap == null) {
//            loadElzaSearchConfig();
//            // Field should be set
//            Validate.notNull(fieldConfigMap);
//        }
//
//        SettingIndexSearch.Field fieldSearchConfig = fieldConfigMap.get(name);
//        if (fieldSearchConfig != null && fieldSearchConfig.getTransliterate() != null) {
//            transliterate = fieldSearchConfig.getTransliterate();
//        }
//
//        return transliterate;
//    }
//
//    @Nullable
//    private void loadElzaSearchConfig() {
//        if (settingsService == null) {
//            log.error("Search configuration is not set");
//            throw new IllegalStateException("Not initialized");
//        }
//
//        UISettings.SettingsType indexSearch = UISettings.SettingsType.INDEX_SEARCH;
//        List<UISettings> uiSettings = settingsService.getGlobalSettings(indexSearch.toString(), indexSearch
//                .getEntityType());
//        if (CollectionUtils.isEmpty(uiSettings)) {
//            this.fieldConfigMap = Collections.emptyMap();
//            return;
//        }
//        // TODO: process more configs
//        SettingIndexSearch cfg = SettingIndexSearch.newInstance(uiSettings.get(0));
//        List<SettingIndexSearch.Field> fields = cfg.getFields();
//        if (CollectionUtils.isEmpty(fields)) {
//            this.fieldConfigMap = Collections.emptyMap();
//            return;
//        }
//        this.fieldConfigMap = fields.stream().collect(Collectors.toMap(SettingIndexSearch.Field::getName,
//                                                                       Function.identity()));
//    }
//
//    @Override
//    public String objectToString(Object object) {
//        return (String) object;
//    }
//
//    @Override
//    public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
//        builder.field(name + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX + SEPARATOR + SORT, FieldType.STRING).sortable(true);
//    }
//}
