package cz.tacr.elza.core.data;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.RulPartType;

public class PartType {

    private final RulPartType type;

    public PartType(RulPartType type) {
        this.type = Validate.notNull(type);
    }

    public Integer getId() {
        return type.getPartTypeId();
    }

    public String getName() {
        return type.getName();
    }

    public String getCode() {
        return type.getCode();
    }

    public Boolean getRepeatable() {
        return type.getRepeatable();
    }

    public RulPartType getEntity() {
        return type;
    }

}
