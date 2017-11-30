package cz.tacr.elza.core.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.DataTypeRepository;

public enum DataType {
    INT(Integer.MAX_VALUE),
    STRING(1000),
    TEXT(Integer.MAX_VALUE),
    UNITDATE,
    UNITID(250),
    FORMATTED_TEXT(Integer.MAX_VALUE),
    COORDINATES,
    PARTY_REF,
    RECORD_REF,
    DECIMAL(38),
    PACKET_REF,
    ENUM,
    FILE_REF,
    JSON_TABLE(Integer.MAX_VALUE);

    private static Map<Integer, DataType> entityIdMap;

    private final Integer valueMaxSize;

    private RulDataType entity;

    private DataType(Integer valueMaxSize) {
        this.valueMaxSize = valueMaxSize;
    }

    private DataType() {
        this(null);
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
