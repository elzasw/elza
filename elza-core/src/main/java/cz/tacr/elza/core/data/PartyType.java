package cz.tacr.elza.core.data;

import java.util.List;

import org.springframework.util.Assert;

import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.repository.PartyTypeRepository;

public enum PartyType {
    PERSON, DYNASTY, GROUP_PARTY, EVENT;

    private ParPartyType entity;

    /**
     * @return Equal to <code>name()</code>.
     */
    public String getCode() {
        return name();
    }

    public int getId() {
        return getEntity().getPartyTypeId();
    }

    public String getName() {
        return getEntity().getName();
    }

    public ParPartyType getEntity() {
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
    public static PartyType fromCode(String code) {
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
    public static PartyType fromId(int id) {
        for (PartyType type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }

    static synchronized void init(PartyTypeRepository partyTypeRepository) {
        List<ParPartyType> entities = partyTypeRepository.findAll();
        PartyType[] values = values();
        if (entities.size() != values.length) {
            throw new IllegalStateException("Database count does not match with length of enum values");
        }
        // init enum
        nextVal: for (PartyType value : values) {
            Assert.isNull(value.entity);
            for (ParPartyType entity : entities) {
                if (value.name().equals(entity.getCode())) {
                    value.entity = entity;
                    continue nextVal;
                }
            }
            throw new IllegalStateException("Entity not found, code:" + value.name());
        }
    }
}
