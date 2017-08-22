package cz.tacr.elza.core.data;

import java.util.List;

import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.repository.CalendarTypeRepository;

public enum CalendarType {
    JULIAN, GREGORIAN;

    private ArrCalendarType entity;

    /**
     * @return Same value as result of <code>name()</code>.
     */
    public String getCode() {
        return name();
    }

    public int getId() {
        return getEntity().getCalendarTypeId();
    }

    public String getName() {
        return getEntity().getName();
    }

    public ArrCalendarType getEntity() {
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
    public static CalendarType fromCode(String code) {
        Assert.hasLength(code);
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
    public static CalendarType fromId(int id) {
        for (CalendarType type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }

    static synchronized void init(CalendarTypeRepository calendarTypeRepository) {
        List<ArrCalendarType> entities = calendarTypeRepository.findAll();
        CalendarType[] values = values();
        if (entities.size() != values.length) {
            throw new IllegalStateException("Database count does not match with length of enum values");
        }
        // init enum
        nextVal: for (CalendarType value : values) {
            Assert.isNull(value.entity);
            for (ArrCalendarType entity : entities) {
                if (value.name().equals(entity.getCode())) {
                    value.entity = entity;
                    continue nextVal;
                }
            }
            throw new IllegalStateException("Entity not found, code:" + value.name());
        }
    }
}
