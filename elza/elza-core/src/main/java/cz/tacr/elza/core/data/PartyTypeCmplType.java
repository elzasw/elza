package cz.tacr.elza.core.data;

import cz.tacr.elza.domain.ParComplementType;

public class PartyTypeCmplType {

    private final ParComplementType entity;

    private final boolean repeatable;

    PartyTypeCmplType(ParComplementType entity, boolean repeatable) {
        this.entity = entity;
        this.repeatable = repeatable;
    }

    public String getCode() {
        return entity.getCode();
    }

    public ParComplementType getEntity() {
        return entity;
    }

    public boolean isRepeatable() {
        return repeatable;
    }
}