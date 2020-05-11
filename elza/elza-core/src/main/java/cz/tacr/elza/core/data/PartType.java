package cz.tacr.elza.core.data;

import cz.tacr.elza.domain.RulPartType;
import org.apache.commons.lang3.Validate;

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

    public RulPartType getEntity() {
        return type;
    }

}
