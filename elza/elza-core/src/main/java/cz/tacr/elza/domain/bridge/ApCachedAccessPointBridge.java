package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;
import static cz.tacr.elza.groovy.GroovyResult.PT_PREFER_NAME;
import static cz.tacr.elza.domain.ApCachedAccessPoint.DATA;
import static cz.tacr.elza.domain.ApCachedAccessPoint.FIELD_ACCESSPOINT_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBinder.REL_AP_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBinder.NORM_FROM;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBinder.NORM_TO;

import java.util.List;
import java.util.Map;
import java.util.Properties;

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

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.packageimport.xml.SettingIndexSearch;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;
import jakarta.annotation.Nullable;

public class ApCachedAccessPointBridge implements TypeBridge<ApCachedAccessPoint> {

    private final static Logger log = LoggerFactory.getLogger(ApCachedAccessPointBridge.class);

    private static SettingsService settingsService;

    private static SettingIndexSearch settingIndexSearch;
    
    // TODO převést na použití Bean
    private static AccessPointCacheService accessPointCacheService;

    public static final String AP_TYPE_ID = "ap_type_id";
    public static final String SCOPE_ID = "scope_id";
    public static final String STATE = "state";
    public static final String REV_STATE = "rev_state";

    public static final String PREFIX_PREF = "pref";
    public static final String SEPARATOR = "_";
    public static final String INDEX = "index";

    public static final String USERNAME = "username";

    //public static final String TRANS = "trans";
    //public static final String SORT = "sort";

    public static final String NM_MAIN = "nm_main";
    public static final String NM_MINOR = "nm_minor";

    static Properties names = null;
    private Map<String, IndexFieldReference<String>> fields;

    public ApCachedAccessPointBridge(Map<String, IndexFieldReference<String>> fields) {
        this.fields = fields;
        log.debug("Creating ApCachedAccessPointClassBridge");
    }

    public static void init(SettingsService settingsService, AccessPointCacheService accessPointCacheService) throws BeansException {
        if (settingsService == null) {
            throw new IllegalArgumentException("settingsService is null");
        }
        ApCachedAccessPointBridge.settingsService = settingsService;
        ApCachedAccessPointBridge.settingIndexSearch = getElzaSearchConfig();
        ApCachedAccessPointBridge.accessPointCacheService = accessPointCacheService;
    }

    @Override
    public void write(DocumentElement document, ApCachedAccessPoint apCachedAccessPoint, TypeBridgeWriteContext typeBridgeWriteContext) {

    	CachedAccessPoint cachedAccessPoint = accessPointCacheService.deserialize(apCachedAccessPoint.getData()); 
        if (cachedAccessPoint.getPreferredPartId() == null) {
        	cachedAccessPoint.setPreferredPartId(findPreferredPartId(cachedAccessPoint));
        }
        // do not index APs without state or deleted APs
        ApState apState = cachedAccessPoint.getApState();
        if (apState == null || apState.getDeleteChangeId() != null) {
            return;
        }

        addStringField(FIELD_ACCESSPOINT_ID, apState.getAccessPointId().toString(), document);
        addStringField(STATE, apState.getStateApproval().name().toLowerCase(), document);
        addStringField(AP_TYPE_ID, apState.getApTypeId().toString(), document);
        addStringField(SCOPE_ID, apState.getScopeId().toString(), document);
        if (cachedAccessPoint.getRevState() != null) {
            addStringField(REV_STATE, cachedAccessPoint.getRevState().name().toLowerCase(), document);
        }
        if (cachedAccessPoint.getCreateUsername() != null) {
        	addStringField(USERNAME, cachedAccessPoint.getCreateUsername().toLowerCase(), document);
        }

        if (CollectionUtils.isNotEmpty(cachedAccessPoint.getParts())) {
            for (CachedPart part : cachedAccessPoint.getParts()) {
                addItemFields(DATA, part, cachedAccessPoint, document);
                addIndexFields(DATA, part, cachedAccessPoint, document);
            }
        }
    }

    private Integer findPreferredPartId(CachedAccessPoint cachedAccessPoint) {
    	if (cachedAccessPoint.getParts() != null) {
    		for (CachedPart part : cachedAccessPoint.getParts()) {
    			if (part.getKeyValue() != null && part.getKeyValue().getKeyType().equals(PT_PREFER_NAME)) {
    				return part.getPartId();
    			}
    		}
    	}
    	return null;
	}

	private void addItemFields(String name, CachedPart part, CachedAccessPoint cachedAccessPoint, DocumentElement document) {
        if (CollectionUtils.isNotEmpty(part.getItems())) {
            StaticDataProvider sdp = StaticDataProvider.getInstance();

            for (ApItem item : part.getItems()) {
                ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());
                RulItemSpec itemSpec = item.getItemSpecId() != null ? sdp.getItemSpecById(item.getItemSpecId()) : null;
                DataType dataType = DataType.fromCode(itemType.getEntity().getDataType().getCode());
                String itemTypeCode = itemType.getCode().toLowerCase();
                String itemSpecCode = itemSpec != null? itemSpec.getCode().toLowerCase() : null;

                // TODO refactor logic using switch

                if (dataType == DataType.COORDINATES) {
                    continue;
                }

                String value;

                if (dataType == DataType.RECORD_REF) {
                    ArrDataRecordRef dataRecordRef = HibernateUtils.unproxy(item.getData());
                    if (dataRecordRef == null || dataRecordRef.getRecordId() == null) {
                        continue;
                    }
                    document.addValue(REL_AP_ID, dataRecordRef.getRecordId());
                    value = dataRecordRef.getRecordId().toString();
                } else {
                    ArrData data = HibernateUtils.unproxy(item.getData());
                    value = data.getFulltextValue();
                }

                if (value == null) {
                    if (itemSpec == null) {
                        continue;
                    }
                    value = itemSpecCode;
                }

                if (part.getPartId().equals(cachedAccessPoint.getPreferredPartId())) {
                    addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + itemTypeCode, value.toLowerCase(), document, name);

                    if (itemSpec != null) {
                        addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + itemTypeCode + SEPARATOR + itemSpecCode, value.toLowerCase(), document, name);
                    }
                }

                // indexování polí unitdate
                if (dataType == DataType.UNITDATE) {
                	document.addValue(name + SEPARATOR + itemTypeCode + NORM_FROM, item.getData().getNormalizedFrom());
                	document.addValue(name + SEPARATOR + itemTypeCode + NORM_TO, item.getData().getNormalizedTo());
                }

                // indexování polí s více než 32766 znaky
                if (dataType == DataType.TEXT) {
                    document.addValue(name + SEPARATOR + itemTypeCode + ApCachedAccessPointBinder.ANALYZED, value);
                } else {
                    addField(name + SEPARATOR + itemTypeCode, value.toLowerCase(), document, name);
                }

                if (itemSpec != null) {
                    addField(name + SEPARATOR + itemTypeCode + SEPARATOR + itemSpecCode, value.toLowerCase(), document, name);
                }
            }
        }
    }

    private void addIndexFields(String name, CachedPart part, CachedAccessPoint cachedAccessPoint, DocumentElement document) {
        if (CollectionUtils.isNotEmpty(part.getIndices())) {
            for (ApIndex index : part.getIndices()) {
                if (index.getIndexType().equals(DISPLAY_NAME)) {

                    if (part.getPartId().equals(cachedAccessPoint.getPreferredPartId())) {
                        addField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX, index.getIndexValue().toLowerCase(), document, name);
                        addSortField(name + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX, index.getIndexValue().toLowerCase(), document);
                    }

                    addField(name + SEPARATOR + part.getPartTypeCode().toLowerCase() + SEPARATOR + INDEX, index.getIndexValue().toLowerCase(), document, name);
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
        document.addValue(name + ApCachedAccessPointBinder.SORTABLE, valueTrans);
    }

    private void addStringField(String name, String value, DocumentElement document) {
        document.addValue(name + ApCachedAccessPointBinder.NOT_ANALYZED, value); //TODO pasek
    }

    private void addField(String name, String value, DocumentElement document, String prefixName) {
        // Pridani raw hodnoty fieldu (bez tranliterace - NOT_ANALYZED) //TODO pasek
        document.addValue(name + ApCachedAccessPointBinder.NOT_ANALYZED, value);

        if (isFieldForTransliteration(name, prefixName)) {
            document.addValue(name + ApCachedAccessPointBinder.ANALYZED, value);
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

        if (settingIndexSearch != null) {
            SettingIndexSearch.Field fieldSearchConfig = getFieldSearchConfigByName(settingIndexSearch.getFields(), name);
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
    private static SettingIndexSearch getElzaSearchConfig() {
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
}
