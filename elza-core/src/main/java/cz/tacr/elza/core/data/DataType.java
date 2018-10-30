package cz.tacr.elza.core.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataApFragRef;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.DataTypeRepository;

public enum DataType {
    INT(ArrDataInteger.class, Integer.MAX_VALUE),
    STRING(ArrDataString.class, 1000),
    TEXT(ArrDataText.class, Integer.MAX_VALUE),
    UNITDATE(ArrDataUnitdate.class),
    UNITID(ArrDataUnitid.class, 250),
    FORMATTED_TEXT(ArrDataText.class, Integer.MAX_VALUE),
    COORDINATES(ArrDataCoordinates.class),
    PARTY_REF(ArrDataPartyRef.class),
    RECORD_REF(ArrDataRecordRef.class),
    DECIMAL(ArrDataDecimal.class, 38),
    STRUCTURED(ArrDataStructureRef.class),
    ENUM(ArrDataNull.class),
    FILE_REF(ArrDataFileRef.class),
    JSON_TABLE(ArrDataJsonTable.class, Integer.MAX_VALUE),
    DATE(ArrDataDate.class),
    APFRAG_REF(ArrDataApFragRef.class);

    private static Map<Integer, DataType> entityIdMap;

    private final Integer valueMaxSize;

    private RulDataType entity;

    private final Class<? extends ArrData> domainClass;

    private DataType(Class<? extends ArrData> domainClass, Integer valueMaxSize) {
        this.domainClass = domainClass;
        this.valueMaxSize = valueMaxSize;
    }

    private DataType(Class<? extends ArrData> domainClass) {
        this(domainClass, null);
    }

    /**
     * Check if data class is valid for this type
     * 
     * @param dataClass
     * @return
     */
    public boolean isValidClass(Class<? extends ArrData> dataClass) {
        return (dataClass == domainClass);
    }

    /**
     * @return Same value as result of <code>name()</code>.
     */
    public String getCode() {
        return name();
    }

    public int getId() {
        return getEntity().getDataTypeId();
    }

    public String getName() {
        return getEntity().getName();
    }

    public RulDataType getEntity() {
        return Validate.notNull(entity, "Cache not initialized");
    }

    /**
     * @return Max size of value or null when size is not relevant for data type.
     */
    public Integer getValueMaxSize() {
        return valueMaxSize;
    }

    /**
     * Case sensitive search by code (used by <code>valueOf</code>).
     *
     * @param code not-null
     * @return Null when not found.
     */
    public static DataType fromCode(String code) {
        Validate.notEmpty(code);
        try {
            return valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Search by entity id.
     *
     * @return Null when not found.
     */
    public static DataType fromId(int id) {
        return Validate.notNull(entityIdMap, "Cache not initialized").get(id);
    }

    static synchronized void init(DataTypeRepository dataTypeRepository) {
        List<RulDataType> entities = dataTypeRepository.findAll();
        DataType[] values = values();
        if (entities.size() != values.length) {
            throw new SystemException("Database count does not match with length of enum values");
        }
        // create id lookup
        Map<Integer, DataType> idMap = new HashMap<>(values.length);
        // init enum
        nextVal: for (DataType value : values) {
            Validate.isTrue(value.entity == null);
            for (RulDataType entity : entities) {
                if (value.name().equals(entity.getCode())) {
                    value.entity = entity;
                    // set lookups
                    idMap.put(entity.getDataTypeId(), value);
                    continue nextVal;
                }
            }
            throw new SystemException("Entity not found, code:" + value.name());
        }
        // set id lookup
        entityIdMap = idMap;
    }
}
