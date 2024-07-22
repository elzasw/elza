package cz.tacr.elza.core.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.domain.*;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.DataBitRepository;
import cz.tacr.elza.repository.DataCoordinatesRepository;
import cz.tacr.elza.repository.DataDateRepository;
import cz.tacr.elza.repository.DataDecimalRepository;
import cz.tacr.elza.repository.DataFileRefRepository;
import cz.tacr.elza.repository.DataIntegerRepository;
import cz.tacr.elza.repository.DataJsonTableRepository;
import cz.tacr.elza.repository.DataNullRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataStructureRefRepository;
import cz.tacr.elza.repository.DataTextRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DataUnitdateRepository;
import cz.tacr.elza.repository.DataUnitidRepository;
import cz.tacr.elza.repository.DataUriRefRepository;

public enum DataType {
    INT(ArrDataInteger.class, DataIntegerRepository.class, Integer.MAX_VALUE),
    STRING(ArrDataString.class, DataStringRepository.class, 1000),
    TEXT(ArrDataText.class, DataTextRepository.class, Integer.MAX_VALUE),
    UNITDATE(ArrDataUnitdate.class, DataUnitdateRepository.class),
    UNITID(ArrDataUnitid.class, DataUnitidRepository.class, 250),
    FORMATTED_TEXT(ArrDataText.class, DataTextRepository.class, Integer.MAX_VALUE),
    COORDINATES(ArrDataCoordinates.class, DataCoordinatesRepository.class),
    RECORD_REF(ArrDataRecordRef.class, DataRecordRefRepository.class),
    DECIMAL(ArrDataDecimal.class, DataDecimalRepository.class, 38),
    STRUCTURED(ArrDataStructureRef.class, DataStructureRefRepository.class),
    ENUM(ArrDataNull.class, DataNullRepository.class),
    FILE_REF(ArrDataFileRef.class, DataFileRefRepository.class),
    JSON_TABLE(ArrDataJsonTable.class, DataJsonTableRepository.class, Integer.MAX_VALUE),
    DATE(ArrDataDate.class, DataDateRepository.class),
    URI_REF(ArrDataUriRef.class, DataUriRefRepository.class),
    BIT(ArrDataBit.class, DataBitRepository.class);

    private static Map<Integer, DataType> entityIdMap;

    private final Integer valueMaxSize;

    private RulDataType entity;

    private JpaRepository<? extends ArrData, Integer> repository;

    private final Class<? extends ArrData> domainClass;

    private final Class<? extends JpaRepository<?, Integer>> repositoryClass;

    private DataType(Class<? extends ArrData> domainClass, Class<? extends JpaRepository<?, Integer>> repositoryClass, Integer valueMaxSize) {
        this.domainClass = domainClass;
        this.repositoryClass = repositoryClass;
        this.valueMaxSize = valueMaxSize;
    }

    private DataType(Class<? extends ArrData> domainClass, Class<? extends JpaRepository<?, Integer>> repositoryClass) {
        this(domainClass, repositoryClass, null);
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

    /**
     * Return entity DB id
     *
     * @return
     */
    public int getId() {
        return getEntity().getDataTypeId();
    }

    public String getName() {
        return getEntity().getName();
    }

    public RulDataType getEntity() {
        return Validate.notNull(entity, "Cache not initialized");
    }

	public JpaRepository<? extends ArrData, Integer> getRepository() {
		return repository;
	}

	public void setRepository(JpaRepository<? extends ArrData, Integer> repository) {
		this.repository = repository;
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

    static synchronized void init(DataTypeRepository dataTypeRepository, ApplicationContext ctx) {
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
                    value.repository = (JpaRepository<? extends ArrData, Integer>) ctx.getBean(value.repositoryClass);
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
