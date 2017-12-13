package cz.tacr.elza.core.data;

import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.repository.PartyTypeRepository;

public enum PartyType {
    PERSON(ParPerson.class),
    DYNASTY(ParDynasty.class),
    GROUP_PARTY(ParPartyGroup.class),
    EVENT(ParEvent.class);

    private final Class<? extends ParParty> domainClass;

    private ParPartyType entity;

    private PartyType(Class<? extends ParParty> domainClass) {
        this.domainClass = domainClass;
    }

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

    public Class<? extends ParParty> getDomainClass() {
        return domainClass;
    }

    public ParPartyType getEntity() {
        return Validate.notNull(entity, "Cache not initialized");
    }

    /**
     * Case sensitive search by code (used by <code>valueOf</code>).
     *
     * @param code not-null
     * @return Null when not found.
     */
    public static PartyType fromCode(String code) {
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
            Validate.isTrue(value.entity == null);
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
