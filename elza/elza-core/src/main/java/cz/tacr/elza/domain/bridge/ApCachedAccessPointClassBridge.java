package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.util.ArrayUtil;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApState;
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

public class ApCachedAccessPointClassBridge implements TypeBridge<ApCachedAccessPoint> {

    private final static Logger log = LoggerFactory.getLogger(ApCachedAccessPointClassBridge.class);

    static private SettingsService settingsService;

    public static final String SCOPE_ID = "scope_id";
    public static final String STATE = "state";
    public static final String AP_TYPE_ID = "ap_type_id";

    public static final String PREFIX_PREF = "pref";
    public static final String SEPARATOR = "_";
    public static final String INDEX = "index";
    public static final String TRANS = "trans";
    public static final String SORT = "sort";

    public static final String NM_MAIN = "nm_main";
    public static final String NM_MINOR = "nm_minor";

    static Properties names = null;
    private Map<String, IndexFieldReference<String>> fields;

    public ApCachedAccessPointClassBridge(Map<String, IndexFieldReference<String>> fields) {
        this.fields = fields;
        log.debug("Creating ApCachedAccessPointClassBridge");
    }

    static public void init(SettingsService settingsService) throws BeansException {
        if (settingsService == null) {
            throw new IllegalArgumentException("settingsService is null");
        }
        ApCachedAccessPointClassBridge.settingsService = settingsService;
    }


    private void addItemFields(String name, CachedPart part, CachedAccessPoint cachedAccessPoint, DocumentElement document) {
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
                    addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase(), value.toLowerCase(), document, name);

                    if (itemSpec != null) {
                        addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase() + SEPARATOR + itemSpec.getCode().toLowerCase(),
                                value.toLowerCase(), document, name);
                    }
                }

                // indexování polí s více než 32766 znaky
                if (dataType == DataType.TEXT) {
                    document.addValue(name + SEPARATOR + itemType.getCode().toLowerCase() + ApCachedAccessPointClassBinder.NOT_ANALYZED, value);
                } else {
                    addField(name + SEPARATOR + itemType.getCode().toLowerCase(), value.toLowerCase(), document, name);
                }

                if (itemSpec != null) {
                    addField(name + SEPARATOR + itemType.getCode().toLowerCase() + SEPARATOR + itemSpec.getCode().toLowerCase(), value.toLowerCase(), document, name);
                }
            }
        }
    }

    private void addIndexFields(String name, CachedPart part, CachedAccessPoint cachedAccessPoint, DocumentElement document) {
        if (CollectionUtils.isNotEmpty(part.getIndices())) {
            for (ApIndex index : part.getIndices()) {
                if (index.getIndexType().equals(DISPLAY_NAME)) {
                    StringBuilder fieldName = new StringBuilder(part.getPartTypeCode());
                    fieldName.append(SEPARATOR).append(INDEX);

                    if (part.getPartId().equals(cachedAccessPoint.getPreferredPartId())) {
                        addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX, index.getIndexValue().toLowerCase(), document, name);
                        addSortField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX + SEPARATOR + SORT, index
                                .getIndexValue().toLowerCase(), document);
                    }

                    addField(name + SEPARATOR + fieldName.toString().toLowerCase(), index.getIndexValue().toLowerCase(), document, name);
                    addField(name + SEPARATOR + INDEX, index.getIndexValue().toLowerCase(), document, name);
                }
            }
        }
    }

    /**
     * Pridani pole pro razeni
     *
     * @param name
     * @param value
     * @param document
     */
    private void addSortField(String name, String value, DocumentElement document) {
        String valueTrans = removeDiacritic(value); //TODO pasek
        document.addValue(name + ApCachedAccessPointClassBinder.STORED_SORTABLE, valueTrans);
    }

    private void addStringField(String name, String value, DocumentElement document) {
        document.addValue(name + ApCachedAccessPointClassBinder.NOT_ANALYZED, value); //TODO pasek
    }

    private void addField(String name, String value, DocumentElement document, String prefixName) {
        // Pridani raw hodnoty fieldu (bez tranliterace - NOT_ANALYZED) //TODO pasek
        document.addValue(name + ApCachedAccessPointClassBinder.NOT_ANALYZED, value);

        if (isFieldForTransliteration(name, prefixName)) {
            document.addValue(name + SEPARATOR + TRANS + SEPARATOR + ApCachedAccessPointClassBinder.ANALYZED, value);
        }
    }

    private String removeDiacritic(String value) {
        char[] chars = new char[512];
        final int maxSizeNeeded = 4 * value.length();
        if (chars.length < maxSizeNeeded) {
            chars = new char[ArrayUtil.oversize(maxSizeNeeded, Character.BYTES)];
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
        if (settingsService == null) {
            log.error("Search configuration is not set");
            throw new IllegalStateException("Not initialized");
        }

        UISettings.SettingsType indexSearch = UISettings.SettingsType.INDEX_SEARCH;
        List<UISettings> uiSettings = settingsService.getGlobalSettings(indexSearch.toString(), indexSearch
                .getEntityType());
        if (CollectionUtils.isNotEmpty(uiSettings)) {
            return SettingIndexSearch.newInstance(uiSettings.get(0));
        }
        return null;
    }

    @Override
    public void write(DocumentElement document, ApCachedAccessPoint apCachedAccessPoint, TypeBridgeWriteContext typeBridgeWriteContext) {

        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setVisibility(new ApVisibilityChecker(AccessPointCacheSerializable.class,
                String.class, Number.class, Boolean.class, Iterable.class,
                LocalDate.class, LocalDateTime.class));
        String name = "data";
        try {
            // TODO: use cache service to deserialize
            CachedAccessPoint cachedAccessPoint = mapper.readValue(apCachedAccessPoint.getData(), CachedAccessPoint.class);
            // do not index APs without state or deleted APs
            ApState apState = cachedAccessPoint.getApState();
            if (apState == null || apState.getDeleteChangeId() != null) {
                return;
            }

            addStringField(STATE, cachedAccessPoint.getApState().getStateApproval().name().toLowerCase(), document);
            addStringField(AP_TYPE_ID, cachedAccessPoint.getApState().getApTypeId().toString(), document);
            addStringField(SCOPE_ID, cachedAccessPoint.getApState().getScopeId().toString(), document);

            if (CollectionUtils.isNotEmpty(cachedAccessPoint.getParts())) {
                for (CachedPart part : cachedAccessPoint.getParts()) {
                    addItemFields(name, part, cachedAccessPoint, document);
                    addIndexFields(name, part, cachedAccessPoint, document);
                }
            }

        } catch (IOException e) {
            throw new SystemException("Nastal problém při deserializaci objektu", e);
        }
    }

    private static String getMappingFile() {
        //hibernate.search.backend.directory.root + "AeRecordCache"
        String dir = System.getProperty("user.dir") + "/" + ApCachedAccessPoint.class.getSimpleName() + "/mapping.txt";
        return dir;
    }

}
