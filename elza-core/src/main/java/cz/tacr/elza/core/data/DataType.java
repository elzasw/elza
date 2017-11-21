package cz.tacr.elza.core.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.DataTypeRepository;

public enum DataType {
    INT, STRING, TEXT, UNITDATE, UNITID, FORMATTED_TEXT, COORDINATES, PARTY_REF, RECORD_REF, DECIMAL, ENUM, FILE_REF, JSON_TABLE, STRUCTURED;

    private static Map<Integer, DataType> ENTITY_ID_LOOKUP;

    private RulDataType entity;

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
        if (entity == null) {
            throw new IllegalStateException("Cache not initialized");
        }
        return entity;
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
        if (ENTITY_ID_LOOKUP == null) {
            throw new IllegalStateException("Cache not initialized");
        }
        return ENTITY_ID_LOOKUP.get(id);
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
        ENTITY_ID_LOOKUP = idMap;
    }
}
