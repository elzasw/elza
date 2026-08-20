package cz.tacr.elza.repository.filter;

import static cz.tacr.elza.repository.filter.AipFilterValueType.BOOLEAN;
import static cz.tacr.elza.repository.filter.AipFilterValueType.DATE;
import static cz.tacr.elza.repository.filter.AipFilterValueType.ENUM;
import static cz.tacr.elza.repository.filter.AipFilterValueType.NUMBER;
import static cz.tacr.elza.repository.filter.AipFilterValueType.REF;
import static cz.tacr.elza.repository.filter.AipFilterValueType.TEXT;

import java.util.EnumMap;
import java.util.Map;

import cz.tacr.elza.controller.vo.AipFieldName;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Maps a field of the AIP filter contract onto the entity model.
 *
 * This is the single place that knows where a filterable field lives. The predicate builder
 * resolves a field through it and never matches on names of its own.
 */
public enum AipFieldMapping {

    AIP_ID           (AipJoin.AIP,             "aipId",           NUMBER),
    CODE             (AipJoin.AIP,             "code",            TEXT),
    AIP_VERSION      (AipJoin.STATE,           "aipVersion",      TEXT),
    FUND             (AipJoin.FUND,            ArrFund.FIELD_FUND_ID, REF),
    FUND_CODE        (AipJoin.STATE,           "fundCode",        TEXT),
    INSTITUTION      (AipJoin.INSTITUTION_AP,  ApAccessPoint.FIELD_ACCESS_POINT_ID, REF),
    INSTITUTION_CODE (AipJoin.STATE,           "institutionCode", TEXT),
    UNITDATE         (AipJoin.STATE,           "unitdateFrom",    "unitdateTo", DATE),
    ORIGINATOR       (AipJoin.ORIGINATOR_AP,   ApAccessPoint.FIELD_ACCESS_POINT_ID, REF),
    INGESTION_CODE   (AipJoin.STATE,           "ingestionCode",   TEXT),
    REFERENCE_NUMBER (AipJoin.STATE,           "referenceNumber", TEXT),
    NAD_CHANGE_CODE  (AipJoin.STATE,           "nadChangeCode",   TEXT),
    AIP_SIZE         (AipJoin.STATE,           "aipSize",         NUMBER),
    METADATA_LOAD    (AipJoin.STATE,           "metadataLoad",    BOOLEAN),
    COMPLETE_AIP_LOAD(AipJoin.STATE,           "completeAipLoad", BOOLEAN),
    METADATA_ERROR   (AipJoin.STATE,           "metadataError",   BOOLEAN),
    IMPORT_STATE     (AipJoin.IMPORT_SYNC,     "state",           ENUM),
    EXPORT_STATE     (AipJoin.EXPORT_SYNC,     "state",           ENUM);

    /**
     * Join of the AIP query a field is reached through, with the entity it leads to.
     */
    public enum AipJoin {
        AIP(DaAip.class),
        STATE(DaAipState.class),
        IMPORT_SYNC(DaSyncQueueItem.class),
        EXPORT_SYNC(DaSyncQueueItem.class),
        ORIGINATOR_AP(ApAccessPoint.class),
        INSTITUTION_AP(ApAccessPoint.class),
        FUND(ArrFund.class);

        private final Class<?> entityClass;

        AipJoin(final Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        public Class<?> getEntityClass() {
            return entityClass;
        }
    }

    private static final Map<AipFieldName, AipFieldMapping> BY_FIELD_NAME = new EnumMap<>(AipFieldName.class);

    static {
        for (AipFieldMapping mapping : values()) {
            BY_FIELD_NAME.put(AipFieldName.valueOf(mapping.name()), mapping);
        }
    }

    private final AipJoin join;

    private final String attribute;

    /**
     * Second attribute of a field that spans a pair of columns, otherwise null.
     */
    private final String secondAttribute;

    private final AipFilterValueType valueType;

    AipFieldMapping(final AipJoin join, final String attribute, final AipFilterValueType valueType) {
        this(join, attribute, null, valueType);
    }

    AipFieldMapping(final AipJoin join, final String attribute, final String secondAttribute,
                    final AipFilterValueType valueType) {
        this.join = join;
        this.attribute = attribute;
        this.secondAttribute = secondAttribute;
        this.valueType = valueType;
    }

    public AipJoin getJoin() {
        return join;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getSecondAttribute() {
        return secondAttribute;
    }

    public boolean isPair() {
        return secondAttribute != null;
    }

    public AipFilterValueType getValueType() {
        return valueType;
    }

    /**
     * Resolves a field of the contract onto its mapping.
     *
     * Every {@link AipFieldName} has a mapping - the static initializer fails fast if one is
     * missing, and AipFieldMappingTest asserts it for the whole enum.
     */
    public static AipFieldMapping of(final AipFieldName fieldName) {
        AipFieldMapping mapping = BY_FIELD_NAME.get(fieldName);
        if (mapping == null) {
            throw new BusinessException("Pole '" + fieldName.getValue() + "' nelze použít pro filtrování AIP",
                    BaseCode.PROPERTY_IS_INVALID).set(BaseCode.PARAM_PROPERTY, fieldName.getValue());
        }
        return mapping;
    }
}
